package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Token may be used to retain complete, arbitrary type information at runtime. This includes generics, arrays, generic
 * arrays, bounded or wildcard generics, and classes. This type may be accessed by using the {@code get} method.
 * <p>
 * This class is abstract to force implementing it wherever it is used, so that information may be extracted from the
 * superclass type parameter. A standard usage pattern for Token is shown below:
 * <p>
 * {@code Token<List<String>> token = new Token<>() {};}
 * <p>
 * It is impossible to construct an instance of Token without a type parameter. Attempting to do so will result in an
 * {@link IllegalStateException}, as in the following code:
 * <p>
 * {@code Token token = new Token() {}; //throws an IllegalStateException!}
 * <p>
 * All instance methods defined in this class, unless otherwise noted, will throw a {@link TypeNotPresentException} if
 * their underlying type object no longer exists.
 *
 * @param <T> the type information to be held in this token
 */
//T is NOT unused, it is inspected reflectively on Token instantiation!
@SuppressWarnings("unused")
public abstract class Token<T> implements Supplier<Type> {
    /**
     * Common shared token whose underlying type is {@link Object}.
     */
    public static final Token<Object> OBJECT = new Token<>(Object.class) {
    };

    /**
     * Common shared token whose underlying type is {@link String}.
     */
    public static final Token<String> STRING = new Token<>(String.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Long}.
     */
    public static final Token<Long> LONG = new Token<>(Long.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Double}.
     */
    public static final Token<Double> DOUBLE = new Token<>(Double.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Integer}.
     */
    public static final Token<Integer> INTEGER = new Token<>(Integer.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Float}.
     */
    public static final Token<Float> FLOAT = new Token<>(Float.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Short}.
     */
    public static final Token<Short> SHORT = new Token<>(Short.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Character}.
     */
    public static final Token<Character> CHARACTER = new Token<>(Character.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Byte}.
     */
    public static final Token<Byte> BYTE = new Token<>(Byte.class) {
    };

    /**
     * Common shared token whose underlying type is {@link Boolean}.
     */
    public static final Token<Boolean> BOOLEAN = new Token<>(Boolean.class) {
    };

    private final Reference<Type> typeReference;
    private final String typeName;

    private Token(@NotNull Type type) {
        Objects.requireNonNull(type);

        Type resolved = resolveType(type);
        this.typeReference = new WeakReference<>(resolved);
        this.typeName = resolved.getTypeName();
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

        Type resolved = resolveType(target);
        this.typeReference = new WeakReference<>(resolved);
        this.typeName = resolved.getTypeName();
    }

    private static Type resolveType(Type input) {
        if (input instanceof TypeVariable<?> variable) {
            return variable.getBounds()[0];
        } else if (input instanceof WildcardType wildcardType) {
            //interpret wildcards only by their upper bound
            return wildcardType.getUpperBounds()[0];
        }

        return input;
    }

    /**
     * Creates a token from the given {@link Class}. Unlike {@link Token#ofType(Type)}, this method is safe for general
     * use.
     *
     * @param type     the class from which to create a token
     * @param <TClass> the object type defined by the class
     * @return a new token containing the specified class
     */
    public static @NotNull <TClass> Token<TClass> ofClass(@NotNull Class<TClass> type) {
        Objects.requireNonNull(type);
        return new Token<>(type) {
        };
    }

    private static Token<?> parameterizeInternal(Class<?> raw, Type owner, Type... params) {
        //currently, only checks the parameter array length against the number of variables, ignoring type bounds
        checkTypes(raw, params);

        Class<?> enclosing = raw.getEnclosingClass();
        Type actualOwner;
        if (enclosing == null) {
            if (owner != null) {
                throw new IllegalArgumentException("No owner allowed for '" + raw + "'");
            }
            actualOwner = null;
        } else if (owner == null) {
            actualOwner = enclosing;
        } else {
            if (!TypeUtils.isAssignable(owner, enclosing)) {
                throw new IllegalArgumentException("Invalid enclosing type for '" + raw + "'");
            }
            actualOwner = owner;
        }

        return Token.ofType(GenericInfoRepository.bind(raw, new InternalParameterizedType(raw, actualOwner, params)));
    }

    private static Type[] extractTypes(Token<?>[] tokens) {
        if (tokens.length == 0) {
            return ReflectionUtils.EMPTY_TYPE_ARRAY;
        }

        Type[] types = new Type[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            types[i] = Objects.requireNonNull(tokens[i], "token").get();
        }

        return types;
    }

    private static @NotNull Class<?> rawType(Type type) {
        Class<?> cls = TypeUtils.getRawType(type, null);
        if (cls == null) {
            throw new IllegalArgumentException("Invalid type '" + type.getTypeName() + "'");
        }

        return cls;
    }

    private static void checkTypes(Class<?> raw, Type... params) {
        int requiredLength = raw.getTypeParameters().length;
        int actualLength = params.length;

        //validate parameter length
        if (requiredLength != actualLength) {
            throw new IllegalArgumentException(
                "Actual and required number of type parameters differ in length for '" + raw.getTypeName() + "', was " +
                    actualLength + ", needed " + requiredLength);
        }
    }

    /**
     * Creates a new Token containing the provided type. Only a weak reference to the type is retained. Since Java's own
     * Type implementations have strong references elsewhere due to internal caching, the type will (assuming no other
     * strong references exist) be garbage collected when the classloader it was created by is unloaded. However, custom
     * subclasses of Type are not necessarily bound to the classloader. Unless additional precautions are taken, these
     * objects are not safe for storage in Tokens because they will be garbage collected too soon.<p>
     * <p>
     * This method is public to allow access across packages, but is not intended for general use. It is not part of the
     * public API. Users should create tokens of specific types through subclassing,
     * {@link Token#parameterize(Class[])}, {@link Token#ofClass(Class)}, or a similar supported method.
     *
     * @param type the type from which to create a token
     * @return a new token containing the given type
     */
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    @ApiStatus.Internal
    public static @NotNull Token<?> ofType(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof WeakType weakType) {
            //return the bound type if we can
            return new Token<>(GenericInfoRepository.bind(rawType(type), weakType)) {
            };
        }

        return new Token<>(type) {
        };
    }

    private static Type[] extractTypeArgumentsFrom(Map<TypeVariable<?>, Type> mappings, TypeVariable<?>[] variables) {
        Type[] result = new Type[variables.length];
        int i = 0;
        for (TypeVariable<?> var : variables) {
            if (!mappings.containsKey(var)) {
                throw new IllegalArgumentException("Missing type mapping for '" + var + "'");
            }

            result[i++] = Objects.requireNonNull(mappings.get(var), "mapping value");
        }

        return result;
    }

    /**
     * Creates a new token referencing the result of parameterizing this token's type with the provided parameters. If
     * this token already represents a parameterized type, its raw class is used.
     *
     * @param params the parameters to use
     * @return a new token containing the result of parameterizing this type
     * @throws IllegalArgumentException if the given number of types differs from the amount required by this type
     */
    public final @NotNull Token<?> parameterize(@NotNull Token<?> @NotNull ... params) {
        Objects.requireNonNull(params);
        return parameterizeInternal(rawType(), null, extractTypes(params));
    }

    /**
     * Computes the raw type of this token. For parameterized types, this is the type-erased class. For generic arrays,
     * this is an array of the raw type of the component class.
     *
     * @return the raw type for this token
     */
    public final @NotNull Class<?> rawType() {
        return rawType(get());
    }

    /**
     * Gets the underlying Type object. The returned value is a strong reference; care should be taken to avoid storing
     * it statically unless additional precautions are taken.
     *
     * @return this token's type
     * @throws IllegalStateException if the type once referred to by this token no longer exists
     */
    public final @NotNull Type get() {
        return ReflectionUtils.resolve(typeReference, typeName);
    }

    /**
     * Convenience overload for {@link Token#parameterize(Token[])}. Uses {@link Class} objects directly instead of
     * tokens.
     *
     * @param params the parameters to use
     * @return a new token containing the result of parameterizing this type
     */
    public final @NotNull Token<?> parameterize(@NotNull Class<?> @NotNull ... params) {
        Objects.requireNonNull(params);

        Class<?> raw = rawType();
        return parameterizeInternal(raw, null, params);
    }

    /**
     * Parameterizes this token by using types obtained from the given {@link TypeVariableMap}. If some necessary types
     * cannot be found, an {@link IllegalArgumentException} will be thrown.
     *
     * @param typeVariables the map of type variables from which to extract type parameters
     * @return a new token containing the result of parameterizing this type
     * @throws IllegalArgumentException if some necessary types cannot be found
     */
    public final @NotNull Token<?> parameterize(@NotNull TypeVariableMap typeVariables) {
        Objects.requireNonNull(typeVariables);

        Class<?> raw = rawType();
        Map<TypeVariable<?>, Type> mappings = typeVariables.resolve();
        TypeVariable<?>[] parameters = raw.getTypeParameters();
        return parameterizeInternal(raw, null, extractTypeArgumentsFrom(mappings, parameters));
    }

    /**
     * Creates a new token referencing the result of parameterizing this token's type with the provided parameters and
     * owner. If this token already represents a parameterized type, its raw class is used. The owner type must be
     * assignable to the raw type of the enclosing class.
     *
     * @param owner the owner class
     * @param params the parameters to use
     * @return a new token containing the result of parameterizing this type
     * @throws IllegalArgumentException if the given number of types differs from the amount required by this type, or
     * if the given owner type is not assignable to the raw type of the enclosing class
     */
    public final @NotNull Token<?> parameterizeWithOwner(@NotNull Token<?> owner,
        @NotNull Token<?> @NotNull ... params) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(params);

        return parameterizeInternal(rawType(), owner.get(), extractTypes(params));
    }

    /**
     * Convenience overload for {@link Token#parameterizeWithOwner(Token, Token[])}.
     * @param owner the owner class
     * @param params the parameters to use
     * @return a new token containing the result of parameterizing this type
     */
    public final @NotNull Token<?> parameterizeWithOwner(@NotNull Class<?> owner,
        @NotNull Class<?> @NotNull ... params) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(params);

        return parameterizeInternal(rawType(), owner, params);
    }

    /**
     * Convenience overload for {@link Token#parameterizeWithOwner(Token, Token[])}. Extracts required type variables
     * from the given {@link TypeVariableMap}.
     *
     * @param owner the owner class
     * @param typeVariables the parameters to use
     * @return a new token containing the result of parameterizing this type
     * @throws IllegalArgumentException if the given TypeVariableMap does not contain some necessary types
     */
    public final @NotNull Token<?> parameterizeWithOwner(@NotNull Token<?> owner,
        @NotNull TypeVariableMap typeVariables) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(typeVariables);

        Class<?> raw = rawType();
        TypeVariable<?>[] parameters = raw.getTypeParameters();
        return parameterizeInternal(raw, owner.get(), extractTypeArgumentsFrom(typeVariables.resolve(), parameters));
    }

    /**
     * Convenience overload for {@link Token#parameterizeWithOwner(Token, TypeVariableMap)}.
     * @param owner the owner class
     * @param typeVariables the parameters to use
     * @return a new token containing the result of parameterizing this type
     * @throws IllegalArgumentException if the given TypeVariableMap does not contain some necessary types
     */
    public final @NotNull Token<?> parameterizeWithOwner(@NotNull Class<?> owner,
        @NotNull TypeVariableMap typeVariables) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(typeVariables);

        Class<?> raw = rawType();
        TypeVariable<?>[] parameters = raw.getTypeParameters();
        return parameterizeInternal(raw, owner, extractTypeArgumentsFrom(typeVariables.resolve(), parameters));
    }

    /**
     * Creates a new token whose type is an array, whose component type is the current type of this token. If the type
     * represented by this token is a raw class, the returned token will contain the result of calling
     * {@link Class#arrayType()}. Otherwise, an appropriate implementation of {@link GenericArrayType} will be used
     * instead.
     *
     * @return a new token representing an array type
     */
    public final @NotNull Token<?> arrayType() {
        Type type = get();
        if (type instanceof Class<?> cls) {
            return Token.ofType(cls.arrayType());
        }

        return Token.ofType(GenericInfoRepository.bind(rawType(type), new InternalGenericArrayType(type)));
    }

    /**
     * Creates a new token whose type is the component type of the array type represented by this token. If this token
     * does not contain an array type, an {@link IllegalStateException} is thrown.
     *
     * @return a token containing the component type of the array type underlying this token
     */
    public final @NotNull Token<?> componentType() {
        Type type = get();
        if (!TypeUtils.isArrayType(type)) {
            throw new IllegalStateException("This token does not represent an array type");
        }

        return Token.ofType(TypeUtils.getArrayComponentType(type));
    }

    /**
     * Returns true if this token represents an array type; false otherwise. An array type is either a ray array or a
     * {@link GenericArrayType}.
     *
     * @return true if this token is an array type, false otherwise
     */
    public final boolean isArrayType() {
        return TypeUtils.isArrayType(get());
    }

    /**
     * Checks if this type is a subclass of the given class.
     *
     * @param type the potential superclass
     * @return true if this type subclasses (is assignable to) the given class, false otherwise
     */
    public final boolean isSubclassOf(@NotNull Class<?> type) {
        Objects.requireNonNull(type);
        return TypeUtils.isAssignable(get(), type);
    }

    /**
     * Checks if this type is a superclass of the given type.
     *
     * @param type the potential subclass
     * @return true if this type superclasses (is assignable from) the given type, false otherwise
     */
    public final boolean isSuperclassOf(@NotNull Token<?> type) {
        Objects.requireNonNull(type);
        return TypeUtils.isAssignable(type.get(), get());
    }


    /**
     * Checks if this type is a superclass of the given class.
     *
     * @param type the potential subclass
     * @return true if this type superclasses (is assignable from) the given class, false otherwise
     */
    public final boolean isSuperclassOf(@NotNull Class<?> type) {
        Objects.requireNonNull(type);
        return TypeUtils.isAssignable(type, get());
    }

    /**
     * Determines if this token represents a primitive or wrapper type.
     * @return true if this token represents a primitive or primitive wrapper type, false otherwise
     */
    public final boolean isPrimitiveOrWrapper() {
        return ClassUtils.isPrimitiveOrWrapper(rawType());
    }

    public final @NotNull Token<?>[] actualTypeParameters() {
        Type type = get();

        if (!isParameterized()) {
            throw new IllegalStateException("Expected a parameterized type");
        }

        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
        Token<?>[] tokens = new Token[args.length];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = Token.ofType(args[i]);
        }

        return tokens;
    }

    /**
     * Determines if this token represents a parameterized type or not.
     * @return true if the token's underlying type extends {@link ParameterizedType}, false otherwise
     */
    public final boolean isParameterized() {
        return get() instanceof ParameterizedType;
    }

    /**
     * Extracts a map containing all of the {@link TypeVariable} instances between this (parameterized) token and the
     * given subtype.
     *
     * @param subtype the subtype token, whose type must be assignable to this token's type
     * @return a {@link TypeVariableMap} of {@link TypeVariable}s to {@link Type}s
     */
    public final @NotNull TypeVariableMap subtypeVariables(@NotNull Token<?> subtype) {
        Objects.requireNonNull(subtype);

        Type type = get();
        if (!subtype.isSubclassOf(this)) {
            throw new IllegalStateException("This token's type is not assignable to the given token's type");
        }

        if (!isParameterized()) {
            throw new IllegalStateException("This token must be parameterized");
        }

        return new TypeVariableMap(TypeUtils.determineTypeArguments(subtype.rawType(), (ParameterizedType) type));
    }

    /**
     * Checks if this type is a subclass of the given type..
     *
     * @param type the potential superclass
     * @return true if this type subclasses (is assignable to) the given type, false otherwise
     */
    public final boolean isSubclassOf(@NotNull Token<?> type) {
        Objects.requireNonNull(type);
        return TypeUtils.isAssignable(get(), type.get());
    }

    /**
     * Convenience overload for {@link Token#subtypeVariables(Token)}.
     *
     * @param subtype the subtype class, which must be assignable to this token's type
     * @return a {@link TypeVariableMap} of {@link TypeVariable}s to {@link Type}s
     */
    public final @NotNull TypeVariableMap subtypeVariables(@NotNull Class<?> subtype) {
        Objects.requireNonNull(subtype);

        Type type = get();
        if (!TypeUtils.isAssignable(subtype, type)) {
            throw new IllegalArgumentException("Subtype class is not assignable to this token's type");
        }

        if (!isParameterized()) {
            throw new IllegalStateException("This token must be parameterized");
        }

        return new TypeVariableMap(TypeUtils.determineTypeArguments(subtype, (ParameterizedType) type));
    }

    /**
     * Extracts a map containing all of the {@link TypeVariable} instances between this token and the given
     * (parameterized) supertype.
     *
     * @param supertype the supertype token, to which this token's type must be assignable
     * @return a {@link TypeVariableMap} of {@link TypeVariable}s to {@link Type}s
     */
    public final @NotNull TypeVariableMap supertypeVariables(@NotNull Token<?> supertype) {
        Objects.requireNonNull(supertype);

        Type type = get();
        if (!isSubclassOf(supertype)) {
            throw new IllegalArgumentException("Token type is not assignable to the supertype class");
        }

        return new TypeVariableMap(TypeUtils.getTypeArguments(type, supertype.rawType()));
    }

    /**
     * Convenience overload for {@link Token#supertypeVariables(Token)}.
     *
     * @param supertype the supertype {@link Class}.
     * @return a {@link TypeVariableMap} of {@link TypeVariable}s to {@link Type}s
     */
    public final @NotNull TypeVariableMap supertypeVariables(@NotNull Class<?> supertype) {
        Objects.requireNonNull(supertype);

        Type type = get();
        if (!TypeUtils.isAssignable(type, supertype)) {
            throw new IllegalArgumentException("Token type is not assignable to the supertype class");
        }

        return new TypeVariableMap(TypeUtils.getTypeArguments(type, supertype));
    }

    /**
     * Gets the name of the type underlying this token. This method will not throw a {@link TypeNotPresentException} if
     * the underlying type has been garbage collected.
     *
     * @return the name of the underlying type
     */
    public final @NotNull String getTypeName() {
        return typeName;
    }

    @Override
    public final int hashCode() {
        return get().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        Type type = get();

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof Token<?> other) {
            return Objects.equals(type, other.get());
        }

        return false;
    }

    @Override
    public final String toString() {
        return "Token{type=" + get() + "}";
    }
}