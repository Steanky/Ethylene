package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This utility class can be used to aid the process of designing generalized topology-preserving object graph
 * transformations.
 */
public final class GraphTransformer {
    public static final class Options {
        /**
         * Default options: circular references supported, scalar references are not tracked, and depth-first traversal.
         */
        public static final int DEFAULT = 0;

        public static final int DISABLE_CIRCULAR_REF_SUPPORT = 1;
        public static final int CIRCULAR_REF_FOR_SCALAR = 2;
        public static final int BREADTH_FIRST = 4;

        static boolean hasOption(int options, int option) {
            return (options & option) != 0;
        }
    }

    private static final Accumulator<?, ?> EMPTY_ACCUMULATOR = (Accumulator<Object, Object>) (o, o2, circular) -> {};
    private static final Output<?, ?> EMPTY_OUTPUT = new Output<>(null, EMPTY_ACCUMULATOR);
    private static final Node<Object, Object, Object> EMPTY_NODE = new Node<>(new Iterator<>() {
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

        boolean circularRefSupport = !Options.hasOption(flags, Options.DISABLE_CIRCULAR_REF_SUPPORT);
        boolean circularRefForScalar = Options.hasOption(flags, Options.CIRCULAR_REF_FOR_SCALAR);
        boolean breadthFirst = Options.hasOption(flags, Options.BREADTH_FIRST);

        //don't initialize the visitation map if there is no support for circular references
        Map<TVisit, TOut> visited = null;
        if (circularRefSupport) {
            visited = visitedSupplier.get();
            visited.put(visitKeyMapper.apply(rootInput), rootNode.output.data);
        }

        stack.push(rootNode);
        while (!stack.isEmpty()) {
            //guaranteed to be non-empty (empty nodes are never added to the stack)
            Node<TIn, TOut, TKey> node = breadthFirst ? stack.pop() : stack.peek();

            boolean hasOutput = hasOutput(node);
            boolean unfinished = false;
            while (node.inputIterator.hasNext()) {
                Entry<TKey, TIn> entry = node.inputIterator.next();
                if (!containerPredicate.test(entry.getSecond())) {
                    //nodes that aren't containers have no children, so we can immediately add them to the accumulator
                    if (hasOutput) {
                        TIn second = entry.getSecond();
                        TOut out;
                        boolean circular;
                        if (circularRefSupport && circularRefForScalar) {
                            TVisit visit = visitKeyMapper.apply(second);
                            if (visited.containsKey(visit)) {
                                out = visited.get(visit);
                                circular = true;
                            }
                            else {
                                out = scalarMapper.apply(second);
                                circular = false;
                            }
                        }
                        else {
                            out = scalarMapper.apply(second);
                            circular = false;
                        }

                        node.output.accumulator.accept(entry.getFirst(), out, circular);
                    }

                    continue;
                }

                if (circularRefSupport) {
                    //handle already-visited non-scalar nodes, to allow proper handling of circular references
                    TVisit visit = visitKeyMapper.apply(entry.getSecond());

                    //check containsKey, null values are allowed in the map
                    if (visited.containsKey(visit)) {
                        //already-visited references are immediately added to the accumulator
                        //if these references are nodes, their output might not have been fully constructed yet
                        //it might not even be possible to ensure that it is constructed, in the case of circular references
                        //therefore, immediately add them to the accumulator, and let it know the reference is circular
                        if (hasOutput) {
                            node.output.accumulator.accept(entry.getFirst(), visited.get(visit), true);
                        }
                        continue;
                    }
                }

                Node<TIn, TOut, TKey> newNode = nodeFunction.apply(entry.getSecond());
                if (circularRefSupport) {
                    visited.put(visitKeyMapper.apply(entry.getSecond()), newNode.output.data);
                }

                if (isEmpty(newNode)) {
                    if (hasOutput) {
                        //call the accumulator right away, empty nodes are not unfinished
                        node.output.accumulator.accept(entry.getFirst(), newNode.output.data, false);
                    }

                    //don't bother pushing empty nodes to the stack, they cannot be explored
                    continue;
                }

                //newNode will be processed next
                stack.push(newNode);

                if (!breadthFirst) {
                    if (hasOutput) {
                        //set the current node's result key and out fields
                        node.result.key = entry.getFirst();
                        node.result.out = newNode.output.data;
                        unfinished = true;
                    }

                    break;
                }
            }

            if (!unfinished && !breadthFirst) {
                //remove the finished node
                stack.pop();

                Node<TIn, TOut, TKey> old = stack.peek();
                if (old != null && hasOutput(old)) {
                    old.output.accumulator.accept(old.result.key, old.result.out, false);
                }
            }
        }

        return rootNode.output.data;
    }

    private static boolean isEmpty(Node<?, ?, ?> node) {
        return node == EMPTY_NODE || !node.inputIterator.hasNext();
    }

    private static boolean hasOutput(Node<?, ?, ?> node) {
        return node.output != EMPTY_OUTPUT;
    }

    public static <TIn, TOut, TKey> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, Function.identity(), IdentityHashMap::new,
                new ArrayDeque<>(), Options.DEFAULT);
    }

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
            @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, visitKeyMapper, IdentityHashMap::new,
                new ArrayDeque<>(), Options.DEFAULT);
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