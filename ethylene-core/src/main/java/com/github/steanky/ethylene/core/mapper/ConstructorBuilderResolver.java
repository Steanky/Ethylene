package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Objects;

public class ConstructorBuilderResolver implements BuilderResolver {
    private final AbstractClassResolver abstractClassResolver;

    public ConstructorBuilderResolver(@NotNull AbstractClassResolver abstractClassResolver) {
        this.abstractClassResolver = Objects.requireNonNull(abstractClassResolver);
    }

    @Override
    public @NotNull ObjectBuilder forType(@NotNull Type type, @NotNull ConfigContainer container) {
        Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);

        if(resolved.isInterface()) {
            //can't construct abstract classes
            resolved = abstractClassResolver.resolveAbstract(resolved);
        }

        //TODO: handle abstract classes/interfaces
        Constructor<?>[] constructors = resolved.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors) {

        }

        return null;
    }
}
