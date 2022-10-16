package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Objects;

/**
 * Default scalar signature used to match enums. Not part of the public API.
 *
 * @param <T> the enum type
 */
final class EnumSignature<T extends Enum<T>> extends PrioritizedBase implements ScalarSignature<T> {
    private final Token<T> enumType;

    private Reference<Map<String, T>> constantMap;

    /**
     * Creates a new instance of this signature capable of creating and interpreting enums.
     *
     * @param enumType a token for the enum type
     */
    EnumSignature(@NotNull Token<T> enumType) {
        super(0);
        this.enumType = Objects.requireNonNull(enumType);
        this.constantMap = new SoftReference<>(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, T> getConstantMap() {
        Map<String, T> map = constantMap.get();
        if (map != null) {
            return map;
        }

        Class<T> raw = (Class<T>) enumType.rawType();
        T[] constants = raw.getEnumConstants();

        Map.Entry<String, T>[] entries = new Map.Entry[constants.length];
        for (int i = 0; i < entries.length; i++) {
            T constant = constants[i];
            entries[i] = Map.entry(constant.name(), constant);
        }

        map = Map.ofEntries(entries);
        this.constantMap = new SoftReference<>(map);
        return map;
    }

    @Override
    public @NotNull Token<T> objectType() {
        return enumType;
    }

    @Override
    public @NotNull ElementType elementType() {
        return ElementType.SCALAR;
    }

    @Override
    public @Nullable T createScalar(@NotNull ConfigElement element) {
        if (element.isNull()) {
            return null;
        }

        String name = element.asString();
        T value = getConstantMap().get(name);
        if (value == null) {
            throw new MapperException("Failed to deserialize '" + name + "' to enum type '" + enumType.getTypeName() +
                "', enum constant does not exist");
        }

        return value;
    }

    @Override
    public @NotNull ConfigElement createElement(@Nullable T t) {
        if (t == null) {
            return ConfigPrimitive.NULL;
        }

        return ConfigPrimitive.of(t.name());
    }
}
