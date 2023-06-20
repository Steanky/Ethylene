package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object describing a parameter which may be used to instantiate a signature.
 */
public interface SignatureParameter {
    /**
     * Gets the type of this parameter. This is an upper-bound; it is legal to pass assignable subclasses of this type
     * to a signature's constructor.
     * @return the type of this parameter
     */
    @NotNull Token<?> type();

    /**
     * Gets the default {@link ConfigElement}. This is only used when necessary; i.e. when the provided config does not
     * contain an existing value for this parameter.
     * @return the default configuration; {@code null} if there is no default
     */
    @Nullable ConfigElement defaultOption();

    /**
     * Creates a new implementation of {@link SignatureParameter} that has the specified type and does not supply a
     * default value.
     * @param type the type
     * @return a new SignatureParameter
     */
    static @NotNull SignatureParameter parameter(@NotNull Token<?> type) {
        return new SignatureParameter() {
            @Override
            public @NotNull Token<?> type() {
                return type;
            }

            @Override
            public @Nullable ConfigElement defaultOption() {
                return null;
            }
        };
    }

    /**
     * Creates a new implementation of {@link SignatureParameter} that has the specified type and supplies the given
     * default value.
     * @param type the type
     * @param element the default value
     * @return a new SignatureParameter
     */
    static @NotNull SignatureParameter parameter(@NotNull Token<?> type, @Nullable ConfigElement element) {
        return new SignatureParameter() {
            @Override
            public @NotNull Token<?> type() {
                return type;
            }

            @Override
            public @Nullable ConfigElement defaultOption() {
                return element;
            }
        };
    }
}
