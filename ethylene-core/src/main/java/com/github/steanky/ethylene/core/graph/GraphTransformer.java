package com.github.steanky.ethylene.core.graph;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This utility class can be used to aid the process of designing generalized object graph transformations.
 * @param <TIn> The input type
 * @param <TOut> The output type
 */
public class GraphTransformer<TIn, TOut> {
    public record Node<TIn, TOut>(TIn in,
                                  TOut out,
                                  @NotNull Iterable<? extends TIn> inputIterable,
                                  @NotNull Consumer<? super TOut> accumulator) {}

    private final Function<? super TIn, ? extends Node<TIn, TOut>> nodeFunction;
    private final Predicate<? super TIn> scalarPredicate;
    private final Function<? super TIn, ? extends TOut> scalarMapper;

    public GraphTransformer(@NotNull Function<? super TIn, ? extends Node<TIn, TOut>> nodeFunction,
                            @NotNull Predicate<? super TIn> scalarPredicate,
                            @NotNull Function<? super TIn, ? extends TOut> scalarMapper) {
        this.nodeFunction = Objects.requireNonNull(nodeFunction);
        this.scalarPredicate = Objects.requireNonNull(scalarPredicate);
        this.scalarMapper = Objects.requireNonNull(scalarMapper);
    }

    public TOut processRoot(TIn input, @NotNull Deque<Node<TIn, TOut>> stack, @NotNull Map<TIn, TOut> visited) {
        Objects.requireNonNull(stack);
        Objects.requireNonNull(visited);

        if(scalarPredicate.test(input)) {
            return scalarMapper.apply(input);
        }

        Node<TIn, TOut> node = nodeFunction.apply(input);
        stack.push(node);

        visited.put(input, node.out);

        while(!stack.isEmpty()) {
            processNode(stack.pop(), stack, visited);
        }

        return node.out;
    }

    public TOut processRoot(TIn input) {
        return processRoot(input, new ArrayDeque<>(), new IdentityHashMap<>());
    }

    private void processNode(@NotNull Node<TIn, TOut> node,
                             @NotNull Deque<Node<TIn, TOut>> stack,
                             @NotNull Map<TIn, TOut> visited) {
        for(TIn in : node.inputIterable) {
            if(scalarPredicate.test(in)) {
                node.accumulator.accept(scalarMapper.apply(in));
                continue;
            }

            TOut matching = visited.get(in); //handle already-visited non-scalar nodes
            if(matching != null) {
                node.accumulator.accept(matching);
                continue;
            }

            Node<TIn, TOut> newNode = nodeFunction.apply(in);
            stack.push(newNode);
            visited.put(in, newNode.out);

            node.accumulator.accept(newNode.out);
        }
    }
}
