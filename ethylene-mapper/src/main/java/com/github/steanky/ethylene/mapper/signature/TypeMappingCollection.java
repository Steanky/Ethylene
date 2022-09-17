package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Class used internally to more easily translate a {@link Collection} of entries of strings and tokens into a
 * collection of entries of strings and {@link Type} objects. No strong references to type objects are retained.
 * <p>
 * This class is public to enable access across different packages, but is not considered part of the public API.
 */
@ApiStatus.Internal
public class TypeMappingCollection extends AbstractCollection<Entry<String, Type>> {
    private final Collection<Entry<String, Token<?>>> tokenCollection;

    /**
     * Creates a new instance of this collection from the given collection of token entries. Changes to the underlying
     * collection will be reflected in this instance.
     * @param tokenCollection the underlying collection
     */
    public TypeMappingCollection(@NotNull Collection<Entry<String, Token<?>>> tokenCollection) {
        this.tokenCollection = Objects.requireNonNull(tokenCollection);
    }

    @Override
    public Iterator<Entry<String, Type>> iterator() {
        return new Iterator<>() {
            private final Iterator<Entry<String, Token<?>>> iterator = tokenCollection.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Entry<String, Type> next() {
                Entry<String, Token<?>> entry = iterator.next();
                return Entry.of(entry.getFirst(), entry.getSecond().get());
            }
        };
    }

    @Override
    public int size() {
        return tokenCollection.size();
    }
}
