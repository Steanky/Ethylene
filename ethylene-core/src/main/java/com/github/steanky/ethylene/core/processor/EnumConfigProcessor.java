package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * General ConfigProcessor implementation for serializing and deserializing enums.
 *
 * @param <TEnum> the type of enum to serialize/deserialize
 */
class EnumConfigProcessor<TEnum extends Enum<?>> implements ConfigProcessor<TEnum> {
    private final String className;
    private final boolean caseSensitive;
    private final Map<String, TEnum> map;

    /**
     * Creates a new EnumConfigProcessor, which will be able to process instances of the provided enum class. The
     * processor will be case-sensitive. No strong reference to the provided class will be retained.
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
        this.className = enumClass.getName();
        this.caseSensitive = caseSensitive;

        TEnum[] constants = enumClass.getEnumConstants();
        @SuppressWarnings("unchecked") Map.Entry<String, TEnum>[] entries = new Map.Entry[constants.length];
        for (int i = 0; i < entries.length; i++) {
            TEnum constant = constants[i];
            String string = caseSensitive ? constant.name() : constant.name().toLowerCase(Locale.ROOT);
            entries[i] = Map.entry(string, constant);
        }

        map = Map.ofEntries(entries);
    }

    @Override
    public TEnum dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        if (element.isNull()) {
            return null;
        }

        if (!element.isString()) {
            throw new ConfigProcessException("Element must be a string");
        }

        String elementString = element.asString();
        TEnum result = lookup(elementString);
        if (result == null) {
            throw new ConfigProcessException(
                "No enum constant named '" + elementString + "' in " + className);
        }

        return result;
    }

    @Override
    public @NotNull ConfigElement elementFromData(TEnum data) {
        if (data == null) {
            return ConfigPrimitive.NULL;
        }

        return ConfigPrimitive.of(data.name());
    }

    private TEnum lookup(String name) {
        return map.get(caseSensitive ? name : name.toLowerCase(Locale.ROOT));
    }
}
