package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class WeakTypeKey extends WeakTypeBase implements WeakType {
    private final byte[] identifier;
    private final ClassLoader classLoader;
    
    WeakTypeKey(byte @NotNull [] identifier, @Nullable ClassLoader classLoader) {
        this.identifier = identifier;
        this.classLoader = classLoader;
    }
    
    @Override
    public @Nullable ClassLoader getBoundClassloader() {
        return classLoader;
    }

    @Override
    public byte[] identifier() {
        return identifier;
    }
}
