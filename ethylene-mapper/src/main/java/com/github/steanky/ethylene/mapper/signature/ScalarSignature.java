package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.Prioritized;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Similar concept to {@link Signature}, but intended for scalar types.
 *
 * @param <TScalar> the scalar type
 */
public interface ScalarSignature<TScalar> extends Prioritized {
    /**
     * Creates a new, basic ScalarSignature implementation.
     *
     * @param priority       the priority of the signature
     * @param type           the type created/interpreted by the signature
     * @param elementType    the element type
     * @param scalarCreator  the function used to create the scalar objects
     * @param elementCreator the function used to create the scalar elements
     * @param <T>            the scalar type
     * @return the ScalarSignature
     */
    static <T> @NotNull ScalarSignature<T> of(int priority, @NotNull Token<T> type, @NotNull ElementType elementType,
        @NotNull Function<? super ConfigElement, ? extends T> scalarCreator,
        @NotNull Function<? super T, ? extends ConfigElement> elementCreator) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(elementType);
        Objects.requireNonNull(scalarCreator);
        Objects.requireNonNull(elementCreator);

        return new ScalarSignatureImpl<>(priority, type, elementType, scalarCreator, elementCreator);
    }

    /**
     * Equivalent to {@link ScalarSignature#of(int, Token, ElementType, Function, Function)}, but uses a default
     * priority of 0 and has a scalar element type.
     *
     * @param type           the type created/interpreted by the signature
     * @param scalarCreator  the function used to create the scalar objects
     * @param elementCreator the function used to create the scalar elements
     * @param <T>            the scalar type
     * @return the ScalarSignature
     */
    static <T> @NotNull ScalarSignature<T> of(@NotNull Token<T> type,
        @NotNull Function<? super ConfigElement, ? extends T> scalarCreator,
        @NotNull Function<? super T, ? extends ConfigElement> elementCreator) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(scalarCreator);
        Objects.requireNonNull(elementCreator);

        return new ScalarSignatureImpl<>(0, type, ElementType.SCALAR, scalarCreator, elementCreator);
    }

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

    /**
     * Basic {@link ScalarSignature} implementation. Not part of the public API.
     *
     * @param <T> the scalar type
     */
    final class ScalarSignatureImpl<T> extends PrioritizedBase implements ScalarSignature<T> {
        private final Token<T> type;
        private final ElementType elementType;
        private final Function<? super ConfigElement, ? extends T> scalarCreator;
        private final Function<? super T, ? extends ConfigElement> elementCreator;

        private ScalarSignatureImpl(int priority, @NotNull Token<T> type, @NotNull ElementType elementType,
            @NotNull Function<? super ConfigElement, ? extends T> scalarCreator,
            @NotNull Function<? super T, ? extends ConfigElement> elementCreator) {
            super(priority);
            this.type = type;
            this.elementType = elementType;
            this.scalarCreator = scalarCreator;
            this.elementCreator = elementCreator;
        }

        @Override
        public @NotNull Token<T> objectType() {
            return type;
        }

        @Override
        public @NotNull ElementType elementType() {
            return elementType;
        }

        @Override
        public @Nullable T createScalar(@NotNull ConfigElement element) {
            return scalarCreator.apply(element);
        }

        @Override
        public @NotNull ConfigElement createElement(@Nullable T t) {
            return elementCreator.apply(t);
        }
    }
}
