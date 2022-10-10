package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.Prioritized;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Similar concept to {@link Signature}, but intended for scalar types.
 *
 * @param <TScalar> the scalar type
 */
public interface ScalarSignature<TScalar> extends Prioritized {
    /**
     * Gets a token representing the scalar type.
     *
     * @return a token representing the scalar type
     */
    @NotNull Token<TScalar> objectType();

    /**
     * Gets the {@link ElementType} used to create objects of this type. Generally, this is {@link ElementType#SCALAR},
     * but not always.
     *
     * @return the ElementType used to create this scalar
     */
    @NotNull ElementType elementType();

    /**
     * Creates a scalar object from some configuration data.
     *
     * @param element the configuration data
     * @return a scalar object from the data
     */
    @Nullable TScalar createScalar(@NotNull ConfigElement element);

    /**
     * Creates a {@link ConfigElement} from some scalar data.
     *
     * @param scalar the scalar data
     * @return a ConfigElement from the data
     */
    @NotNull ConfigElement createElement(@Nullable TScalar scalar);
}
