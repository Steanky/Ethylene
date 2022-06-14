package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * Token may be used to retain complete, arbitrary type information at runtime. This includes generics, arrays, generic
 * arrays, bounded or wildcard generics, and classes. This type may be accessed by using the {@code get} method.
 * <p>
 * This class is abstract to force implementing it wherever it is used, so that information may be extracted from the
 * superclass type parameter.
 * <p>
 * It is impossible to construct an instance of Token without a type parameter. Attempting to do so will result in an
 * {@link IllegalStateException}.
 * @param <T> the type information to be held in this token
 */
//T is NOT unused, it is inspected reflectively on Token instantiation!
@SuppressWarnings("unused")
public abstract class Token<T> implements Supplier<Type> {
    private final Type type;

    /**
     * Constructs a new Token representing a (often generic) type.
     * @throws IllegalStateException if this class is constructed with no type parameters, or an unexpected condition
     * occurs
     */
    public Token() {
        Type superclass = getClass().getGenericSuperclass();
        if(!(superclass instanceof ParameterizedType parameterizedType)) {
            //happens if someone tries to construct a Token raw, without type parameters
            throw new IllegalStateException("This class may not be constructed without type parameters");
        }

        Type[] types = parameterizedType.getActualTypeArguments();
        if(types.length != 1) {
            //sanity check, should absolutely never happen, if it does there's probably something wrong with the JVM
            throw new IllegalStateException("Expected 1 type parameter, found " + types.length + " for class " +
                    getClass().getTypeName());
        }

        Type target = types[0];
        if(target == null) {
            //second sanity check, probably completely unnecessary, JVM might have blown up
            throw new IllegalStateException("Expected non-null type parameter for class " + getClass().getTypeName());
        }

        this.type = target;
    }

    public final @NotNull Type get() {
        return type;
    }
}
