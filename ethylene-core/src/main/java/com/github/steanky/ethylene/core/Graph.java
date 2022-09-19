package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This utility class can be used to aid the process of designing generalized topology-preserving object graph
 * transformations.
 */
public final class Graph {
    //shared empty accumulator which is just a no-op
    private static final Accumulator<?, ?> EMPTY_ACCUMULATOR = (Accumulator<Object, Object>) (o, o2, circular) -> {
    };
    //shared empty Output with null data and the empty accumulator
    private static final Output<?, ?> EMPTY_OUTPUT = new Output<>(null, EMPTY_ACCUMULATOR);
    //shared empty Node with an empty iterator and the empty output
    private static final Node<?, ?, ?> EMPTY_NODE = new Node<>(new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Entry<Object, Object> next() {
            throw new NoSuchElementException();
        }
    }, emptyOutput());

    private Graph() {
        throw new UnsupportedOperationException();
    }

    public static <TIn, TOut, TKey> TOut process(TIn input,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        int flags) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, Function.identity(), IdentityHashMap::new,
            ArrayDeque::new, flags);
    }

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn rootInput,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper,
        @NotNull Supplier<? extends Map<? super TVisit, TOut>> visitedSupplier,
        @NotNull Supplier<? extends Deque<Node<TIn, TOut, TKey>>> stackSupplier, int flags) {
        if (!containerPredicate.test(rootInput)) {
            //if rootInput is a scalar, just return whatever the scalar mapper produces
            //there's nothing more to do
            return scalarMapper.apply(rootInput);
        }

        Node<TIn, TOut, TKey> rootNode = nodeFunction.apply(rootInput);

        //can't iterate an empty node, so just return its data immediately
        if (isEmpty(rootNode)) {
            return rootNode.output.data;
        }

        boolean circularRefSupport = Options.hasOption(flags, Options.TRACK_REFERENCES);
        boolean trackScalarReference = Options.hasOption(flags, Options.TRACK_SCALAR_REFERENCES);
        boolean depthFirst = Options.hasOption(flags, Options.DEPTH_FIRST);
        boolean lazyAccumulation = Options.hasOption(flags, Options.LAZY_ACCUMULATION) && depthFirst;

        //don't initialize the visitation map if there is no support for circular references
        //make sure usages of this map check circularRefSupport to avoid NPE
        Map<? super TVisit, TOut> visited = null;
        if (circularRefSupport) {
            visited = visitedSupplier.get();
            visited.put(visitKeyMapper.apply(rootInput), rootNode.output.data);
        }

        Deque<Node<TIn, TOut, TKey>> stack = stackSupplier.get();
        stack.push(rootNode);
        while (!stack.isEmpty()) {
            //guaranteed to be non-empty (empty nodes are never added to the stack)
            Node<TIn, TOut, TKey> node = depthFirst ? stack.peek() : stack.pop();

            boolean hasOutput = hasOutput(node);
            boolean finished = true;
            while (node.inputIterator.hasNext()) {
                Entry<? extends TKey, ? extends TIn> entry = node.inputIterator.next();
                TKey entryKey = entry.getFirst();
                TIn entryInput = entry.getSecond();

                //if not a container, then we have a scalar
                if (!containerPredicate.test(entryInput)) {
                    //nodes that aren't containers have no children, so we can immediately add them to the accumulator
                    if (hasOutput) {
                        TOut out;
                        boolean circular;

                        //keep track of scalar references in the same way as nodes, if enabled
                        if (circularRefSupport && trackScalarReference) {
                            TVisit visit = visitKeyMapper.apply(entryInput);
                            if (visited.containsKey(visit)) {
                                out = visited.get(visit);
                                circular = true;
                            } else {
                                out = scalarMapper.apply(entryInput);
                                circular = false;
                            }
                        } else {
                            out = scalarMapper.apply(entryInput);
                            circular = false;
                        }

                        node.output.accumulator.accept(entryKey, out, circular);
                    }

                    continue;
                }

                TVisit visit = null;
                if (circularRefSupport) {
                    //handle already-visited non-scalar nodes, to allow proper handling of circular references
                    visit = visitKeyMapper.apply(entryInput);

                    //check containsKey, null values are allowed in the map
                    if (visited.containsKey(visit)) {
                        /*
                        already-visited references are immediately added to the accumulator. if these references are
                        nodes, their output might not have been fully constructed yet. it might not even be possible to
                        ensure that it is constructed, in the case of circular references. therefore, immediately add
                        them to the accumulator, and let it know the reference is circular
                         */
                        if (hasOutput) {
                            node.output.accumulator.accept(entryKey, visited.get(visit), true);
                        }

                        continue;
                    }
                }

                Node<TIn, TOut, TKey> newNode = nodeFunction.apply(entryInput);
                if (circularRefSupport) {
                    visited.put(visit, newNode.output.data);
                }

                if (isEmpty(newNode)) {
                    if (hasOutput) {
                        //call the accumulator right away, empty nodes cannot have children
                        node.output.accumulator.accept(entryKey, newNode.output.data, false);
                    }

                    //don't bother pushing empty nodes to the stack, they cannot be explored
                    continue;
                }

                //if depth-first, newNode is the next node to be processed
                //if breadth-first, nodes further to the end of this node's iterator will be processed first
                stack.push(newNode);

                if (hasOutput) {
                    if (lazyAccumulation) {
                        //set the current node's result key and out fields
                        node.setResult(entryKey, newNode.output.data);
                    } else {
                        //no lazy accumulation, immediately add this node
                        node.output.accumulator.accept(entryKey, newNode.output.data, false);
                    }
                }

                //depthFirst also means we've only peeked the top node!
                if (depthFirst) {
                    //if depth-first, try to process the next node
                    //we are "unfinished" at this point because we have a child node (newNode)
                    finished = false;
                    break;
                }
            }

            if (finished && depthFirst) {
                //if depth-first, we only peeked the stack node
                //if we're finished, we can pop this node
                stack.pop();

                if (lazyAccumulation) {
                    //when lazily accumulating depth-first, the parent node is at the top of the stack
                    //we will never lazily accumulate when breadth-first
                    Node<TIn, TOut, TKey> old = stack.peek();
                    if (old != null && hasOutput(old) && old.hasResult()) {
                        old.output.accumulator.accept(old.result.key, old.result.out, false);
                    }
                }
            }
        }

        return rootNode.output.data;
    }

    private static boolean isEmpty(Node<?, ?, ?> node) {
        return node == EMPTY_NODE || !node.inputIterator.hasNext();
    }

    private static boolean hasOutput(Node<?, ?, ?> node) {
        return node.output != EMPTY_OUTPUT && node.output.accumulator != EMPTY_ACCUMULATOR;
    }

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn input,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper, int flags) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, visitKeyMapper, IdentityHashMap::new,
            ArrayDeque::new, flags);
    }

    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> node(
        @NotNull Iterator<? extends Entry<? extends TKey, ? extends TIn>> inputIterator,
        @NotNull Output<TOut, TKey> output) {
        return new Node<>(inputIterator, output);
    }

    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> node(
        @NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator) {
        return new Node<>(inputIterator, emptyOutput());
    }

    @SuppressWarnings("unchecked")
    public static <TKey, TOut> @NotNull Output<TKey, TOut> emptyOutput() {
        return (Output<TKey, TOut>) EMPTY_OUTPUT;
    }

    public static <TOut, TKey> @NotNull Output<TOut, TKey> output(TOut data,
        @NotNull Accumulator<? super TKey, ? super TOut> accumulator) {
        return new Output<>(data, accumulator);
    }

    @SuppressWarnings("unchecked")
    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> emptyNode() {
        return (Node<TIn, TOut, TKey>) EMPTY_NODE;
    }

    @FunctionalInterface
    public interface Accumulator<TKey, TOut> {
        void accept(TKey key, TOut out, boolean circular);
    }

    /**
     * Bit flags for setting various options.
     */
    public static final class Options {
        /**
         * All options are disabled. This corresponds to a breadth-first graph transform with no reference tracking of
         * any sort, and no lazy accumulation.
         */
        public static final int NONE = 0;

        /**
         * Enables support for reference tracking. If this is enabled, all node references will be tracked, whereas
         * scalar references <i>can</i> be tracked only if their corresponding option flag is set. Warning: when this
         * option is not present, any circular references in the input data structure will cause an infinite loop and
         * eventually an OOM.
         */
        public static final int TRACK_REFERENCES = 1;

        /**
         * Enables support for tracking scalar references. This option will do nothing if the REFERENCE_TRACKING flag is
         * not also set. Since scalars cannot have children, not having this option will not cause a possibility of
         * infinite loops. However, enabling it may be desirable in order to "de-duplicate" equivalent instances of
         * scalars in the output data structure, at the cost of additional memory being used to store the scalar objects
         * during graph transformation.
         */
        public static final int TRACK_SCALAR_REFERENCES = 2;

        /**
         * Equivalent to combining REFERENCE_TRACKING and TRACK_SCALAR_REFERENCE.
         */
        public static final int TRACK_ALL_REFERENCES = 3;

        /**
         * Causes graphs to be processed in a depth-first manner.
         */
        public static final int DEPTH_FIRST = 4;

        /**
         * Enables lazy accumulation of nodes. Child nodes will only be added to their parent accumulator when all the
         * child's child nodes have been added. This option has no effect unless depth-first processing is enabled.
         */
        public static final int LAZY_ACCUMULATION = 8;

        private static boolean hasOption(int options, int option) {
            return (options & option) != 0;
        }
    }

    //used internally for lazy accumulation of results
    //only relevant when doing depth-first transforms
    private static final class NodeResult<TKey, TOut> {
        private TKey key;
        private TOut out;
    }

    public static final class Node<TIn, TOut, TKey> {
        private final Iterator<? extends Entry<? extends TKey, ? extends TIn>> inputIterator;
        private final Output<TOut, ? super TKey> output;
        private NodeResult<TKey, TOut> result;

        private Node(@NotNull Iterator<? extends Entry<? extends TKey, ? extends TIn>> inputIterator,
            @NotNull Output<TOut, ? super TKey> output) {
            this.inputIterator = inputIterator;
            this.output = output;
        }

        private boolean hasResult() {
            return result != null;
        }

        private void setResult(TKey key, TOut out) {
            NodeResult<TKey, TOut> result =
                Objects.requireNonNullElseGet(this.result, () -> this.result = new NodeResult<>());
            result.key = key;
            result.out = out;
        }

        public @NotNull Iterator<? extends Entry<? extends TKey, ? extends TIn>> inputIterator() {
            return inputIterator;
        }

        public @NotNull Output<TOut, ? super TKey> output() {
            return output;
        }
    }

    public static final class Output<TOut, TKey> {
        private final TOut data;
        private final Accumulator<? super TKey, ? super TOut> accumulator;

        private Output(TOut data, @NotNull Accumulator<? super TKey, ? super TOut> accumulator) {
            this.data = data;
            this.accumulator = accumulator;
        }
    }
}