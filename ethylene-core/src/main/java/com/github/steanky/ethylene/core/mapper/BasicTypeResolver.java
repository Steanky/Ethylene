package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Supplier;

public class BasicTypeResolver implements TypeResolver {
    private final TypeHinter typeHinter;
    private final Map<String, ClassEntry> types;
    private final Map<Class<?>, ClassEntry> cache;

    public BasicTypeResolver(@NotNull TypeHinter typeHinter, int initialCapacity) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        types = new HashMap<>(initialCapacity);
        cache = new WeakHashMap<>(initialCapacity);
    }

    public BasicTypeResolver(@NotNull TypeHinter typeHinter) {
        this(typeHinter, 4);
    }

    private static Class<?> forName(String className, Supplier<String> nameSupplier) {
        try {
            return ClassUtils.getClass(className);
        } catch (ClassNotFoundException e) {
            throw new MapperException("got invalid concrete type when resolving '" + nameSupplier.get() + "'", e);
        }
    }

    @Override
    public @NotNull Class<?> resolveType(@NotNull Type type, @NotNull ConfigElement element) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(element);

        Class<?> raw = TypeUtils.getRawType(type, null);
        if (raw == null) {
            throw new MapperException("resolved raw type was null for '" + type.getTypeName() + "'");
        }

        ElementType hint = typeHinter.getHint(raw);
        if (raw.isArray() || raw.isPrimitive()) {
            if (!hint.compatible(element)) {
                throw new MapperException("incompatible types: " + hint + " to " + type);
            }

            return raw;
        }

        if ((!Modifier.isAbstract(raw.getModifiers()) && !types.containsKey(ClassUtils.getName(raw)))) {
            if (hint.compatible(element)) {
                return raw;
            }
            else {
                //check assignability
                Class<?> elementType = switch (element.type()) {
                    case NODE -> raw;
                    case LIST -> ArrayList.class;
                    case SCALAR -> {
                        Object scalar = element.asScalar();
                        if (scalar == null) {
                            yield Object.class;
                        }

                        yield scalar.getClass();
                    }
                };

                if (!raw.isAssignableFrom(elementType)) {
                    throw new MapperException("element type " + element.type() + " not compatible with " + type);
                }

                return elementType;
            }
        }

        ClassEntry cached = cache.get(raw);
        if (cached != null) {
            Class<?> target = cached.reference.get();
            if (target != null) {
                return target;
            }

            cache.remove(raw);
        }

        for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
            ClassEntry entry = types.get(ClassUtils.getName(superclass));
            if (entry != null) {
                Class<?> ref = entry.reference.get();
                if (ref == null) {
                    ref = forName(entry.name, type::getTypeName);
                    entry.reference = new WeakReference<>(ref);
                }

                cache.put(raw, entry);
                return ref;
            }
        }

        throw new MapperException("no concrete type found for '" + type.getTypeName() + "'");
    }

    public void registerTypeImplementation(@NotNull Class<?> superclass, @NotNull Class<?> implementation) {
        if (!TypeUtils.isAssignable(implementation, superclass)) {
            throw new MapperException(
                    "implementation class '" + implementation.getName() + "' is not assignable to " + "superclass '" +
                            superclass.getName() + "'");
        }

        if (Modifier.isAbstract(implementation.getModifiers())) {
            throw new MapperException("implementation must not be abstract");
        }

        ClassEntry newEntry = new ClassEntry(ClassUtils.getName(implementation), implementation);
        if (types.putIfAbsent(ClassUtils.getName(superclass), newEntry) != null) {
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
