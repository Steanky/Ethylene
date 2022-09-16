package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;
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
 * Custom implementations of Type (see also: {@link InternalGenericArrayType}, {@link InternalParameterizedType})
 * present a special issue, however, because they are not cached internally by the JVM. Therefore, weak references to
 * these types can be garbage collected <i>too soon</i>, before the classloader responsible for creating them is
 * destroyed.
 * <p>
 * This class is designed to contain the <i>only</i> strong references to such types in a {@link WeakHashMap}, keyed by
 * the {@link Class} to which their lifetime should be scoped. It incidentally performs equality-based caching on said
 * custom instances (resulting in a form of object canonicalization, ensuring the returned objects are the same as the
 * ones actually held in the map).
 */
class GenericInfoRepository {
    //global mapping of class objects to generic info, may contain classes belonging to various classloaders
    private static final Map<Class<?>, GenericInfoRepository> store = new WeakHashMap<>();

    private final Map<Type, Type> canonicalTypes = new ConcurrentHashMap<>(4);

    private GenericInfoRepository() {}

    /**
     * Binds the given type to the owner type. If an equal type has previously been created, the old type is returned.
     * Otherwise, the given type is cached and returned.<p>
     * <p>
     * This method is thread-safe.
     *
     * @param owner the owner class
     * @param type  the {@link CustomType} to cache
     * @return the cached type if present, else {@code type}
     */
    static @NotNull Type bind(@NotNull Class<?> owner, @NotNull CustomType type) {
        GenericInfoRepository repository;
        synchronized (store) {
            repository = store.computeIfAbsent(owner, ignored -> new GenericInfoRepository());
        }

        //we can give up the global lock on store once we retrieve the repository, canonicalTypes is a ConcurrentHashMap
        return repository.canonicalTypes.computeIfAbsent(type, Function.identity());
    }
}
