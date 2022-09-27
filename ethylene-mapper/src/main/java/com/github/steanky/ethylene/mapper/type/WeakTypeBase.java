package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Base abstract class for {@link WeakType}. Includes implementations of {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} suitable for any WeakType. Also contains the identifier byte
 * array.
 */
abstract non-sealed class WeakTypeBase implements WeakType {
    private final byte[] identifier;

    private boolean hashed;
    private int hash;

    /**
     * Creates a new instance of this class from the provided identifier array.
     *
     * @param identifier the identifier array
     */
    WeakTypeBase(byte @NotNull [] identifier) {
        this.identifier = identifier;
    }

    @Override
    public final int hashCode() {
        if (!hashed) {
            //cache the hashcode to improve map lookup times
            hash = Arrays.hashCode(identifier());
            hashed = true;
        }

        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof WeakType other) {
            return Arrays.equals(identifier(), other.identifier());
        }

        return false;
    }

    @Override
    public final String toString() {
        return TypeUtils.toString(this);
    }

    @Override
    public final byte @NotNull [] identifier() {
        return identifier;
    }
}
