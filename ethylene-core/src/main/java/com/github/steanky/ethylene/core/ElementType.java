package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;

public enum ElementType {
    NODE,
    LIST,
    SCALAR;

    public boolean compatible(@NotNull ConfigElement element) {
        return this == element.type();
    }
}
