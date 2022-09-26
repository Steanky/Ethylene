package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

abstract non-sealed class WeakTypeBase implements WeakType {
    private final byte[] identifier;

    private boolean hashed;
    private int hash;

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
    public final byte[] identifier() {
        return identifier;
    }
}
