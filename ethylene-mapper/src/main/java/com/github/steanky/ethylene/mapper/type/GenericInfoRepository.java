package com.github.steanky.ethylene.mapper.type;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Internal class that can be used to bind custom implementations of {@link Type} to an "owner" class. Such bound
 * objects will not be garbage collected until the owner class is garbage collected, assuming there are no other strong
 * references whose lifetime exceeds that of the owner class.
 * <p>
 * In effect, this results in the lifetime of bound types being scoped to the lifetime of the classloader that created
 * the owner class. Assuming care is taken to avoid strong references, this can prevent memory leaks in certain
 * situations where there are multiple classloaders.
 * <p>
 * For example, take a situation where there are two classloaders: classloader A and classloader B. Classloader A is
 * responsible for loading Ethylene's classes. It also loads a user-defined class {@code EthyleneApiProvider} which
 * holds a long-lived reference to a {@code MappingConfigProcessor}. This processor contains a
 * {@code BasicSignatureMapper}, which itself holds a strong reference to a Signature array. However, a complication
 * occurs if one or more signatures in the array references a class loaded by classloader B. In this case, none of the
 * classes loaded by classloader B can ever be garbage collected before the underlying {@code MappingConfigProcessor}.
 * Since classes hold a reference to their classloader, the signature (which is indirectly statically referenced by a
 * class in classloader A, meaning it will not be garbage collected until the classloader is) is single-handedly keeping
 * classloader B loaded.
 * <p>
 * This edge case is a significant memory leak because classloaders indirectly keep all static references in their
 * loaded classes alive, and the issue compounds every single time a classloader is created and one of its managed
 * classes is referenced under the classloader used to load Ethylene.
 * <p>
 * This problem is avoided by refraining from keeping any strong references to class objects. Unfortunately, this also
 * means that most reflection-related objects are unsafe also, because many of them (directly or indirectly) reference a
 * class.
 * <p>
 * Custom implementations of Type (see also: {@link WeakGenericArrayType}, {@link WeakParameterizedType})
 * present a special issue, however, because they are not cached internally by the JDK. Therefore, weak references to
 * these types can be garbage collected <i>too soon</i>, before the classloader responsible for creating them is
 * destroyed.
 * <p>
 * This class is designed to contain the <i>only</i> strong references to such types in a weak-key cache, keyed by the
 * {@link Class} to which their lifetime should be scoped. It incidentally performs equality-based caching on said
 * custom instances (resulting in a form of object canonicalization, ensuring the returned objects are the same as the
 * ones actually held in the map).
 */
class GenericInfoRepository {
    //global mapping of class objects to generic info, may contain classes belonging to various classloaders
    private static final Cache<ClassLoader, GenericInfoRepository> store = Caffeine.newBuilder().weakKeys().build();

    //we can't access the bootstrap classloader, so we can't keep this in the canonicalTypes map
    //bootstrap types won't be unloaded
    private static final Map<Type, Type> bootstrapTypes = new ConcurrentHashMap<>();

    //initially has a tiny capacity since we don't expect that many bound types per registered class
    private final Map<Type, Type> canonicalTypes = new ConcurrentHashMap<>(2);

    private GenericInfoRepository() {
    }

    private static @NotNull Type bind(@NotNull Class<?> owner, @NotNull WeakType type) {
        ClassLoader loader = owner.getClassLoader();
        if (loader == null) {
            return bootstrapTypes.computeIfAbsent(type, Function.identity());
        }

        return store.get(loader, ignored -> new GenericInfoRepository()).canonicalTypes.computeIfAbsent(type, Function
            .identity());
    }

    static @NotNull Type resolveType(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof Class<?>) {
            //raw classes don't need to be bound as they should already be retained by their classloader
            return type;
        }
        else if (type instanceof WeakType weakType) {
            return bind(weakType.getBoundClass(), weakType);
        }
        else if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return bind(rawType, new WeakParameterizedType(rawType, parameterizedType.getOwnerType(), parameterizedType
                .getActualTypeArguments()));
        }
        else if (type instanceof TypeVariable<?> typeVariable) {
            GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
            int i = 0;
            for (TypeVariable<?> sample : genericDeclaration.getTypeParameters()) {
                if (sample == typeVariable) {
                    break;
                }

                i++;
            }

            return bind(ReflectionUtils.getOwner(genericDeclaration), new WeakTypeVariable<>(typeVariable, i));
        }
        else if (type instanceof WildcardType wildcardType) {
            return bind(ReflectionUtils.rawType(wildcardType), new WeakWildcardType(wildcardType));
        }
        else if(type instanceof GenericArrayType genericArrayType) {
            return bind(ReflectionUtils.rawType(genericArrayType), new WeakGenericArrayType(genericArrayType
                .getGenericComponentType()));
        }

        throw new IllegalArgumentException("Unexpected Type implementation '" + type.getClass().getName() + "'");
    }

    static void populate(@NotNull Type @NotNull [] types, @NotNull Reference<Type> @NotNull [] typeReferences,
        @NotNull String @NotNull [] names) {
        if (types.length != names.length) {
            throw new IllegalArgumentException("Types and names array must be the same length");
        }

        for (int i = 0; i < types.length; i++) {
            Type type = resolveType(types[i]);
            names[i] = type.getTypeName();
            typeReferences[i] = new WeakReference<>(type);
        }
    }
}
