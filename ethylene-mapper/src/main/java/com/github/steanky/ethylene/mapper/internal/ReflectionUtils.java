package com.github.steanky.ethylene.mapper.internal;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.propylene.Parser;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.annotation.Default;
import com.github.steanky.ethylene.mapper.annotation.Name;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains some reflection-related utilities. These generally serve to supplement those already found in Apache
 * Commons.
 * <p>
 * This class and all of its methods are public to enable cross-package access, but must not be considered part of the
 * public API.
 */
@ApiStatus.Internal
public class ReflectionUtils {
    /**
     * The shared empty type array.
     */
    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    /**
     * The shared empty {@link Signature} array.
     */
    public static final Signature<?>[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    /**
     * The shared empty {@link ScalarSignature} array.
     */
    public static final ScalarSignature<?>[] EMPTY_SCALAR_SIGNATURE_ARRAY = new ScalarSignature[0];

    /**
     * Extracts the name of a field. This is either its name as defined in the source code, or the value of a present
     * {@link Name} annotation. If such an annotation is present, its value is returned. Otherwise, it defaults to the
     * field name.
     *
     * @param field the field to extract a name from
     * @return the field's name
     */
    public static @NotNull String getFieldName(@NotNull Field field) {
        Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
        return nameAnnotation == null ? field.getName() : nameAnnotation.value();
    }


    /**
     * Resolves the given reference to a Type. If it is not present, throws a {@link TypeNotPresentException}.
     *
     * @param typeReference the type reference
     * @param typeName      the type name
     * @param <TType>       the type object
     * @return the type itself
     */
    public static <TType extends Type> @NotNull TType resolve(@NotNull Reference<TType> typeReference,
        @NotNull String typeName) {
        TType type = typeReference.get();
        if (type == null) {
            throw new TypeNotPresentException(typeName, null);
        }

        return type;
    }

    /**
     * Resolves a number of type references to concrete types. If any type is null (missing), throw a
     * {@link TypeNotPresentException}.
     *
     * @param typeReferences the references from which to extract types
     * @param names          the corresponding names of each type
     * @param type           the class of type, used to create the type array; must be assignable to {@code TType}
     * @param <TType>        the subclass of {@link Type}
     * @return a resolved type array
     */
    @SuppressWarnings("unchecked")
    public static <TType extends Type> @NotNull TType[] resolve(@NotNull Reference<TType>[] typeReferences,
        @NotNull String[] names, @NotNull Class<?> type) {
        if (typeReferences.length != names.length) {
            throw new IllegalArgumentException("Reference array and name array must be the same length");
        }

        TType[] array = (TType[]) Array.newInstance(type, typeReferences.length);
        for (int i = 0; i < typeReferences.length; i++) {
            array[i] = resolve(typeReferences[i], names[i]);
        }

        return array;
    }

    /**
     * Returns the {@link ClassLoader} responsible for the given type. For class objects, this is the value returned by
     * {@link Class#getClassLoader()}. For {@link ParameterizedType}, this is the class loader of its raw type. For
     * {@link TypeVariable}, this is the classloader of the declaring class of the {@link GenericDeclaration} the
     * variable is part of. For {@link GenericArrayType} and {@link WildcardType}, this method returns {@code null}.
     *
     * @param type the type to retrieve a classloader from
     * @return the classloader of the type
     */
    public static @Nullable ClassLoader getClassLoader(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof Class<?> cls) {
            return cls.getClassLoader();
        } else if (type instanceof ParameterizedType parameterizedType) {
            return ((Class<?>) parameterizedType.getRawType()).getClassLoader();
        } else if (type instanceof TypeVariable<?> typeVariable) {
            return getOwner(typeVariable.getGenericDeclaration()).getClassLoader();
        } else if (type instanceof GenericArrayType || type instanceof WildcardType) {
            return null;
        }

        throw new IllegalArgumentException("Unexpected subclass of Type '" + type.getClass().getName() + "'");
    }

    /**
     * Resolves the underlying raw type of the given {@link Type} object. Supports implementations of
     * {@link ParameterizedType}. {@link TypeVariable}, {@link GenericArrayType}, {@link WildcardType}, and
     * {@link Class}. Any other subclasses of {@link Type} are not supported and will cause this method to throw an
     * exception.
     * <p>
     * Class objects are returned as-is. {@link ParameterizedType#getRawType()} is cast to Class and returned. The first
     * upper bound of a {@link TypeVariable} is recursively resolved and the result returned. A regular array type is
     * recursively resolved and returned for {@link GenericArrayType}. The first upper bound of a {@link WildcardType}
     * is recursively resolved and returned.
     *
     * @param type the type to extract a raw class from
     * @return the raw class
     */
    public static @NotNull Class<?> rawType(@NotNull Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            return rawType(typeVariable.getBounds()[0]);
        }

        if (type instanceof GenericArrayType genericArrayType) {
            return Array.newInstance(rawType(genericArrayType.getGenericComponentType()), 0).getClass();
        }

        if (type instanceof WildcardType wildcardType) {
            return rawType(wildcardType.getUpperBounds()[0]);
        }

        throw new IllegalArgumentException("Unexpected subclass of Type '" + type.getClass().getName() + "'");
    }

    /**
     * Gets the owner class of the given {@link GenericDeclaration}.
     * <p>
     * If the generic declaration is itself a class, it is returned. Otherwise, if the declaration is an
     * {@link Executable}, its declaring class is returned. If the {@link GenericDeclaration} is not a class object, or
     * subclass of Executable, an {@link IllegalArgumentException} is thrown.
     *
     * @param genericDeclaration the declaration from which to extract an owner
     * @return the owner class
     */
    public static @NotNull Class<?> getOwner(@NotNull GenericDeclaration genericDeclaration) {
        if (genericDeclaration instanceof Class<?> cls) {
            return cls;
        } else if (genericDeclaration instanceof Executable executable) {
            return executable.getDeclaringClass();
        }

        throw new IllegalArgumentException(
            "Unexpected subclass of GenericDeclaration '" + genericDeclaration.getClass().getName() + "'");
    }

    /**
     * Simple check to ensure that the provided token is not abstract; if it is, an error is thrown.
     * @param token the token to check
     */
    public static void validateNotAbstract(Token<?> token) {
        Class<?> rawType = token.rawType();
        int modifiers = rawType.getModifiers();
        if (!rawType.isArray() && Modifier.isAbstract(modifiers)) {
            throw new MapperException("Type " + rawType + " is abstract and cannot be instantiated; a concrete type " +
                "implementation must be registered");
        }
    }

    /**
     * Constructs a map of parameter names to {@link ConfigElement}s representing the default values for the class.
     *
     * @param objectClass the class to search in
     * @return an unmodifiable map of strings to methods
     */
    public static @NotNull @Unmodifiable Map<String, ConfigElement> constructDefaultValueMap(@NotNull Class<?> objectClass) {
        Map<String, ConfigElement> methodMap = new HashMap<>(4);
        Default classDefault = objectClass.getAnnotation(Default.class);
        if (classDefault != null) {
            ConfigElement defaultSpec;
            try {
                defaultSpec = Parser.fromString(classDefault.value(), LinkedConfigNode::new, ArrayConfigList::new);
            }
            catch (IOException e) {
                throw new MapperException("Invalid Propylene in class @Default annotation", e);
            }

            if (!defaultSpec.isNode()) {
                throw new MapperException("Wrong top-level type for configuration produced by @Default");
            }

            for (ConfigEntry entry : defaultSpec.asNode().entryCollection()) {
                String name = entry.getKey();
                putThrowIfDuplicate(methodMap, name, entry.getValue());
            }

            return Map.copyOf(methodMap);
        }

        if (objectClass.isRecord()) {
            boolean usesAnyComponentDefaults = false;
            for (RecordComponent recordComponent : objectClass.getRecordComponents()) {
                Default defaultAnnotation = recordComponent.getAnnotation(Default.class);
                if (defaultAnnotation == null) {
                    continue;
                }

                String name = recordComponent.getName();

                ConfigElement defaultValue;
                try {
                    defaultValue = Parser.fromString(defaultAnnotation.value(), LinkedConfigNode::new,
                        ArrayConfigList::new);
                }
                catch (IOException e) {
                    throw new MapperException("Invalid Propylene in @Default annotation", e);
                }

                putThrowIfDuplicate(methodMap, name, defaultValue);
                usesAnyComponentDefaults = true;
            }

            if (usesAnyComponentDefaults) {
                return Map.copyOf(methodMap);
            }
        }

        for (Method method : objectClass.getDeclaredMethods()) {
            Default defaultAnnotation = method.getAnnotation(Default.class);
            if (defaultAnnotation == null) {
                continue;
            }

            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
                throw new MapperException("Default value supplier method must be declared public static");
            }

            if (!ConfigElement.class.isAssignableFrom(method.getReturnType())) {
                throw new MapperException("Default value supplier method must return a type assignable to ConfigElement");
            }

            if (method.getParameterCount() != 0) {
                throw new MapperException("Default value supplier methods must be parameterless");
            }

            String name = defaultAnnotation.value();

            ConfigElement defaultValue;
            try {
                defaultValue = (ConfigElement) method.invoke(null);
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new MapperException("Error invoking default value accessor method", e);
            }

            if (defaultValue == null) {
                throw new MapperException("Default value accessor method returned null");
            }

            putThrowIfDuplicate(methodMap, name, defaultValue);
        }

        return Map.copyOf(methodMap);
    }

    private static void putThrowIfDuplicate(Map<String, ConfigElement> methodMap, String key, ConfigElement value) {
        if (methodMap.put(key, value) != null) {
            throw new MapperException("Duplicate default value suppliers for parameter named " + key);
        }
    }
}
