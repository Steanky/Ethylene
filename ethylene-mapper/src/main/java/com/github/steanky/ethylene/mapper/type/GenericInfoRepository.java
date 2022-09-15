package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

class GenericInfoRepository {
    private static final Map<Class<?>, GenericInfoRepository> store = new WeakHashMap<>();
    private static final Object sync = new Object();

    private final Map<Type, Type> canonicalTypes = new HashMap<>(4);

    private GenericInfoRepository() {}

    static @NotNull Type retain(@NotNull Class<?> owner, @NotNull Type type) {
        synchronized (sync) {
            return store.computeIfAbsent(owner, ignored -> new GenericInfoRepository()).canonicalTypes
                    .computeIfAbsent(type, key -> key);
        }
    }
}
