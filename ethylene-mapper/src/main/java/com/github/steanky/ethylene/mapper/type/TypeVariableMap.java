package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

public class TypeVariableMap extends AbstractMap<TypeVariable<?>, Token<?>> {
    private final Map<TypeVariable<?>, Token<?>> tokenMap;

    private Set<Entry<TypeVariable<?>, Token<?>>> entrySet;

    TypeVariableMap(Map<TypeVariable<?>, Type> underlying) {
        if (underlying.isEmpty()) {
            tokenMap = Map.of();
        }
        else {
            tokenMap = new HashMap<>(underlying.size());
            for (Map.Entry<TypeVariable<?>, Type> entry : underlying.entrySet()) {
                tokenMap.put(entry.getKey(), Token.ofType(entry.getValue()));
            }
        }
    }

    @NotNull
    @Override
    public Set<Entry<TypeVariable<?>, Token<?>>> entrySet() {
        if (tokenMap.isEmpty()) {
            return Set.of();
        }

        return Objects.requireNonNullElseGet(entrySet, () -> entrySet = Collections.unmodifiableSet(tokenMap
                .entrySet()));
    }

    @Override
    public int size() {
        return tokenMap.size();
    }

    @Override
    public boolean isEmpty() {
        return tokenMap.isEmpty();
    }

    @Override
    public Token<?> get(Object key) {
        return tokenMap.get(key);
    }

    public @NotNull Map<TypeVariable<?>, Type> resolve() {
        Map<TypeVariable<?>, Type> entries = new HashMap<>(tokenMap.size());
        for (Entry<TypeVariable<?>, Token<?>> entry : tokenMap.entrySet()) {
            entries.put(entry.getKey(), entry.getValue().get());
        }

        return entries;
    }
}
