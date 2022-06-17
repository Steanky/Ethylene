package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ConstructorBuilderResolver implements BuilderResolver {
    private final Predicate<Constructor<?>> filter;

    public ConstructorBuilderResolver(@NotNull Predicate<Constructor<?>> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public @NotNull ObjectBuilder[] forType(@NotNull Type type) {
        Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);

        //TODO: handle abstract classes/interfaces
        Constructor<?>[] constructors = resolved.getDeclaredConstructors();
        List<ObjectBuilder> builders = new ArrayList<>(constructors.length);

        for(Constructor<?> constructor : constructors) {
            if(filter.test(constructor)) {
                builders.add(new ConstructorObjectBuilder(constructor));
            }
        }

        return builders.toArray(ObjectBuilder[]::new);
    }
}
