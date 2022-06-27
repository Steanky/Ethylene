package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Processes some configuration data. Fundamentally, implementations of this interface act as simple bidirectional
 * mapping functions between {@link ConfigElement} instances and arbitrary data.
 * @param <TData> the type of data to convert to and from
 */
public interface ConfigProcessor<TData> {
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
    static <TEnum extends Enum<?>> @NotNull ConfigProcessor<TEnum> newEnumProcessor(@NotNull Class<? extends TEnum> enumClass) {
        return new EnumConfigProcessor<>(enumClass);
    }

    /**
     * Creates a new ConfigProcessor capable of processing some type of collection which holds elements whose type is
     * assignable to the type of data this ConfigProcessor converts.
     * @param collectionSupplier the function which will produce new collections
     * @return a new ConfigProcessor which can process collections of elements
     * @param <TCollection> the type of collection to create
     */
    default <TCollection extends Collection<TData>> @NotNull ConfigProcessor<TCollection> toCollectionProcessor(
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
}
