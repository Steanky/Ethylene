package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

final class WeakTypeKey extends WeakTypeBase implements WeakType {
    private final byte[] identifier;
    
    WeakTypeKey(byte @NotNull [] identifier) {
        this.identifier = identifier;
    }
    
    @Override
    public @NotNull Class<?> getBoundClass() {
        //have to return something, this is unimportant
        return Object.class;
    }

    @Override
    public byte[] identifier() {
        return identifier;
    }
}
