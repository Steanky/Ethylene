package com.github.steanky.ethylene.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Token may be used to retain complete, arbitrary type information at runtime. This includes generics, arrays, generic
 * arrays, bounded or wildcard generics, and classes. This type may be accessed by using the {@code get} method.
 * <p>
 * This class is abstract to force implementing it wherever it is used, so that information may be extracted from the
 * superclass type parameter. A standard usage pattern for Token is shown below:
 * <p></p>
 * {@code Token<List<String>> token = new Token<>() {};}
 * <p></p>
 * <p>
 * It is impossible to construct an instance of Token without a type parameter. Attempting to do so will result in an
 * {@link IllegalStateException}, as in the following code:
 * <p></p>
 * {@code Token token = new Token() {}; //throws an IllegalStateException!}
 *
 * @param <T> the type information to be held in this token
 */
//T is NOT unused, it is inspected reflectively on Token instantiation!
@SuppressWarnings("unused")
public abstract class Token<T> implements Supplier<Type> {
    public static final Token<Object> OBJECT = new Token<>() {};

    private final Type type;

    private Token(@NotNull Type type) {
        if (type instanceof TypeVariable<?> typeVariable) {
            type = typeVariable.getBounds()[0];
        }

        this.type = Objects.requireNonNull(type);
    }

    /**
     * Constructs a new Token representing a (often generic) type.
     *
     * @throws IllegalStateException if this class is constructed with no type parameters, or an unexpected condition
     *                               occurs
     */
    public Token() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterizedType)) {
            //happens if someone tries to construct a Token raw, without type parameters
            throw new IllegalStateException("This class may not be constructed without type parameters");
        }

        Type[] types = parameterizedType.getActualTypeArguments();
        if (types.length != 1) {
            throw new IllegalStateException(
                    "Expected 1 type parameter, found " + types.length + " for class " + getClass().getTypeName());
        }

        Type target = types[0];
        if (target == null) {
            //second sanity check, probably completely unnecessary, JVM might have blown up
            throw new IllegalStateException("Expected non-null type parameter for class " + getClass().getTypeName());
        }

        if (target instanceof TypeVariable<?> typeVariable) {
            target = typeVariable.getBounds()[0];
        }

        this.type = target;
    }

    public static @NotNull Token<?> of(@NotNull Type type) {
        return new Token<>(type) {};
    }

    public final @NotNull Type get() {
        return type;
    }

    @Override
    public final int hashCode() {
        return type.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof Token<?> other) {
            return type.equals(other.type);
        }

        return false;
    }

    @Override
    public final String toString() {
        return "Token{type=" + type + "}";
    }
}
