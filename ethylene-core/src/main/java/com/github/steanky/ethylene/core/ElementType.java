package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;

public enum ElementType {
    NODE,
    LIST,
    SCALAR;

    public boolean compatible(@NotNull ConfigElement element) {
        return this == element.type();
    }
}
