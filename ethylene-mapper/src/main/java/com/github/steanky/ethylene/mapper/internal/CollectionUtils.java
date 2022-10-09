package com.github.steanky.ethylene.mapper.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Miscellaneous utilities related to the Java Collections Framework. Not part of the public API.
 */
public final class CollectionUtils {
    /**
     * Adds all elements of the provided {@link Iterable} to the collection. Will use
     * {@link Collection#addAll(Collection)} if possible, otherwise the iterable will be iterated and elements added to
     * the collection one by one.
     *
     * @param objects the iterable used as a source of elements
     * @param target the target collection to add elements to
     * @param <T> the upper bound of the element source type, and the lower bound of the collection type
     */
    @SuppressWarnings("unchecked")
    public static <T> void addAll(@NotNull Iterable<? extends T> objects, @NotNull Collection<? super T> target) {
        Objects.requireNonNull(objects);
        Objects.requireNonNull(target);

        if (objects instanceof Collection<?>) {
            //this cast is safe
            target.addAll((Collection<? extends T>)objects);
        }
        else {
            for (T element : objects) {
                target.add(element);
            }
        }
    }

}
