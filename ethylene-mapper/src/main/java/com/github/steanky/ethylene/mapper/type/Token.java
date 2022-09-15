package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
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
    public static final Token<Object> OBJECT = new Token<>(Object.class) {};
    public static final Token<String> STRING = new Token<>(String.class) {};
    public static final Token<Long> LONG = new Token<>(Long.class) {};
    public static final Token<Double> DOUBLE = new Token<>(Double.class) {};
    public static final Token<Integer> INTEGER = new Token<>(Integer.class) {};
    public static final Token<Float> FLOAT = new Token<>(Float.class) {};
    public static final Token<Short> SHORT = new Token<>(Short.class) {};
    public static final Token<Character> CHARACTER = new Token<>(Character.class) {};
    public static final Token<Byte> BYTE = new Token<>(Byte.class) {};
    public static final Token<Boolean> BOOLEAN = new Token<>(Boolean.class) {};

    private final Reference<Type> typeReference;

    private static final Map<Class<?>, Type> referenceMap = Collections.synchronizedMap(new WeakHashMap<>());

    private Token(@NotNull Type type) {
        if (type instanceof TypeVariable<?> typeVariable) {
            this.typeReference = new WeakReference<>(typeVariable.getBounds()[0]);
        } else {
            this.typeReference = new WeakReference<>(Objects.requireNonNull(type));
        }
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
                    "Expected 1 type parameter, found " + types.length + " for '" + getClass().getTypeName() + "'");
        }

        Type target = types[0];
        if (target == null) {
            //second sanity check, probably completely unnecessary, JVM might have blown up
            throw new IllegalStateException("Expected non-null type parameter for '" + getClass().getTypeName() + "'");
        }

        if (target instanceof TypeVariable<?> typeVariable) {
            target = typeVariable.getBounds()[0];
        }

        this.typeReference = new WeakReference<>(target);
    }

    public static @NotNull Token<?> of(@NotNull Type type) {
        return new Token<>(Objects.requireNonNull(type)) {};
    }

    private static Type[] extractTypeArgumentsFrom(Map<TypeVariable<?>, Type> mappings, TypeVariable<?>[] variables) {
        Type[] result = new Type[variables.length];
        int index = 0;
        for (TypeVariable<?> var : variables) {
            if (!mappings.containsKey(var)) {
                throw new IllegalArgumentException("Missing type mapping for '" + var + "'");
            }

            result[index++] = mappings.get(var);
        }

        return result;
    }

    public static @NotNull Token<?> parameterize(@NotNull Class<?> raw, Type @NotNull ... params) {
        return Token.of(GenericInfoRepository.retain(raw, new InternalParameterizedType(raw, null, params)));
    }

    public static @NotNull Token<?> parameterize(@NotNull Class<?> raw, @NotNull Map<TypeVariable<?>, Type> typeMap) {
        Objects.requireNonNull(raw);
        Objects.requireNonNull(typeMap);

        Type[] types = extractTypeArgumentsFrom(typeMap, raw.getTypeParameters());
        return Token.of(GenericInfoRepository.retain(raw, new InternalParameterizedType(raw, null, types)));
    }

    public final @NotNull Token<?> genericArrayType() {
        Type type = get();
        Class<?> raw = TypeUtils.getRawType(type, null);
        return Token.of(GenericInfoRepository.retain(raw, new InternalGenericArrayType(type)));
    }

    public final @NotNull Type get() {
        Type type = typeReference.get();
        if (type == null) {
            throw new IllegalStateException("The type referred to by this token no longer exists");
        }

        return type;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(typeReference.get());
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
            return Objects.equals(typeReference.get(), other.typeReference.get());
        }

        return false;
    }

    @Override
    public final String toString() {
        return "Token{type=" + typeReference.get() + "}";
    }
}
