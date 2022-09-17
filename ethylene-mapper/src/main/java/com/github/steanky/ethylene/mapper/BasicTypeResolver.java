package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class BasicTypeResolver implements TypeResolver {
    private final TypeHinter typeHinter;

    private final Cache<Class<?>, ClassEntry> typeCache;
    private final Cache<Class<?>, ClassEntry> exactCache;

    public BasicTypeResolver(@NotNull TypeHinter typeHinter,
            @NotNull Collection<Entry<Class<?>, Class<?>>> typeImplementations) {
        this.typeHinter = Objects.requireNonNull(typeHinter);

        int size = typeImplementations.size();
        typeCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();
        exactCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();

        registerTypeImplementations(typeImplementations);
    }

    private void registerTypeImplementations(Collection<Entry<Class<?>, Class<?>>> typeImplementations) {
        for (Entry<Class<?>, Class<?>> entry : typeImplementations) {
            Class<?> implementation = entry.getFirst();
            Class<?> superclass = entry.getSecond();

            Objects.requireNonNull(implementation);
            Objects.requireNonNull(superclass);
            if (!TypeUtils.isAssignable(implementation, superclass)) {
                throw new MapperException(
                        "Implementation class '" + implementation.getTypeName() + "' is not assignable to superclass '" +
                                superclass.getTypeName() + "'");
            }

            ClassEntry newEntry = new ClassEntry(ClassUtils.getName(implementation), implementation);
            if (typeCache.getIfPresent(superclass) != null) {
                throw new MapperException("An implementation class is already registered to superclass '" + superclass
                        .getTypeName() +"'");
            }

            typeCache.put(superclass, newEntry);
            exactCache.put(superclass, newEntry);
        }
    }

    @Override
    public @NotNull Type resolveType(@NotNull Type type, @Nullable ConfigElement element) {
        Objects.requireNonNull(type);

        Class<?> raw = ReflectionUtils.rawType(type);
        ElementType hint = typeHinter.getHint(raw);
        if (raw.isArray() || raw.isPrimitive()) {
            if (element != null && !hint.compatible(element)) {
                throw new MapperException("Incompatible type assignment '" + hint + "' to '" + type + "'");
            }

            return type;
        }

        ClassEntry cached = exactCache.getIfPresent(raw);
        if (cached != null) {
            Class<?> ref = cached.reference.get();
            if (ref != null) {
                if (type instanceof ParameterizedType parameterizedType) {
                    return Token.parameterize(ref, TypeUtils.determineTypeArguments(ref, parameterizedType)).get();
                }

                return ref;
            }

            exactCache.invalidate(raw);
        }

        for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
            ClassEntry entry = typeCache.getIfPresent(superclass);
            if (entry != null) {
                Class<?> ref = entry.reference.get();
                if (ref == null) {
                    throw new MapperException("Class '" + entry.name + "' no longer exists");
                }

                exactCache.put(raw, entry);

                if (type instanceof ParameterizedType parameterizedType) {
                    return Token.parameterize(ref, TypeUtils.determineTypeArguments(ref, parameterizedType)).get();
                }

                return ref;
            }
        }

        if (!Modifier.isAbstract(raw.getModifiers()) && (typeCache.getIfPresent(raw) == null)) {
            if (element == null || hint.compatible(element)) {
                return type;
            }

            //check assignability
            Type elementType = switch (element.type()) {
                case NODE -> raw;
                case LIST -> {
                    Type[] params = ReflectionUtils.extractGenericTypeParameters(type, raw);
                    yield Token.parameterize(ArrayList.class, params.length == 0 ? Object.class : params[0]).get();
                }
                case SCALAR -> {
                    Object scalar = element.asScalar();
                    if (scalar == null) {
                        //null is assignable to anything
                        yield type;
                    }

                    yield scalar.getClass();
                }
            };

            if (!TypeUtils.isAssignable(elementType, type)) {
                throw new MapperException("Element type '" + element.type() + "' not compatible with '" + type + "'");
            }

            return elementType;
        }

        //no type resolution found (but maybe we can still find a suitable signature later)
        return type;
    }

    private static class ClassEntry {
        private final String name;
        private final Reference<Class<?>> reference;

        private ClassEntry(String name, Class<?> type) {
            this.name = name;
            reference = new WeakReference<>(type);
        }
    }
}
