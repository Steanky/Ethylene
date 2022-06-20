package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

public class ConstructorBuilderResolver implements BuilderResolver {
    private final AbstractClassResolver abstractClassResolver;
    private final TypeHinter typeHinter;

    public ConstructorBuilderResolver(@NotNull AbstractClassResolver abstractClassResolver,
                                      @NotNull TypeHinter typeHinter) {
        this.abstractClassResolver = Objects.requireNonNull(abstractClassResolver);
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
        if(resolved.isInterface()) {
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
        Type[] parameters = constructor.getGenericParameterTypes();
        if(entries.size() != parameters.length) {
            throw new MappingException("Element size mismatch");
        }

        int i = 0;
        for(ConfigEntry entry : entries) {
            if(!typeHinter.getHint(parameters[i]).matches(entry.getSecond())) {
                return false;
            }
            i++;
        }

        return true;
    }
}
