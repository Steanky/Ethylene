package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstructorBuilderResolver implements BuilderResolver {
    private final AbstractClassResolver abstractClassResolver;
    private final TypeHinter typeHinter;

    public ConstructorBuilderResolver(@Nullable AbstractClassResolver abstractClassResolver,
                                      @NotNull TypeHinter typeHinter) {
        this.abstractClassResolver = abstractClassResolver;
        this.typeHinter = Objects.requireNonNull(typeHinter);
    }

    @Override
    public @NotNull ObjectBuilder forType(@NotNull Type type, @NotNull ConfigContainer container) {
        TypeHinter.TypeHint hint = typeHinter.getHint(type);
        Collection<ConfigEntry> entries = container.entryCollection();

        switch (hint) {
            case LIST_LIKE -> {
                return new ListObjectBuilder(entries.size(), ReflectionUtils.isArray(type));
            }
            case MAP_LIKE -> {

            }
            case SCALAR -> throw new MappingException("Cannot map a container to a scalar");
        }

        Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);
        if(abstractClassResolver != null && resolved.isInterface()) {
            //can't construct abstract classes
            resolved = abstractClassResolver.resolveAbstract(resolved);
        }

        Constructor<?>[] constructors = resolved.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors) {
            if(constructorMatches(constructor, entries)) {
                return new ConstructorObjectBuilder(constructor);
            }
        }

        throw new MappingException("Unable to find suitable constructor for " + type.getTypeName());
    }

    private boolean constructorMatches(Constructor<?> constructor, Collection<ConfigEntry> entries) {
        Parameter[] parameters = constructor.getParameters();
        if(entries.size() != parameters.length) {
            return false;
        }

        Map<String, Type> typeMap = new HashMap<>(parameters.length);
        for(Parameter parameter : parameters) {
            typeMap.put(parameter.getName(), parameter.getParameterizedType());
        }

        for(ConfigEntry entry : entries) {
            if(!typeHinter.getHint(typeMap.get(entry.getFirst())).matches(entry.getSecond())) {
                return false;
            }
        }

        return true;
    }
}
