package com.github.steanky.ethylene.mapper.type;

import java.util.Arrays;

abstract non-sealed class WeakTypeBase implements WeakType {
    private boolean hashed;
    private int hash;

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
}
