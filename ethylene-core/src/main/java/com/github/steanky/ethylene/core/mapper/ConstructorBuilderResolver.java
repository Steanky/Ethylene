package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

public class ConstructorBuilderResolver implements BuilderResolver {
    @Override
    public @NotNull ObjectBuilder forType(@NotNull Type type, @NotNull ConfigContainer container) {
        Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);

        //TODO: handle abstract classes/interfaces
        Constructor<?>[] constructors = resolved.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors) {

        }

        return null;
    }
}
