package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * General ConfigProcessor implementation for serializing and deserializing enums.
 * @param <TEnum> the type of enum to serialize/deserialize
 */
class EnumConfigProcessor<TEnum extends Enum<?>> implements ConfigProcessor<TEnum> {
    private Reference<Class<? extends TEnum>> enumClassReference;
    private final String className;
    private final boolean caseSensitive;

    private Function<String, TEnum> lookupFunction;

    /**
     * Creates a new EnumConfigProcessor, which will be able to process instances of the provided enum class. The
     * processor will be case-sensitive.
     * @param enumClass the enum class used to provide a list of enum constants
     */
    EnumConfigProcessor(@NotNull Class<? extends TEnum> enumClass) {
        this(enumClass, true);
    }

    /**
     * Creates a new EnumConfigProcessor, which will be able to process instances of the provided enum class, and with
     * the provided case sensitivity handling.
     * @param enumClass the enum class used to provide a list of enum constants
     * @param caseSensitive whether this processor should be case-sensitive
     */
    EnumConfigProcessor(@NotNull Class<? extends TEnum> enumClass, boolean caseSensitive) {
        this.enumClassReference = new WeakReference<>(enumClass);
        this.className = enumClass.getName();
        this.caseSensitive = caseSensitive;
    }

    @Override
    public TEnum dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        if(!element.isString()) {
            throw new ConfigProcessException("Element must be a string");
        }

        String elementString = element.asString();
        TEnum result = lookup(elementString);
        if(result == null) {
            Class<? extends TEnum> enumClass = getEnumClass();
            throw new ConfigProcessException("No enum constant named '" + elementString + "' in " + enumClass
                    .getTypeName());
        }

        return result;
    }

    @Override
    public @NotNull ConfigElement elementFromData(TEnum data) throws ConfigProcessException {
        if(data == null) {
            throw new ConfigProcessException("Cannot convert null enum constant to a ConfigElement");
        }

        return new ConfigPrimitive(data.toString());
    }

    private TEnum lookup(String name) {
        if(lookupFunction == null) {
            Class<? extends TEnum> enumClass = getEnumClass();
            TEnum[] constants = enumClass.getEnumConstants();

            //for tiny enums, we can save a bit of memory by not using a hashmap
            if(constants.length > 10) {
                Map<String, TEnum> lookupMap = new HashMap<>(constants.length);
                for(TEnum constant : constants) {
                    lookupMap.put(caseSensitive ? constant.name() : constant.name().toLowerCase(Locale.ROOT), constant);
                }

                lookupFunction = caseSensitive ? lookupMap::get : s -> lookupMap.get(s.toLowerCase(Locale.ROOT));
            }
            else {
                lookupFunction = constantName -> {
                    for(TEnum constant : constants) {
                        if(caseSensitive ? constant.name().equals(constantName) : constant.name()
                                .toLowerCase(Locale.ROOT).equals(constantName.toLowerCase(Locale.ROOT))) {
                            return constant;
                        }
                    }

                    return null;
                };
            }
        }

        return lookupFunction.apply(name);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends TEnum> getEnumClass() {
        Class<? extends TEnum> cls = enumClassReference.get();
        if (cls == null) {
            try {
                cls = (Class<? extends TEnum>) Class.forName(className);
                enumClassReference = new WeakReference<>(cls);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return cls;
    }
}
