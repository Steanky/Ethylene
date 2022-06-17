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
    public record Node<TIn, TOut, TKey>(TIn in,
                                        @NotNull Iterable<? extends Entry<TKey, TIn>> inputIterable,
                                        @NotNull Output<TOut, TKey> output) {}

    public record Output<TOut, TKey>(@NotNull TOut data,
                                     @NotNull BiConsumer<? super TKey, ? super TOut> accumulator) {}

    public static <TIn, TOut, TKey> TOut process(TIn input,
                                                 @NotNull Deque<Node<TIn, TOut, TKey>> stack,
                                                 @NotNull Map<TIn, TOut> visited,
                                                 @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
                                                 @NotNull Predicate<? super TIn> containerPredicate,
                                                 @NotNull Function<? super TIn, ? extends Entry<TKey, TOut>> scalarMapper) {
        if(!containerPredicate.test(input)) {
            return scalarMapper.apply(input).getSecond();
        }

        Node<TIn, TOut, TKey> root = nodeFunction.apply(input);
        visited.put(input, root.output.data);
        stack.push(root);

        while(!stack.isEmpty()) {
            Node<TIn, TOut, TKey> node = stack.pop();

            for(Entry<TKey, TIn> in : node.inputIterable) {
                if(!containerPredicate.test(in.getSecond())) {
                    Entry<TKey, TOut> entry = scalarMapper.apply(in.getSecond());
                    node.output.accumulator.accept(in.getFirst(), entry.getSecond());
                    continue;
                }

                TOut out = visited.get(in.getSecond()); //handle already-visited non-scalar nodes
                if(out != null) {
                    node.output.accumulator.accept(in.getFirst(), out);
                    continue;
                }

                Node<TIn, TOut, TKey> newNode = nodeFunction.apply(in.getSecond());
                visited.put(in.getSecond(), newNode.output.data);
                stack.push(newNode);
                node.output.accumulator.accept(in.getFirst(), newNode.output.data);
            }
        }

        return root.output.data;
    }

    public static <TIn, TOut, TKey> TOut process(TIn input,
                                                 @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
                                                 @NotNull Predicate<? super TIn> containerPredicate,
                                                 @NotNull Function<? super TIn, ? extends Entry<TKey, TOut>> scalarMapper)  {
        return process(input, new ArrayDeque<>(), new IdentityHashMap<>(), nodeFunction, containerPredicate, scalarMapper);
    }
}