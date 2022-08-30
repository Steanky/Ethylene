package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * General ConfigProcessor implementation for serializing and deserializing enums.
 *
 * @param <TEnum> the type of enum to serialize/deserialize
 */
class EnumConfigProcessor<TEnum extends Enum<?>> implements ConfigProcessor<TEnum> {
    private final Class<? extends TEnum> enumClass;
    private final boolean caseSensitive;

    private Function<String, TEnum> lookupFunction;

    /**
     * Creates a new EnumConfigProcessor, which will be able to process instances of the provided enum class. The
     * processor will be case-sensitive.
     *
     * @param enumClass the enum class used to provide a list of enum constants
     */
    EnumConfigProcessor(@NotNull Class<? extends TEnum> enumClass) {
        this(enumClass, true);
    }

    /**
     * Creates a new EnumConfigProcessor, which will be able to process instances of the provided enum class, and with
     * the provided case sensitivity handling.
     *
     * @param enumClass     the enum class used to provide a list of enum constants
     * @param caseSensitive whether this processor should be case-sensitive
     */
    EnumConfigProcessor(@NotNull Class<? extends TEnum> enumClass, boolean caseSensitive) {
        this.enumClass = Objects.requireNonNull(enumClass);
        this.caseSensitive = caseSensitive;
    }

    @Override
    public TEnum dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        if (!element.isString()) {
            throw new ConfigProcessException("Element must be a string");
        }

        String elementString = element.asString();
        TEnum result = lookup(elementString);
        if (result == null) {
            throw new ConfigProcessException(
                    "No enum constant named '" + elementString + "' in " + enumClass.getTypeName());
        }

        return result;
    }

    @Override
    public @NotNull ConfigElement elementFromData(TEnum data) throws ConfigProcessException {
        if (data == null) {
            throw new ConfigProcessException("Cannot convert null enum constant to a ConfigElement");
        }

        return new ConfigPrimitive(data.toString());
    }

    private TEnum lookup(String name) {
        if (lookupFunction == null) {
            TEnum[] constants = enumClass.getEnumConstants();

            //for tiny enums, we can save a bit of memory by not using a hashmap
            if (constants.length > 10) {
                Map<String, TEnum> lookupMap = new HashMap<>(constants.length);
                for (TEnum constant : constants) {
                    lookupMap.put(caseSensitive ? constant.name() : constant.name().toLowerCase(Locale.ROOT), constant);
                }

                lookupFunction = caseSensitive ? lookupMap::get : s -> lookupMap.get(s.toLowerCase(Locale.ROOT));
            } else {
                lookupFunction = constantName -> {
                    for (TEnum constant : constants) {
                        if (caseSensitive ? constant.name().equals(constantName) :
                                constant.name().toLowerCase(Locale.ROOT)
                                        .equals(constantName.toLowerCase(Locale.ROOT))) {
                            return constant;
                        }
                    }

                    return null;
                };
            }
        }

        return lookupFunction.apply(name);
    }
}
