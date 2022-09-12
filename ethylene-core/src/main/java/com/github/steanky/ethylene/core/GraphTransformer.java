package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This utility class can be used to aid the process of designing generalized topology-preserving object graph
 * transformations.
 */
public final class GraphTransformer {
    private static final Accumulator<?, ?> EMPTY_ACCUMULATOR = (Accumulator<Object, Object>) (o, o2, circular) -> {};
    private static final Output<?, ?> EMPTY_OUTPUT = new Output<>(null, EMPTY_ACCUMULATOR);

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn rootInput,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
            @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper, @NotNull Map<TVisit, TOut> visited,
            @NotNull Deque<Node<TIn, TOut, TKey>> stack) {
        if (!containerPredicate.test(rootInput)) {
            return scalarMapper.apply(rootInput);
        }

        Node<TIn, TOut, TKey> rootNode = nodeFunction.apply(rootInput);
        visited.put(visitKeyMapper.apply(rootInput), rootNode.output.data);
        stack.push(rootNode);

        while (!stack.isEmpty()) {
            Node<TIn, TOut, TKey> node = stack.peek();

            boolean hasOutput = node.output != EMPTY_OUTPUT;
            boolean unfinished = false;
            while (node.inputIterator.hasNext()) {
                Entry<TKey, TIn> entry = node.inputIterator.next();
                if (!containerPredicate.test(entry.getSecond())) {
                    //nodes that aren't containers have no children, so we can immediately add them to the accumulator
                    if (hasOutput) {
                        node.output.accumulator.accept(entry.getFirst(), scalarMapper.apply(entry.getSecond()), false);
                    }
                    continue;
                }

                //handle already-visited non-scalar nodes, to allow proper handling of circular references
                TVisit visit = visitKeyMapper.apply(entry.getSecond());
                if (visited.containsKey(visit)) {
                    //circular references are immediately added to the accumulator
                    if (hasOutput) {
                        node.output.accumulator.accept(entry.getFirst(), visited.get(visit), true);
                    }
                    continue;
                }

                Node<TIn, TOut, TKey> newNode = nodeFunction.apply(entry.getSecond());
                visited.put(visitKeyMapper.apply(entry.getSecond()), newNode.output.data);
                stack.push(newNode);

                //this node is unfinished, wait to call the accumulator
                if (hasOutput) {
                    node.result.key = entry.getFirst();
                    node.result.out = newNode.output.data;
                    unfinished = true;
                }
                break;
            }

            if (!unfinished) {
                stack.pop();

                if (hasOutput) {
                    Node<TIn, TOut, TKey> old = stack.peek();
                    if (old != null) {
                        old.output.accumulator.accept(old.result.key, old.result.out, false);
                    }
                }
            }
        }

        return rootNode.output.data;
    }

    public static <TIn, TOut, TKey> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, Function.identity(),
                new IdentityHashMap<>(), new ArrayDeque<>());
    }

    public static <TIn, TOut, TKey, TVisit> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
            @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper) {
        return process(input, nodeFunction, containerPredicate, scalarMapper, visitKeyMapper, new IdentityHashMap<>(),
                new ArrayDeque<>());
    }

    @SuppressWarnings("unchecked")
    public static <TKey, TOut> @NotNull Output<TKey, TOut> emptyOutput() {
        return (Output<TKey, TOut>) EMPTY_OUTPUT;
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