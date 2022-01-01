package com.steank.ethylene.codec;

import com.steank.ethylene.ConfigElement;
import com.steank.ethylene.ConfigPrimitive;
import com.steank.ethylene.collection.ArrayConfigList;
import com.steank.ethylene.collection.ConfigNode;
import com.steank.ethylene.collection.LinkedConfigNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractConfigCodec implements ConfigCodec {
    private final Set<String> names;

    public AbstractConfigCodec(@NotNull Collection<String> names) {
        this.names = Set.copyOf(names);
    }

    @Override
    public <TMap extends Map<String, Object>> void encodeNode(@NotNull ConfigNode node, @NotNull OutputStream output,
                                                              boolean close, @NotNull Supplier<TMap> mapSupplier)
            throws IOException {
        try {
            writeMap(makeMap(node, mapSupplier), output);
        }
        finally {
            if(close) {
                output.close();
            }
        }
    }

    @Override
    public <TNode extends ConfigNode> @NotNull TNode decodeNode(@NotNull InputStream input, boolean close,
                                                                @NotNull Supplier<TNode> nodeSupplier)
            throws IOException {
        try {
            return makeNode(readMap(input), nodeSupplier);
        }
        finally {
            if(close) {
                input.close();
            }
        }
    }

    @Override
    public @NotNull Set<String> getNames() {
        return names;
    }

    /**
     * Used to keep track of the parent container when traversing a nested hierarchy.
     * @param <TOut> the type of object we are outputting
     */
    protected record Node<TOut>(@NotNull Object inputContainer, @NotNull BiConsumer<String, TOut> output) {}

    //returns true if parameter subclasses Map, Collection, or is an array; otherwise returns false
    @Contract("null -> false")
    protected boolean isContainer(@Nullable Object object) {
        return object != null && (object instanceof Map<?, ?> || object instanceof Collection<?> ||
                object.getClass().isArray());
    }

    protected <TMap extends Map<String, TOut>,
            TCollection extends Collection<TOut>,
            TOut> void processValue(@Nullable Object value,
                                    @NotNull Deque<Node<TOut>> stack,
                                    @NotNull Set<Object> visited,
                                    @NotNull Node<TOut> currentNode,
                                    @Nullable String keyString,
                                    @NotNull Supplier<TMap> mapSupplier,
                                    @NotNull Supplier<TCollection> collectionSupplier,
                                    @NotNull Function<Object, TOut> converter) {
        if(isContainer(value)) {
            if(visited.add(value)) {
                BiConsumer<String, TOut> consumer;
                Object output;

                if(value instanceof Map<?, ?>) {
                    TMap newMap = mapSupplier.get();
                    output = newMap;
                    consumer = newMap::put;
                }
                else  {
                    TCollection newCollection = collectionSupplier.get();
                    output = newCollection;
                    consumer = (k, v) -> newCollection.add(v);
                }

                stack.push(new Node<>(value, consumer));
                currentNode.output.accept(keyString, converter.apply(output));
            }
        }
        else {
            currentNode.output.accept(keyString, converter.apply(value));
        }
    }

    protected <TRootMap extends Map<String, TOut>,
            TSubMap extends Map<String, TOut>,
            TCollection extends Collection<TOut>,
            TOut> @NotNull TRootMap processMap(@NotNull Map<String, ?> input,
                                               @NotNull Supplier<TRootMap> rootMapSupplier,
                                               @NotNull Supplier<TSubMap> subMapSupplier,
                                               @NotNull Supplier<TCollection> collection,
                                               @NotNull Function<Object, TOut> converter) {
        TRootMap topLevel = Objects.requireNonNull(rootMapSupplier.get(), "root map cannot be null");

        Deque<Node<TOut>> stack = new ArrayDeque<>();
        stack.push(new Node<>(input, topLevel::put));

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(topLevel);

        while(!stack.isEmpty()) {
            Node<TOut> node = stack.pop();

            if(node.inputContainer instanceof Map<?, ?> inputMap) {
                for(Map.Entry<?, ?> entry : inputMap.entrySet()) {
                    if(entry.getKey() instanceof String key) {
                        processValue(entry.getValue(), stack, visited, node, key, subMapSupplier, collection,
                                converter);
                    }
                    else {
                        throw new IllegalArgumentException("Map keys may only be strings.");
                    }
                }
            }
            else if(node.inputContainer instanceof Collection<?> inputCollection) {
                for(Object value : inputCollection) {
                    processValue(value, stack, visited, node, null, subMapSupplier, collection, converter);
                }
            }
            else {
                //inputContainer must be an array
                int length = Array.getLength(node.inputContainer);

                for(int i = 0; i < length; i++) {
                    processValue(Array.get(node.inputContainer, i), stack, visited, node, null, subMapSupplier,
                            collection, converter);
                }
            }
        }

        return topLevel;
    }

    protected <TNode extends ConfigNode> @NotNull TNode makeNode(@NotNull Map<String, Object> mappings,
                                                                 @NotNull Supplier<TNode> nodeSupplier) {
        Objects.requireNonNull(mappings);
        Objects.requireNonNull(nodeSupplier);

        return processMap(mappings, nodeSupplier, LinkedConfigNode::new, ArrayConfigList::new, value -> {
            if(value instanceof ConfigElement element) {
                return element;
            }
            else {
                //if value is of a type unrecognized by ConfigPrimitive, a runtime exception may be thrown here which
                //indicates some unsupported type
                return new ConfigPrimitive(value);
            }
        });
    }

    protected <TMap extends Map<String, Object>> @NotNull TMap makeMap(@NotNull ConfigNode node,
                                                                       @NotNull Supplier<TMap> mapSupplier) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(mapSupplier);

        return processMap(node, mapSupplier, LinkedHashMap::new, ArrayList::new, value -> {
            if(value instanceof ConfigPrimitive primitive) {
                return primitive.getObject();
            }
            else {
                return value;
            }
        });
    }

    protected abstract @NotNull Map<String, Object> readMap(@NotNull InputStream input) throws IOException;

    protected abstract void writeMap(@NotNull Map<String, Object> mappings, @NotNull OutputStream output)
            throws IOException;
}