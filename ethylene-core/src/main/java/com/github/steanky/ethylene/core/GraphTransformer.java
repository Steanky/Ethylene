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
public final class GraphTransformer {
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
         * option is disabled, any circular references in the input data structure will cause an infinite loop and
         * eventually an OOM.
         */
        public static final int REFERENCE_TRACKING = 1;

        /**
         * Enables support for tracking scalar references. This option will do nothing if the REFERENCE_TRACKING flag
         * is not also set.
         */
        public static final int TRACK_SCALAR_REFERENCE = 2;

        /**
         * Equivalent to using both REFERENCE_TRACKING and TRACK_SCALAR_REFERENCE.
         */
        public static final int TRACK_ALL_REFERENCES = 3;

        /**
         * Causes graphs to be processed in a depth-first manner.
         */
        public static final int DEPTH_FIRST = 4;

        /**
         * Enables lazy accumulation of nodes. Child nodes will only be added to their parent accumulator when all the
         * child's child nodes have been added.
         */
        public static final int LAZY_ACCUMULATION = 8;

        private static boolean hasOption(int options, int option) {
            return (options & option) != 0;
        }
    }

    private static final Accumulator<?, ?> EMPTY_ACCUMULATOR = (Accumulator<Object, Object>) (o, o2, circular) -> {};
    private static final Output<?, ?> EMPTY_OUTPUT = new Output<>(null, EMPTY_ACCUMULATOR);
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

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn rootInput,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
            @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper,
            @NotNull Supplier<? extends Map<TVisit, TOut>> visitedSupplier,
            @NotNull Deque<Node<TIn, TOut, TKey>> stack, int flags) {
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

        boolean circularRefSupport = Options.hasOption(flags, Options.REFERENCE_TRACKING);
        boolean trackScalarReference = Options.hasOption(flags, Options.TRACK_SCALAR_REFERENCE);
        boolean depthFirst = Options.hasOption(flags, Options.DEPTH_FIRST);
        boolean lazyAccumulation = Options.hasOption(flags, Options.LAZY_ACCUMULATION);

        //don't initialize the visitation map if there is no support for circular references
        //make sure usages of this map check circularRefSupport to avoid NPE
        Map<TVisit, TOut> visited = null;
        if (circularRefSupport) {
            visited = visitedSupplier.get();
            visited.put(visitKeyMapper.apply(rootInput), rootNode.output.data);
        }

        stack.push(rootNode);
        while (!stack.isEmpty()) {
            boolean peek = lazyAccumulation || depthFirst;

            //guaranteed to be non-empty (empty nodes are never added to the stack)
            Node<TIn, TOut, TKey> node = peek ? stack.peek() : stack.pop();

            boolean hasOutput = hasOutput(node);
            boolean finished = true;
            while (node.inputIterator.hasNext()) {
                Entry<TKey, TIn> entry = node.inputIterator.next();
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
                            }
                            else {
                                out = scalarMapper.apply(entryInput);
                                circular = false;
                            }
                        }
                        else {
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
                        //already-visited references are immediately added to the accumulator
                        //if these references are nodes, their output might not have been fully constructed yet
                        //it might not even be possible to ensure that it is constructed, in the case of circular references
                        //therefore, immediately add them to the accumulator, and let it know the reference is circular
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
                        node.result.key = entryKey;
                        node.result.out = newNode.output.data;
                    }
                    else {
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

            if (finished && peek) {
                //remove the finished node
                stack.pop();

                if (lazyAccumulation) {
                    //if lazy, it means we have to add a child node to "old", if it exists
                    Node<TIn, TOut, TKey> old = stack.peek();
                    if (old != null && hasOutput(old)) {
                        old.output.accumulator.accept(old.result.key, old.result.out, false);
                    }
                }
            }
        }

        return rootNode.output.data;
    }

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
            @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper, int flags) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, visitKeyMapper, IdentityHashMap::new,
                new ArrayDeque<>(), flags);
    }

    private static boolean isEmpty(Node<?, ?, ?> node) {
        return node == EMPTY_NODE || !node.inputIterator.hasNext();
    }

    private static boolean hasOutput(Node<?, ?, ?> node) {
        return node.output != EMPTY_OUTPUT;
    }

    @SuppressWarnings("unchecked")
    public static <TKey, TOut> @NotNull Output<TKey, TOut> emptyOutput() {
        return (Output<TKey, TOut>) EMPTY_OUTPUT;
    }

    @SuppressWarnings("unchecked")
    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> emptyNode() {
        return (Node<TIn, TOut, TKey>) EMPTY_NODE;
    }

    @FunctionalInterface
    public interface Accumulator<TKey, TOut> {
        void accept(TKey key, TOut out, boolean circular);
    }

    public static class NodeResult<TKey, TOut> {
        private TKey key;
        private TOut out;
    }

    public record Node<TIn, TOut, TKey>(@NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator,
            @NotNull Output<TOut, TKey> output, @NotNull NodeResult<TKey, TOut> result) {
        public Node(@NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator, @NotNull Output<TOut, TKey> output) {
            this(inputIterator, output, new NodeResult<>());
        }

        public Node(@NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator) {
            this(inputIterator, emptyOutput(), new NodeResult<>());
        }
    }

    public record Output<TOut, TKey>(TOut data, @NotNull Accumulator<? super TKey, ? super TOut> accumulator) {}
}