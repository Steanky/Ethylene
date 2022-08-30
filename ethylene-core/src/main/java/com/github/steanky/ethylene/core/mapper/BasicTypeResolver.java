package com.github.steanky.ethylene.core.mapper;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class BasicTypeResolver implements TypeResolver {
    private static class ClassEntry {
        private final String name;
        private Reference<Class<?>> reference;

        private ClassEntry(String name, Class<?> type) {
            this.name = name;
            reference = new WeakReference<>(type);
        }
    }

    private final Map<String, ClassEntry> types;
    private final Map<Class<?>, ClassEntry> cache;

    public BasicTypeResolver(int initialCapacity) {
        types = new HashMap<>(initialCapacity);
        cache = new WeakHashMap<>(initialCapacity);
    }

    public BasicTypeResolver() {
        this(4);
    }

    @Override
    public @NotNull Class<?> resolveType(@NotNull Type type) {
        Class<?> raw = TypeUtils.getRawType(type, null);
        ClassEntry cached = cache.get(raw);
        if (cached != null) {
            Class<?> target = cached.reference.get();
            if (target != null) {
                return target;
            }

            cache.remove(raw);
        }

        for(Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
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
            throw new MapperException("implementation class '" + implementation.getName() + "' is not assignable to " +
                    "superclass '" + superclass.getName() + "'");
        }

        if (Modifier.isAbstract(implementation.getModifiers())) {
            throw new MapperException("implementation must not be abstract");
        }

        ClassEntry newEntry = new ClassEntry(ClassUtils.getName(implementation), implementation);
        if (types.putIfAbsent(ClassUtils.getName(superclass), newEntry) != null) {
            throw new MapperException("there is already an implementation type registered for class '" +
                    superclass.getName() + "'");
        }

        cache.put(superclass, newEntry);
    }

    private static Class<?> forName(String className, Supplier<String> nameSupplier) {
        try {
            return ClassUtils.getClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new MapperException("got invalid concrete type when resolving '" + nameSupplier.get() + "'", e);
        }
    }
}
