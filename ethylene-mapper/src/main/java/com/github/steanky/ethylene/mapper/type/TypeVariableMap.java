package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * An unmodifiable map of {@link TypeVariable} to {@link Token}. Instances can be obtained through calling various
 * methods on {@link Token}. They are primarily meant to be used with methods like
 * {@link Token#parameterize(TypeVariableMap)}.
 * <p>
 * This class has a package-private constructor. This is because it would be possible to supply arbitrary instances of
 * {@link Type}, which could lead to issues as they are stored in tokens using {@link Token#ofType(Type)}, and are thus
 * subject to the constraints discussed further in the documentation for that method.
 * <p>
 * This map may contain value references to {@link Token} objects whose types have been garbage-collected. It may also
 * contain strong references to {@link TypeVariable} objects, which can in some circumstances contain an indirect
 * reference to the classloader. Therefore, instances of this class are not suitable for long-term storage.
 */
public final class TypeVariableMap extends AbstractMap<TypeVariable<?>, Token<?>> {
    private final Map<TypeVariable<?>, Token<?>> tokenMap;

    private Set<Entry<TypeVariable<?>, Token<?>>> entrySet;

    /**
     * Creates a new instance of this class based on the underlying, trusted map, which should never be modified after
     * creation. Its type implementations must either be JDK-cached instances, subclasses of {@link WeakType}, or else
     * subject to some level of caching to prevent instances from being garbage collected too soon.
     *
     * @param underlying the underlying map
     */
    TypeVariableMap(@NotNull Map<TypeVariable<?>, Type> underlying) {
        if (underlying.isEmpty()) {
            tokenMap = Map.of();
        } else {
            @SuppressWarnings("unchecked")
            Map.Entry<TypeVariable<?>, Token<?>>[] tokenArray = new Entry[underlying.size()];

            Iterator<Map.Entry<TypeVariable<?>, Type>> entryIterator = underlying.entrySet().iterator();
            for (int i = 0; i < tokenArray.length && entryIterator.hasNext(); i++) {
                Map.Entry<TypeVariable<?>, Type> entry = entryIterator.next();
                tokenArray[i] = Map.entry(entry.getKey(), Token.ofType(entry.getValue()));
            }

            tokenMap = Map.ofEntries(tokenArray);
        }
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

    @NotNull
    @Override
    public Set<Entry<TypeVariable<?>, Token<?>>> entrySet() {
        if (tokenMap.isEmpty()) {
            return Set.of();
        }

        return Objects.requireNonNullElseGet(entrySet,
            () -> entrySet = Collections.unmodifiableSet(tokenMap.entrySet()));
    }

    /**
     * Returns a {@link Map} of {@link TypeVariable} to {@link Type}. May throw a {@link TypeNotPresentException} if
     * {@link Token#get()} fails to retrieve a type.
     *
     * @return an unmodifiable map of TypeVariables to Types
     */
    public @NotNull @Unmodifiable Map<TypeVariable<?>, Type> resolve() {
        if (tokenMap.isEmpty()) {
            return Map.of();
        }

        Map<TypeVariable<?>, Type> entries = new HashMap<>(tokenMap.size());
        for (Entry<TypeVariable<?>, Token<?>> entry : tokenMap.entrySet()) {
            entries.put(entry.getKey(), entry.getValue().get());
        }

        return Collections.unmodifiableMap(entries);
    }
}
