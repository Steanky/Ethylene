package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

/**
 * Finds matching {@link Signature} implementations and returns a {@link MatchingSignature} representing the match.
 */
public interface SignatureMatcher {
    /**
     * Finds a {@link MatchingSignature} given a return type and {@link ConfigElement}.
     *
     * @param desiredType     the desired type
     * @param providedElement the {@link ConfigElement}
     * @return a MatchingSignature
     */
    @NotNull MatchingSignature signatureForElement(@NotNull Token<?> desiredType,
        @NotNull ConfigElement providedElement);

    /**
     * Finds a {@link MatchingSignature} given a return type and {@link Object}.
     *
     * @param desiredType the desired type
     * @param object      some object
     * @return a MatchingSignature
     */
    @NotNull MatchingSignature signatureForObject(@NotNull Token<?> desiredType, @NotNull Object object);

    /**
     * A source of {@link SignatureMatcher} objects.
     */
    @FunctionalInterface
    interface Source {
        /**
         * Finds a {@link SignatureMatcher} for the given type.
         *
         * @param type the token for which to find a matcher
         * @return a SignatureMatcher instance, or null if the given type represents a scalar
         */
        SignatureMatcher matcherFor(@NotNull Token<?> type);
    }
}
