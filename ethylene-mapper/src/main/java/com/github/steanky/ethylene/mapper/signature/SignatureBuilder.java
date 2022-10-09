package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

/**
 * A builder of {@link Signature} objects for a specific type.
 */
@FunctionalInterface
public interface SignatureBuilder {
    /**
     * Builds signatures for a specific type.
     *
     * @param type the type for which to build signatures
     * @return an array of built signatures; may be empty if no signatures can be made for the given type
     */
    @NotNull Signature<?> @NotNull [] buildSignatures(@NotNull Token<?> type);

    /**
     * A selector of {@link SignatureBuilder} objects.
     */
    @FunctionalInterface
    interface Selector {
        /**
         * Selects a specific {@link SignatureBuilder}, given the type.
         *
         * @param type the type for which to retrieve a SignatureBuilder
         * @return the SignatureBuilder to use
         */
        @NotNull SignatureBuilder select(@NotNull Token<?> type);
    }
}
