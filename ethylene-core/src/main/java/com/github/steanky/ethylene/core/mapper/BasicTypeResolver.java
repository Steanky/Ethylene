package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Supplier;

public class BasicTypeResolver implements TypeResolver {
    private final TypeHinter typeHinter;
    private final Map<Class<?>, ClassEntry> types;
    private final Map<Class<?>, ClassEntry> cache;

    public BasicTypeResolver(@NotNull TypeHinter typeHinter) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        types = new WeakHashMap<>();
        cache = new WeakHashMap<>();
    }

    private static Class<?> forName(String className, Supplier<String> nameSupplier) {
        try {
            return ClassUtils.getClass(className);
        } catch (ClassNotFoundException e) {
            throw new MapperException("got invalid concrete type when resolving '" + nameSupplier.get() + "'", e);
        }
    }

    @Override
    public @NotNull Type resolveType(@NotNull Type type, @Nullable ConfigElement element) {
        Objects.requireNonNull(type);

        Class<?> raw = TypeUtils.getRawType(type, null);
        if (raw == null) {
            throw new MapperException("resolved raw type was null for '" + type.getTypeName() + "'");
        }

        ElementType hint = typeHinter.getHint(raw);
        if (raw.isArray() || raw.isPrimitive()) {
            if (element != null && !hint.compatible(element)) {
                throw new MapperException("incompatible types: " + hint + " to " + type);
            }

            return type;
        }

        ClassEntry cached = cache.get(raw);
        if (cached != null) {
            Class<?> ref = cached.reference.get();
            if (ref != null) {
                if (type instanceof ParameterizedType parameterizedType) {
                    return TypeUtils.parameterize(ref, TypeUtils.determineTypeArguments(ref, parameterizedType));
                }

                return ref;
            }

            cache.remove(raw);
        }

        for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
            ClassEntry entry = types.get(superclass);
            if (entry != null) {
                Class<?> ref = entry.reference.get();
                if (ref == null) {
                    ref = forName(entry.name, type::getTypeName);
                    entry.reference = new WeakReference<>(ref);
                }

                cache.put(raw, entry);

                if (type instanceof ParameterizedType parameterizedType) {
                    return TypeUtils.parameterize(ref, TypeUtils.determineTypeArguments(ref, parameterizedType));
                }

                return ref;
            }
        }

        if (!Modifier.isAbstract(raw.getModifiers()) && !types.containsKey(raw)) {
            if (element == null || hint.compatible(element)) {
                return type;
            }

            //check assignability
            Type elementType = switch (element.type()) {
                case NODE -> raw;
                case LIST -> {
                    Type[] params = ReflectionUtils.extractGenericTypeParameters(type, raw);
                    yield TypeUtils.parameterize(ArrayList.class, params.length == 0 ? Object.class : params[0]);
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
                throw new MapperException("element type '" + element.type() + "' not compatible with '" + type + "'");
            }

            return elementType;
        }

        //no type resolution found (but maybe we can still find a suitable signature later)
        return type;
    }

    public void registerTypeImplementation(@NotNull Class<?> superclass, @NotNull Class<?> implementation) {
        if (!TypeUtils.isAssignable(implementation, superclass)) {
            throw new MapperException(
                    "implementation class '" + implementation.getName() + "' is not assignable to " + "superclass '" +
                            superclass.getName() + "'");
        }

        ClassEntry newEntry = new ClassEntry(ClassUtils.getName(implementation), implementation);
        if (types.putIfAbsent(superclass, newEntry) != null) {
            throw new MapperException("there is already an implementation type registered for class '" + superclass
                    .getName() + "'");
        }

        cache.put(superclass, newEntry);
    }

    private static class ClassEntry {
        private final String name;
        private Reference<Class<?>> reference;

        private ClassEntry(String name, Class<?> type) {
            this.name = name;
            reference = new WeakReference<>(type);
        }
    }
}
