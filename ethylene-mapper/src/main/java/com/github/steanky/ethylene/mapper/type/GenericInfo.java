package com.github.steanky.ethylene.mapper.type;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.charset.Charset;
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
 * Custom implementations of Type (see also: {@link WeakGenericArrayType}, {@link WeakParameterizedType}) present a
 * special issue, however, because they are not cached internally by the JDK. Therefore, weak references to these types
 * can be garbage collected <i>too soon</i>, before the classloader responsible for creating them is destroyed.
 * <p>
 * This class is designed to contain the <i>only</i> strong references to such types in a weak-key cache, keyed by the
 * {@link Class} to which their lifetime should be scoped. It incidentally performs equality-based caching on said
 * custom instances (resulting in a form of object canonicalization, ensuring the returned objects are the same as the
 * ones actually held in the map).
 * <p>
 * Types which are referenced by other types (child types), such as the parameter of a {@link WeakParameterizedType},
 * are held in special references that remove their parent from its cache when garbage collected. This is accomplished
 * through the use of a {@link ReferenceQueue} and a cleanup daemon thread.
 */
class GenericInfo {
    /**
     * Byte indicating a parameterized {@link WeakType}.
     */
    static final byte PARAMETERIZED = 0;

    /**
     * Byte indicating a generic array {@link WeakType}.
     */
    static final byte GENERIC_ARRAY = 1;

    /**
     * Byte indicating a type variable {@link WeakType}.
     */
    static final byte TYPE_VARIABLE = 2;

    /**
     * Byte indicating a wildcard {@link WeakType}.
     */
    static final byte WILDCARD = 3;

    /**
     * Standard charset used by {@link GenericInfo#identifier(byte, String, Type...)}.
     */
    static final Charset CHARSET = StandardCharsets.UTF_8;

    //used by GenericInfo#identifier
    private static final byte[] NIL = new byte[]{0};

    //global mapping of class objects to generic info, may contain classes belonging to various classloaders
    private static final Cache<ClassLoader, GenericInfo> store;

    //we can't access the bootstrap classloader, so we can't keep this in the canonicalTypes map
    //bootstrap types won't be unloaded (unless a child type is GCd)
    private static final Map<Type, Type> bootstrapTypes;

    private static final ReferenceQueue<Type> queue;
    private static final Thread cleanupThread;

    static {
        store = Caffeine.newBuilder().weakKeys().build();
        bootstrapTypes = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        cleanupThread = new Thread(GenericInfo::cleanup, "Ethylene Reference Cleanup Thread");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    //initially has a tiny capacity since we don't expect that many bound types per registered class
    private final Map<Type, Type> canonicalTypes = new ConcurrentHashMap<>(2);

    private GenericInfo() {
    }

    private static void cleanup() {
        //reference cleanup thread never ends
        while (true) {
            try {
                //wait for a TypeReference to enter the queue
                //this happens when its associated type is garbage-collected
                //if there is a parent type, we need to remove it from the map
                TypeReference reference = (TypeReference) queue.remove();
                WeakType ownerType = reference.parent == null ? null : reference.parent.get();

                //if ownerType is null, it's already been garbage collected - this is fine, we don't have to do anything
                //(it can't possibly be in any maps)
                //alternatively, this type has no owner, in which case we also don't have to remove anything
                if (ownerType != null) {
                    //as soon as a type has a child type go out-of-scope, we must remove it
                    Reference<GenericInfo> ownerRepository = reference.repository;
                    if (ownerRepository != null) {
                        GenericInfo genericInfo = ownerRepository.get();
                        if (genericInfo != null) {
                            genericInfo.canonicalTypes.remove(ownerType);
                        }
                    } else {
                        //null repository means bootstrap owner classloader
                        bootstrapTypes.remove(ownerType);
                    }
                }
            } catch (Throwable ignored) {
                //ignored
            }
        }
    }

    private static @NotNull Type bind(@NotNull WeakType type, @Nullable ClassLoader loader) {
        if (loader == null) {
            return bootstrapTypes.computeIfAbsent(type, Function.identity());
        }

        return store.get(loader, ignored -> new GenericInfo()).canonicalTypes.computeIfAbsent(type,
            Function.identity());
    }

    /**
     * Canonicalizes the given type, creating and caching a new representative {@link WeakType} if necessary. The
     * returned type is guaranteed to be safe for storage in a {@link Reference}, meaning that it will only be garbage
     * collected when:
     * <ul>
     *   <li>{@code classLoader} is garbage collected, or</li>
     *   <li>one of the <b>child types</b> of the provided type is garbage collected.</li>
     * </ul>
     * Child types are defined as types that comprise another type. For example, {@link ParameterizedType} may have any
     * number of "type parameters" who are in this case its children. Likewise, {@link WildcardType} has upper and lower
     * bound types; these are its children.
     *
     * @param type the type to resolve
     * @return a {@link Class} if {@code type} was a class; otherwise, a {@link WeakType} implementation corresponding
     * to a kind of generic type
     */
    static @NotNull Type resolveType(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof Class<?>) {
            return type;
        }

        ClassLoader classLoader = ReflectionUtils.getClassLoader(type);
        if (type instanceof WeakType weakType) {
            //get the canonical instance of this weak type
            return bind(weakType, classLoader);
        } else if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return bind(new WeakParameterizedType(rawType, parameterizedType.getOwnerType(),
                parameterizedType.getActualTypeArguments()), classLoader);
        } else if (type instanceof TypeVariable<?> typeVariable) {
            GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
            int i = 0;
            for (TypeVariable<?> sample : genericDeclaration.getTypeParameters()) {
                if (sample == typeVariable) {
                    break;
                }

                i++;
            }

            return bind(new WeakTypeVariable<>(typeVariable, i), classLoader);
        } else if (type instanceof WildcardType wildcardType) {
            return bind(new WeakWildcardType(wildcardType), classLoader);
        } else if (type instanceof GenericArrayType genericArrayType) {
            return bind(new WeakGenericArrayType(genericArrayType.getGenericComponentType()), classLoader);
        }

        throw new IllegalArgumentException("Unexpected Type implementation '" + type.getClass().getName() + "'");
    }

    /**
     * Creates a new reference to the given {@link Type}, after resolving it (see
     * {@link GenericInfo#resolveType(Type)}).
     * <p>
     * We accept a {@link ClassLoader} as a parameter rather than inferring it from the parent using
     * {@link ReflectionUtils#getClassLoader(Type)}, because it may not be fully constructed when this method is
     * invoked.
     *
     * @param type              the type for which to construct a reference
     * @param parent            the parent type
     * @param parentClassLoader the {@link ClassLoader} of the parent
     * @return a new reference object
     */
    static @NotNull Reference<Type> ref(@NotNull Type type, @Nullable WeakType parent,
        @Nullable ClassLoader parentClassLoader) {
        return new TypeReference(resolveType(type), parent, parentClassLoader);
    }

    /**
     * Convenience method for bulk-creating a reference and name array. References are created using
     * {@link GenericInfo#ref(Type, WeakType, ClassLoader)}. Each reference will have the same parent and parent
     * classloader.
     *
     * @param types             an array of type objects from which to construct references
     * @param typeReferences    an empty array of {@link Reference} objects which will be populated by this method
     * @param typeNames         an empty array of strings which will be populated by this method
     * @param parent            the common parent of all the types in the given array
     * @param parentClassLoader the classloader of the common parent
     */
    static void populate(@NotNull Type @NotNull [] types, @NotNull Reference<Type> @NotNull [] typeReferences,
        @NotNull String @NotNull [] typeNames, @Nullable WeakType parent, @Nullable ClassLoader parentClassLoader) {
        if (types.length != typeNames.length) {
            throw new IllegalArgumentException("Types and names array must be the same length");
        }

        for (int i = 0; i < types.length; i++) {
            Type type = resolveType(types[i]);
            typeNames[i] = type.getTypeName();
            typeReferences[i] = ref(type, parent, parentClassLoader);
        }
    }

    /**
     * Generates a type identifier. Used to compare implementations of {@link WeakType}.
     * <p>
     * The returned byte array is generated according to the following protocol:
     * <p>
     * [typeIdentifier] 1 byte<br>
     * <p>
     * [component-1] n bytes, n > 0<br> [0] 1 byte<br>
     * <p>
     * [component-2] q bytes, q > 0<br> [0] 1 byte<br>
     * <p>
     * [...]<br>
     * <p>
     * [component-n] p bytes, p > 0<br> [0] 1 bytes<br>
     * <p>
     * [metadata] r bytes, r >= 0<p> Where {@code typeIdentifier} is a single byte representing the generic type
     * component, {@code component} is a UTF-8 encoded string constructed from the type name of each component type, and
     * {@code 0} is a zero byte.
     * <p>
     * Null components are also represented by the zero byte.
     *
     * @param typeIdentifier the type identifier byte
     * @param metadata       the metadata string
     * @param components     the type components
     * @return the identifier byte array for the given arguments
     * @see GenericInfo#PARAMETERIZED
     * @see GenericInfo#GENERIC_ARRAY
     * @see GenericInfo#TYPE_VARIABLE
     * @see GenericInfo#WILDCARD
     */
    static byte @NotNull [] identifier(byte typeIdentifier, @Nullable String metadata, @Nullable Type... components) {
        byte[][] componentByteArrays = new byte[components.length][];

        int i = 0;
        int totalComponentLength = 0;
        for (Type type : components) {
            byte[] newArray;
            if (type == null) {
                //null type represented by a byte array containing only zero
                //can be used to separate related groups of types
                newArray = NIL;
            } else if (type instanceof WeakType weakType) {
                //if weak type, use its identifier
                newArray = weakType.identifier();
            } else if (type instanceof Class<?> cls) {
                //if class, use its name (this has a nice, compact encoding)
                newArray = CHARSET.encode(cls.getName()).array();
            } else {
                //if any other type, use its type name
                newArray = CHARSET.encode(type.getTypeName()).array();
            }

            componentByteArrays[i++] = newArray;
            totalComponentLength += newArray.length;
        }

        byte[] encodedMetadata = metadata == null ? NIL : CHARSET.encode(metadata).array();
        //save first byte for type indicator, nameChars.length for length, rest for components
        byte[] composite =
            new byte[2 + totalComponentLength + encodedMetadata.length + (Math.max(0, components.length - 1))];
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

    /**
     * Convenience overload for {@link GenericInfo#identifier(byte, String, Type...)}. The metadata parameter is set to
     * null.
     *
     * @param typeIdentifier the type identifier byte
     * @param components     the type components
     * @return the identifier byte array for the given arguments and null metadata
     */
    static byte @NotNull [] identifier(byte typeIdentifier, @Nullable Type... components) {
        return identifier(typeIdentifier, null, components);
    }

    //custom WeakReference containing some additional data
    private static class TypeReference extends WeakReference<Type> {
        private final Reference<WeakType> parent;
        private final Reference<GenericInfo> repository;

        private TypeReference(Type referent, WeakType parent, ClassLoader parentClassLoader) {
            super(referent, queue);

            if (parent != null) {
                this.parent = new WeakReference<>(parent);

                GenericInfo info = parentClassLoader == null ? null : store.getIfPresent(parentClassLoader);
                if (info != null) {
                    //if there is a repository, retain only a weak reference to it
                    this.repository = new WeakReference<>(info);
                } else {
                    //no repository means bootstrap or unknown classloader
                    this.repository = null;
                }
            } else {
                this.parent = null;
                this.repository = null;
            }
        }
    }
}
