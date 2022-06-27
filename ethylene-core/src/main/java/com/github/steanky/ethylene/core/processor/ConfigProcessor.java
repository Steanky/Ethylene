package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Processes some configuration data. Fundamentally, implementations of this interface act as simple bidirectional
 * mapping functions between {@link ConfigElement} instances and arbitrary data.
 * @param <TData> the type of data to convert to and from
 */
public interface ConfigProcessor<TData> {
    /**
     * Built-in ConfigProcessor implementation for strings.
     */
    ConfigProcessor<String> STRING = new ConfigProcessor<>() {
        @Override
        public String dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            if(!element.isString()) {
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
     * Produces some data from a provided {@link ConfigElement}.
     * @param element the element to process
     * @return the data object
     * @throws ConfigProcessException if the provided {@link ConfigElement} does not contain valid data
     */
    TData dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException;

    /**
     * Produces a {@link ConfigElement} from the provided data object.
     * @param data the data object
     * @return a {@link ConfigElement} representing the given data
     * @throws ConfigProcessException if the data is invalid
     */
    @NotNull ConfigElement elementFromData(TData data) throws ConfigProcessException;

    /**
     * Creates a new ConfigProcessor capable of converting enum constants from the specified enum class.
     * @param enumClass the class from which to extract enum constants
     * @return a ConfigProcessor which can convert enum constants
     * @param <TEnum> the type of enum to convert
     */
    static <TEnum extends Enum<?>> @NotNull ConfigProcessor<TEnum> enumProcessor(
            @NotNull Class<? extends TEnum> enumClass) {
        return new EnumConfigProcessor<>(enumClass);
    }

    /**
     * Creates a new ConfigProcessor capable of processing some type of collection which holds elements whose type is
     * assignable to the type of data this ConfigProcessor converts.
     * @param collectionSupplier the function which will produce new collections
     * @return a new ConfigProcessor which can process collections of elements
     * @param <TCollection> the type of collection to create
     */
    default <TCollection extends Collection<TData>> @NotNull ConfigProcessor<TCollection> collectionProcessor(
            @NotNull IntFunction<? extends TCollection> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier);

        return new ConfigProcessor<>() {
            @Override
            public TCollection dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                if(!element.isList()) {
                    throw new ConfigProcessException("Element must be a list");
                }

                ConfigList list = element.asList();
                TCollection container = collectionSupplier.apply(list.size());
                for(ConfigElement sample : list) {
                    container.add(ConfigProcessor.this.dataFromElement(sample));
                }

                return container;
            }

            @Override
            public @NotNull ConfigElement elementFromData(TCollection container) throws ConfigProcessException {
                ConfigList list = new ArrayConfigList(container.size());
                for(TData data : container) {
                    list.add(ConfigProcessor.this.elementFromData(data));
                }

                return list;
            }
        };
    }

    /**
     * Convenience overload for {@link ConfigProcessor#collectionProcessor(IntFunction)} which uses
     * {@code ArrayList::new} for its IntFunction.
     * @return a list ConfigProcessor
     */
    default @NotNull ConfigProcessor<List<TData>> listProcessor() {
        return collectionProcessor(ArrayList::new);
    }

    /**
     * Creates a new ConfigProcessor capable of processing arrays whose component type is the same as this
     * ConfigProcessor's data type. Works similarly to {@link ConfigProcessor#collectionProcessor(IntFunction)}, but
     * for arrays.
     * @return a new array-based ConfigProcessor
     */
    default @NotNull ConfigProcessor<TData[]> arrayProcessor() {
        return new ConfigProcessor<>() {
            @Override
            public TData[] dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                if(!element.isList()) {
                    throw new ConfigProcessException("Element must be a list");
                }

                ConfigList list = element.asList();
                //noinspection unchecked
                TData[] data = (TData[]) new Object[list.size()];
                int i = 0;
                for(ConfigElement sample : list) {
                    data[i++] = ConfigProcessor.this.dataFromElement(sample);
                }

                return data;
            }

            @Override
            public @NotNull ConfigElement elementFromData(TData[] data) throws ConfigProcessException {
                ConfigList list = new ArrayConfigList(data.length);
                for(TData sample : data) {
                    list.add(ConfigProcessor.this.elementFromData(sample));
                }

                return list;
            }
        };
    }
}
