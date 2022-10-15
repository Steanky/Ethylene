package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents a matching {@link Signature} object.
 *
 * @param signature the signature that matched
 * @param elements  the elements that will be used to create a new object; will be null if creating configuration data
 * @param objects   the objects that will be used to create some new configuration data, will be null if creating an
 *                  object instead
 * @param size      the size of the signature
 */
public record MatchingSignature(@NotNull Signature<?> signature, Collection<ConfigElement> elements,
                                Collection<Signature.TypedObject> objects, int size) {
    /**
     * Creates a new instance of this record.
     *
     * @param signature the signature that matched
     * @param elements  the elements that will be used to create a new object; will be null if creating configuration
     *                  data
     * @param objects   the objects that will be used to create some new configuration data, will be null if creating an
     *                  object instead
     * @param size      the size of the signature
     */
    public MatchingSignature(@NotNull Signature<?> signature, Collection<ConfigElement> elements,
        Collection<Signature.TypedObject> objects, int size) {
        this.signature = Objects.requireNonNull(signature);
        this.elements = elements;
        this.objects = objects;
        this.size = size;
    }
}