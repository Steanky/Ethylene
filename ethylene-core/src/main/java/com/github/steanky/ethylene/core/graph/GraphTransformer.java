package com.github.steanky.ethylene.core.graph;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This utility class can be used to aid the process of designing generalized topology-preserving object graph
 * transformations.
 */
public final class GraphTransformer {
    public static <TIn, TOut, TKey> TOut process(TIn input, @NotNull Deque<Node<TIn, TOut, TKey>> stack,
            @NotNull Map<TIn, TOut> visited,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper) {
        if (!containerPredicate.test(input)) {
            return scalarMapper.apply(input);
        }

        Node<TIn, TOut, TKey> root = nodeFunction.apply(input);
        visited.put(input, root.output.data);
        stack.push(root);

        while (!stack.isEmpty()) {
            Node<TIn, TOut, TKey> node = stack.peek();

            outer:
            {
                while (node.inputIterator.hasNext()) {
                    Entry<TKey, TIn> entry = node.inputIterator.next();
                    if (!containerPredicate.test(entry.getSecond())) {
                        node.output.accumulator.accept(entry.getFirst(), scalarMapper.apply(entry.getSecond()));
                        continue;
                    }

                    //handle already-visited non-scalar nodes, to allow proper handling of circular references
                    TIn in = entry.getSecond();
                    if (visited.containsKey(in)) {
                        node.output.accumulator.accept(entry.getFirst(), visited.get(in));
                        continue;
                    }

                    Node<TIn, TOut, TKey> newNode = nodeFunction.apply(entry.getSecond());
                    visited.put(entry.getSecond(), newNode.output.data);
                    stack.push(newNode);

                    //this node is unfinished, wait to call the accumulator
                    node.result.key = entry.getFirst();
                    node.result.out = newNode.output.data;
                    break outer;
                }

                stack.pop();
                Node<TIn, TOut, TKey> old = stack.peek();
                if (old != null) {
                    old.output.accumulator.accept(old.result.key, old.result.out);
                }
            }
        }

        return root.output.data;
    }

    public static <TIn, TOut, TKey> TOut process(TIn input,
            @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
            @NotNull Predicate<? super TIn> containerPredicate,
            @NotNull Function<? super TIn, ? extends TOut> scalarMapper) {
        return process(input, new ArrayDeque<>(), new IdentityHashMap<>(), nodeFunction, containerPredicate,
                scalarMapper);
    }

    public static class NodeResult<TKey, TOut> {
        private TKey key;
        private TOut out;

        private NodeResult(TKey key, TOut out) {
            this.key = key;
            this.out = out;
        }
    }

    public record Node<TIn, TOut, TKey>(TIn in, @NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator,
            @NotNull Output<TOut, TKey> output, @NotNull NodeResult<TKey, TOut> result) {
        public Node(TIn in, @NotNull Iterator<? extends Entry<TKey, TIn>> inputIterator,
                @NotNull Output<TOut, TKey> output) {
            this(in, inputIterator, output, new NodeResult<>(null, null));
        }
    }

    public record Output<TOut, TKey>(@NotNull TOut data, @NotNull BiConsumer<? super TKey, ? super TOut> accumulator) {}
}