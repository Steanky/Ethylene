package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

public class ConstructorSignature implements Signature {
    private final Constructor<?> constructor;
    private Type[] types;

    public ConstructorSignature(@NotNull Constructor<?> constructor) {
        this.constructor = Objects.requireNonNull(constructor);
    }

    @Override
    public boolean matches(@NotNull List<Entry<String, Type>> arguments) {
        Type[] types = getTypes();

        return false;
    }

    private Type[] getTypes() {
        if(types == null) {
            types = constructor.getGenericParameterTypes();
        }

        return types;
    }
}
