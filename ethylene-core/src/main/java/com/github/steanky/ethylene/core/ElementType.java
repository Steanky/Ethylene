package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;

public enum ElementType {
    NODE, LIST, SCALAR;

    public boolean compatible(@NotNull ConfigElement element) {
        return this == element.type();
    }

    public boolean isScalar() {
        return this == SCALAR;
    }

    public boolean isList() {
        return this == LIST;
    }

    public boolean isNode() {
        return this == NODE;
    }

    public boolean isContainer() {
        return this != SCALAR;
    }
}
