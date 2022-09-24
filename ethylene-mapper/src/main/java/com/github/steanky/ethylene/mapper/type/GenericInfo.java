package com.github.steanky.ethylene.mapper.type;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
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
class GenericInfo {
    static final byte PARAMETERIZED = 0;
    static final byte GENERIC_ARRAY = 1;
    static final byte TYPE_VARIABLE = 2;
    static final byte WILDCARD = 3;

    private static final byte[] NIL = new byte[] {0};

    //global mapping of class objects to generic info, may contain classes belonging to various classloaders
    private static final Cache<ClassLoader, GenericInfo> store;

    //we can't access the bootstrap classloader, so we can't keep this in the canonicalTypes map
    //bootstrap types won't be unloaded
    private static final Map<Type, Type> bootstrapTypes;

    private static final ReferenceQueue<Type> queue;
    private static final Thread cleanupThread;

    static {
        store = Caffeine.newBuilder().weakKeys().build();
        bootstrapTypes = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        cleanupThread = new Thread(GenericInfo::cleanup, "Ethylene Reference Cleanup Thread");
        cleanupThread.start();
    }

    private static void cleanup() {
        //reference cleanup thread never ends
        while(true) {
            try {
                TypeReference reference = (TypeReference)queue.remove();
                WeakType ownerType = reference.owner.get();

                //if ownerType is null, it's already been garbage collected - this is fine, we don't have to do anything
                //(it can't possibly be in any maps)
                if (ownerType != null) {
                    //as soon as a type has a child type go out-of-scope, we must remove it
                    Reference<GenericInfo> repository = reference.repository;
                    if (repository != null) {
                        GenericInfo genericInfo = repository.get();
                        if (genericInfo != null) {
                            genericInfo.canonicalTypes.remove(ownerType);
                        }
                    }
                    else {
                        //null repository means bootstrap owner type
                        bootstrapTypes.remove(ownerType);
                    }
                }
            } catch (InterruptedException ignored) {
                //ignored
            }
        }
    }

    private static class TypeReference extends WeakReference<Type> {
        private final Reference<WeakType> owner;
        private final Reference<GenericInfo> repository;

        TypeReference(WeakType referent, WeakType owner, GenericInfo repository) {
            super(referent, queue);
            this.owner = owner == null ? null : new WeakReference<>(owner);
            this.repository = repository == null ? null : new WeakReference<>(repository);
        }
    }

    //initially has a tiny capacity since we don't expect that many bound types per registered class
    private final Map<Type, Type> canonicalTypes = new ConcurrentHashMap<>(2);

    private GenericInfo() {
    }

    private static @NotNull Type bind(@NotNull WeakType type) {
        ClassLoader loader = type.getBoundClassloader();
        if (loader == null) {
            return bootstrapTypes.computeIfAbsent(type, Function.identity());
        }

        return store.get(loader, ignored -> new GenericInfo()).canonicalTypes.computeIfAbsent(type, Function
            .identity());
    }

    static @NotNull Type resolveType(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof Class<?>) {
            //raw classes don't need to be bound as they should already be retained by their classloader
            return type;
        }
        else if (type instanceof WeakType weakType) {
            //get the canonical instance of this weak type
            return bind(weakType);
        }
        else if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return bind(new WeakParameterizedType(rawType, parameterizedType.getOwnerType(), parameterizedType
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

            return bind(new WeakTypeVariable<>(typeVariable, i));
        }
        else if (type instanceof WildcardType wildcardType) {
            return bind(new WeakWildcardType(wildcardType));
        }
        else if(type instanceof GenericArrayType genericArrayType) {
            return bind(new WeakGenericArrayType(genericArrayType
                .getGenericComponentType()));
        }

        throw new IllegalArgumentException("Unexpected Type implementation '" + type.getClass().getName() + "'");
    }

    static void populate(@NotNull Type @NotNull [] types, @NotNull Reference<Type> @NotNull [] typeReferences,
        @NotNull String @NotNull [] names, @Nullable WeakType owner) {
        if (types.length != names.length) {
            throw new IllegalArgumentException("Types and names array must be the same length");
        }

        for (int i = 0; i < types.length; i++) {
            Type type = resolveType(types[i]);
            names[i] = type.getTypeName();
            typeReferences[i] = ref(type, owner);
        }
    }

    static @NotNull Reference<Type> ref(@NotNull Type type, @Nullable WeakType owner) {
        Type resolvedType = resolveType(type);
        if (resolvedType instanceof WeakType weakType) {
            ClassLoader loader = weakType.getBoundClassloader();
            if (loader == null) {
                return new TypeReference(weakType, owner, null);
            }

            GenericInfo repository = store.get(loader, ignored -> new GenericInfo());
            return new TypeReference(weakType, owner, repository);
        }

        return new WeakReference<>(resolvedType);
    }

    static byte @NotNull [] identifier(byte typeIdentifier, @Nullable String metadata, @Nullable Type ... components) {
        byte[][] componentByteArrays = new byte[components.length][];

        int i = 0;
        int totalComponentLength = 0;
        for (Type type : components) {
            byte[] newArray;
            if (type == null) {
                //null type represented by a byte array containing only zero
                //can be used to separate related groups of types
                newArray = NIL;
            }
            else if (type instanceof WeakType weakType) {
                //if weak type, use its identifier
                newArray = weakType.identifier();
            }
            else if (type instanceof Class<?> cls) {
                //if class, use its name
                newArray = StandardCharsets.US_ASCII.encode(cls.getName()).array();
            }
            else {
                //if any other type, use its type name
                newArray = StandardCharsets.US_ASCII.encode(type.getTypeName()).array();
            }

            componentByteArrays[i++] = newArray;
            totalComponentLength += newArray.length;
        }

        byte[] encodedMetadata = metadata == null ? NIL : StandardCharsets.US_ASCII.encode(metadata).array();
        //save first byte for type indicator, nameChars.length for length, rest for components
        byte[] composite = new byte[2 + totalComponentLength + encodedMetadata.length + (Math.max(0, components.length
            - 1))];
        composite[0] = typeIdentifier;

        int offset = 1;
        for (int j = 0; j < componentByteArrays.length; j++) {
            byte[] component = componentByteArrays[j];
            System.arraycopy(component, 0, composite, offset, component.length);
            offset += component.length;

            if (j < componentByteArrays.length - 1) {
                //type separator
                composite[offset++] = 0;
            }
        }

        composite[offset] = 0;

        //encode the metadata at the end
        System.arraycopy(encodedMetadata, 0, composite, offset + 1, encodedMetadata.length);

        return composite;
    }

    static byte @NotNull [] identifier(byte typeIdentifier, @Nullable Type ... components) {
        return identifier(typeIdentifier, null, components);
    }
}
