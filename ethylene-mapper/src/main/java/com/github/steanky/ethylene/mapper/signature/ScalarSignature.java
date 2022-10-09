package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.Prioritized;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ScalarSignature<TScalar> extends Prioritized {
    @NotNull Token<TScalar> objectType();

    @NotNull ElementType elementType();

    @Nullable TScalar createScalar(@NotNull ConfigElement element);

    @NotNull ConfigElement createElement(@Nullable TScalar scalar);
}
