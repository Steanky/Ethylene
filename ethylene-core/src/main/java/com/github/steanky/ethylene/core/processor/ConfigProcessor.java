package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Processes some configuration data. Fundamentally, implementations of this interface act as simple bidirectional
 * mapping functions between {@link ConfigElement} instances and arbitrary data.
 *
 * @param <TData> the type of data to convert to and from
 */
public interface ConfigProcessor<TData> {
    /**
     * Built-in ConfigProcessor implementation for ConfigNodes.
     */
    ConfigProcessor<ConfigNode> CONFIG_NODE = new ConfigProcessor<>() {
        @Override
        public ConfigNode dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if (!element.isNode()) {
                throw new ConfigProcessException("Element must be a node");
            }

            return element.asNode();
        }

        @Override
        public @NotNull ConfigElement elementFromData(ConfigNode node) {
            return node;
        }
    };

    /**
     * Built-in ConfigProcessor implementation for ConfigLists.
     */
    ConfigProcessor<ConfigList> CONFIG_LIST = new ConfigProcessor<>() {
        @Override
        public ConfigList dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if (!element.isList()) {
                throw new ConfigProcessException("Element must be a list");
            }

            return element.asList();
        }

        @Override
        public @NotNull ConfigElement elementFromData(ConfigList list) {
            return list;
        }
    };

    /**
     * Built-in ConfigProcessor implementation for ConfigContainers.
     */
    ConfigProcessor<ConfigContainer> CONFIG_CONTAINER = new ConfigProcessor<>() {
        @Override
        public ConfigContainer dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if (!element.isContainer()) {
                throw new ConfigProcessException("Element must be a container");
            }

            return element.asContainer();
        }

        @Override
        public @NotNull ConfigElement elementFromData(ConfigContainer container) {
            return container;
        }
    };

    /**
     * Built-in ConfigProcessor implementation for strings.
     */
    ConfigProcessor<String> STRING = new ConfigProcessor<>() {
        @Override
        public String dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if (!element.isString()) {
                throw new ConfigProcessException("Element must be a string");
            }

            return element.asString();
        }

        @Override
        public @NotNull ConfigElement elementFromData(String s) {
            return new ConfigPrimitive(s);
        }
    };

    /**
     * Built-in ConfigProcessor implementation for numbers.
     */
    ConfigProcessor<Number> NUMBER = new NumberConfigProcessor<>(Function.identity());

    /**
     * Built-in ConfigProcessor implementation for longs.
     */
    ConfigProcessor<Long> LONG = new NumberConfigProcessor<>(Number::longValue);

    /**
     * Built-in ConfigProcessor implementation for doubles.
     */
    ConfigProcessor<Double> DOUBLE = new NumberConfigProcessor<>(Number::doubleValue);

    /**
     * Built-in ConfigProcessor implementation for integers.
     */
    ConfigProcessor<Integer> INTEGER = new NumberConfigProcessor<>(Number::intValue);

    /**
     * Built-in ConfigProcessor implementation for floats.
     */
    ConfigProcessor<Float> FLOAT = new NumberConfigProcessor<>(Number::floatValue);

    /**
     * Built-in ConfigProcessor implementation for shorts.
     */
    ConfigProcessor<Short> SHORT = new NumberConfigProcessor<>(Number::shortValue);

    /**
     * Built-in ConfigProcessor implementation for bytes.
     */
    ConfigProcessor<Byte> BYTE = new NumberConfigProcessor<>(Number::byteValue);

    /**
     * Built-in ConfigProcessor implementation for booleans.
     */
    ConfigProcessor<Boolean> BOOLEAN = new ConfigProcessor<>() {
        @Override
        public Boolean dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if (!element.isBoolean()) {
                throw new ConfigProcessException("Element is not a boolean");
            }

            return element.asBoolean();
        }

        @Override
        public @NotNull ConfigElement elementFromData(Boolean b) {
            return new ConfigPrimitive(b);
        }
    };

    /**
     * Creates a new ConfigProcessor capable of converting enum constants from the specified enum class. The returned
     * processor will use case-sensitive conversions (the string "ENUM_CONSTANT" is not treated the same as
     * "enum_constant").
     *
     * @param enumClass the class from which to extract enum constants
     * @param <TEnum>   the type of enum to convert
     * @return a ConfigProcessor which can convert enum constants
     */
    static <TEnum extends Enum<?>> @NotNull ConfigProcessor<TEnum> enumProcessor(
        @NotNull Class<? extends TEnum> enumClass) {
        return new EnumConfigProcessor<>(enumClass);
    }

    /**
     * Creates a new ConfigProcessor capable of converting enum constants from the specified enum class, with the
     * provided case sensitivity when converting strings to enum instances.
     *
     * @param enumClass     the class from which to extract enum constants
     * @param caseSensitive whether string comparisons are case-sensitive
     * @param <TEnum>       the type of enum to convert
     * @return a ConfigProcessor which can convert enum constants
     */
    static <TEnum extends Enum<?>> @NotNull ConfigProcessor<TEnum> enumProcessor(
        @NotNull Class<? extends TEnum> enumClass, boolean caseSensitive) {
        return new EnumConfigProcessor<>(enumClass, caseSensitive);
    }

    /**
     * Produces a minimal ConfigProcessor whose {@link ConfigProcessor#elementFromData(Object)} method always returns a
     * new empty {@link ConfigNode} implementation, and whose {@link ConfigProcessor#dataFromElement(ConfigElement)}
     * function calls the provided supplier to obtain data objects.
     *
     * @param returnSupplier the supplier of data objects used
     * @param <TReturn>      the type of value to process
     * @return a minimal ConfigProcessor implementation
     */
    static <TReturn> @NotNull ConfigProcessor<TReturn> emptyProcessor(
        @NotNull Supplier<? extends TReturn> returnSupplier) {
        return new ConfigProcessor<>() {
            @Override
            public TReturn dataFromElement(@NotNull ConfigElement element) {
                return returnSupplier.get();
            }

            @Override
            public @NotNull ConfigElement elementFromData(TReturn tReturn) {
                return new LinkedConfigNode(0);
            }
        };
    }

    /**
     * Creates a ConfigProcessor implementation capable of serializing and deserializing Map objects whose keys are not
     * necessarily string-valued. Elements are expected to be {@link ConfigList}s of "entries", which are single
     * {@link ConfigNode} objects containing exactly two entries, a "key" entry and a "value" entry.
     *
     * @param keyProcessor   the processor used to serialize/deserialize keys
     * @param valueProcessor the processor used to serialize/deserialize values
     * @param mapFunction    the function used to construct the desired map implementation
     * @param <TKey>         the key type
     * @param <TValue>       the value type
     * @param <TMap>         the map type
     * @return a new ConfigProcessor which can serialize/deserialize the desired kind of map
     */
    static <TKey, TValue, TMap extends Map<TKey, TValue>> @NotNull ConfigProcessor<TMap> mapProcessor(
        @NotNull ConfigProcessor<TKey> keyProcessor, @NotNull ConfigProcessor<TValue> valueProcessor,
        @NotNull IntFunction<? extends TMap> mapFunction) {
        Objects.requireNonNull(keyProcessor);
        Objects.requireNonNull(valueProcessor);
        Objects.requireNonNull(mapFunction);

        return new ConfigProcessor<>() {
            @Override
            public TMap dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                ConfigList list = CONFIG_LIST.dataFromElement(element);
                TMap map = mapFunction.apply(list.size());
                for (ConfigElement entry : element.asList()) {
                    if (!entry.isNode()) {
                        throw new ConfigProcessException("All entries must be nodes");
                    }

                    ConfigNode entryNode = entry.asNode();
                    map.put(keyProcessor.dataFromElement(entryNode.getElementOrThrow("key")),
                        valueProcessor.dataFromElement(entryNode.getElementOrThrow("value")));
                }

                return map;
            }

            @Override
            public @NotNull ConfigElement elementFromData(TMap map) throws ConfigProcessException {
                ConfigList list = new ArrayConfigList(map.size());
                for (Map.Entry<TKey, TValue> mapEntry : map.entrySet()) {
                    ConfigNode nodeEntry = new LinkedConfigNode(2);
                    nodeEntry.put("key", keyProcessor.elementFromData(mapEntry.getKey()));
                    nodeEntry.put("value", valueProcessor.elementFromData(mapEntry.getValue()));
                    list.add(nodeEntry);
                }

                return list;
            }
        };
    }

    /**
     * Produces some data from a provided {@link ConfigElement}.
     *
     * @param element the element to process
     * @return the data object
     * @throws ConfigProcessException if the provided {@link ConfigElement} does not contain valid data
     */
    TData dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException;

    /**
     * Produces a {@link ConfigElement} from the provided data object.
     *
     * @param data the data object
     * @return a {@link ConfigElement} representing the given data
     * @throws ConfigProcessException if the data is invalid
     */
    @NotNull ConfigElement elementFromData(TData data) throws ConfigProcessException;

    /**
     * Convenience method that calls {@link ConfigProcessor#mapProcessor(IntFunction)} with {@code HashMap::new}.
     *
     * @return a new ConfigProcessor capable of converting {@link ConfigElement} instances to a Map
     */
    default @NotNull ConfigProcessor<Map<String, TData>> mapProcessor() {
        return mapProcessor(HashMap::new);
    }

    /**
     * Creates a new ConfigProcessor capable of converting {@link ConfigElement} instances to String-keyed Map objects,
     * and vice-versa.
     *
     * @param mapFunction the function used to instantiate new maps
     * @param <M>         the type of map
     * @return a new ConfigProcessor capable of converting {@link ConfigElement} instances to String-keyed Map objects
     */
    default <M extends Map<String, TData>> @NotNull ConfigProcessor<M> mapProcessor(
        @NotNull IntFunction<? extends M> mapFunction) {
        Objects.requireNonNull(mapFunction, "mapFunction");

        return new ConfigProcessor<>() {
            @Override
            public M dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                ConfigNode node = CONFIG_NODE.dataFromElement(element);
                M map = mapFunction.apply(node.size());
                for (ConfigEntry entry : node.entryCollection()) {
                    map.put(entry.getKey(), ConfigProcessor.this.dataFromElement(entry.getValue()));
                }

                return map;
            }

            @Override
            public @NotNull ConfigElement elementFromData(M m) throws ConfigProcessException {
                ConfigNode node = new LinkedConfigNode(m.size());
                for (Map.Entry<String, TData> entry : m.entrySet()) {
                    node.put(entry.getKey(), ConfigProcessor.this.elementFromData(entry.getValue()));
                }

                return node;
            }
        };
    }

    /**
     * Convenience overload for {@link ConfigProcessor#collectionProcessor(IntFunction)} which uses
     * {@code ArrayList::new} for its IntFunction.
     *
     * @return a list ConfigProcessor
     */
    default @NotNull ConfigProcessor<List<TData>> listProcessor() {
        return collectionProcessor(ArrayList::new);
    }

    /**
     * Creates a new ConfigProcessor capable of processing some type of collection which holds elements whose type is
     * assignable to the type of data this ConfigProcessor converts.
     *
     * @param collectionSupplier the function which will produce new collections
     * @param <TCollection>      the type of collection to create
     * @return a new ConfigProcessor which can process collections of elements
     */
    default <TCollection extends Collection<TData>> @NotNull ConfigProcessor<TCollection> collectionProcessor(
        @NotNull IntFunction<? extends TCollection> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier);

        return new ConfigProcessor<>() {
            @Override
            public TCollection dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                ConfigList list = CONFIG_LIST.dataFromElement(element);
                TCollection container = collectionSupplier.apply(list.size());
                for (ConfigElement sample : list) {
                    container.add(ConfigProcessor.this.dataFromElement(sample));
                }

                return container;
            }

            @Override
            public @NotNull ConfigElement elementFromData(TCollection container) throws ConfigProcessException {
                ConfigList list = new ArrayConfigList(container.size());
                for (TData data : container) {
                    list.add(ConfigProcessor.this.elementFromData(data));
                }

                return list;
            }
        };
    }

    /**
     * Convenience overload for {@link ConfigProcessor#collectionProcessor(IntFunction)} which uses
     * {@code ArrayList::new} for its IntFunction.
     *
     * @return a collection ConfigProcessor
     */
    default @NotNull ConfigProcessor<Collection<TData>> collectionProcessor() {
        return collectionProcessor(ArrayList::new);
    }

    /**
     * Convenience overload for {@link ConfigProcessor#collectionProcessor(IntFunction)} which uses {@code HashSet::new}
     * for its IntFunction.
     *
     * @return a set ConfigProcessor
     */
    default @NotNull ConfigProcessor<Set<TData>> setProcessor() {
        return collectionProcessor(HashSet::new);
    }

    /**
     * Creates a new ConfigProcessor capable of processing arrays whose component type is the same as this
     * ConfigProcessor's data type. Works similarly to {@link ConfigProcessor#collectionProcessor(IntFunction)}, but for
     * arrays.
     *
     * @return a new array-based ConfigProcessor
     */
    default @NotNull ConfigProcessor<TData[]> arrayProcessor() {
        return new ConfigProcessor<>() {
            @SuppressWarnings("unchecked")
            @Override
            public TData[] dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                ConfigList list = CONFIG_LIST.dataFromElement(element);
                TData[] data = (TData[]) new Object[list.size()];
                int i = 0;
                for (ConfigElement sample : list) {
                    data[i++] = ConfigProcessor.this.dataFromElement(sample);
                }

                return data;
            }

            @Override
            public @NotNull ConfigElement elementFromData(TData[] data) throws ConfigProcessException {
                ConfigList list = new ArrayConfigList(data.length);
                for (TData sample : data) {
                    list.add(ConfigProcessor.this.elementFromData(sample));
                }

                return list;
            }
        };
    }

    /**
     * Creates a new ConfigProcessor capable of processing optional data. If this ConfigProcessor's
     * {@code dataFromElement} method returns null, the returned processor's optional will be empty, else it will
     * contain the value returned from {@code dataFromElement}. Likewise, when calling {@code elementFromData} with the
     * given optional, if empty, a null-holding {@link ConfigPrimitive} instance will be returned, otherwise, the
     * element will contain the result of calling {@code elementFromData}.
     *
     * @return a ConfigProcessor capable of processing optional data
     */
    default @NotNull ConfigProcessor<Optional<TData>> optionalProcessor() {
        return new ConfigProcessor<>() {
            @Override
            public Optional<TData> dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                return Optional.ofNullable(ConfigProcessor.this.dataFromElement(element));
            }

            @Override
            public @NotNull ConfigElement elementFromData(Optional<TData> data) throws ConfigProcessException {
                if (data.isPresent()) {
                    return ConfigProcessor.this.elementFromData(data.get());
                }

                return ConfigPrimitive.NULL;
            }
        };
    }
}
