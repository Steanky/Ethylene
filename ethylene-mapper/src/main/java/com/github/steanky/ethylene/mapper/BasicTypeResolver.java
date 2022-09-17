package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class BasicTypeResolver implements TypeResolver {
    private static final Object PRESENT = new Object();

    private final TypeHinter typeHinter;

    private final Cache<Class<?>, Reference<Class<?>>> typeCache;
    private final Cache<Class<?>, Reference<Class<?>>> exactCache;
    private final Cache<Class<?>, Object> hierarchyWalkFailures;

    public BasicTypeResolver(@NotNull TypeHinter typeHinter,
            @NotNull Collection<Entry<Class<?>, Class<?>>> typeImplementations) {
        this.typeHinter = Objects.requireNonNull(typeHinter);

        int size = typeImplementations.size();
        typeCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();
        exactCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();
        hierarchyWalkFailures = Caffeine.newBuilder().weakKeys().build();

        registerTypeImplementations(typeImplementations);
    }

    private void registerTypeImplementations(Collection<Entry<Class<?>, Class<?>>> typeImplementations) {
        for (Entry<Class<?>, Class<?>> entry : typeImplementations) {
            Class<?> implementation = entry.getFirst();
            Class<?> superclass = entry.getSecond();

            Objects.requireNonNull(implementation);
            Objects.requireNonNull(superclass);
            if (!TypeUtils.isAssignable(implementation, superclass)) {
                throw new MapperException(
                        "Implementation class '" + implementation.getTypeName() + "' is not assignable to superclass '" +
                                superclass.getTypeName() + "'");
            }

            if (typeCache.getIfPresent(superclass) != null) {
                throw new MapperException("An implementation class is already registered to superclass '" + superclass
                        .getTypeName() + "'");
            }

            typeCache.put(superclass, new WeakReference<>(implementation));
            exactCache.put(superclass, new WeakReference<>(implementation));
        }
    }

    @Override
    public @NotNull Type resolveType(@NotNull Type type, @Nullable ConfigElement element) {
        Objects.requireNonNull(type);

        Class<?> raw = ReflectionUtils.rawType(type);
        ElementType hint = typeHinter.getHint(raw);
        if (raw.isArray() || raw.isPrimitive()) {
            if (element != null && !hint.compatible(element)) {
                throw new MapperException("Incompatible type assignment '" + hint + "' to '" + type + "'");
            }

            return type;
        }

        Reference<Class<?>> classReference = exactCache.getIfPresent(raw);
        if (classReference != null) {
            Class<?> referent = classReference.get();
            if (referent != null) {
                //exact cache had a hit, we can avoid walking the class hierarchy
                if (type instanceof ParameterizedType parameterizedType) {
                    return Token.parameterize(referent, TypeUtils.determineTypeArguments(referent, parameterizedType))
                            .get();
                }

                return referent;
            }

            //this should not happen, if the entry has no reference to the class, the cache shouldn't either
            exactCache.invalidate(raw);
        }

        if (hierarchyWalkFailures.getIfPresent(raw) == null) {
            //no exact hit, we have to walk the class hierarchy this time, and we haven't previously failed at doing so
            for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
                Reference<Class<?>> superclassReference = typeCache.getIfPresent(superclass);
                if (superclassReference != null) {
                    Class<?> ref = superclassReference.get();
                    if (ref == null) {
                        //should not be possible
                        typeCache.invalidate(superclass);
                        continue;
                    }

                    //we'll never need to walk the hierarchy again for this exact type since it's in exactCache
                    //generic parameters might be different next time, though, and we'll have to construct them
                    exactCache.put(raw, superclassReference);

                    if (type instanceof ParameterizedType parameterizedType) {
                        return Token.parameterize(ref, TypeUtils.determineTypeArguments(ref, parameterizedType)).get();
                    }

                    return ref;
                }
            }

            //prevent us from redundantly walking the hierarchy again if we failed to find anything this time
            hierarchyWalkFailures.put(raw, PRESENT);
        }

        //there was no defined "implementation" class, so try to intelligently guess one that will work with our element
        if (element == null || hint.compatible(element)) {
            //trivial case: null elements are compatible with everything, directly-compatible hints are also good
            return type;
        }

        //try to guess what type we need based on the element type and any generic information we have
        Type elementType = switch (element.type()) {
            case NODE -> raw;
            case LIST -> {
                Type[] params = ReflectionUtils.extractGenericTypeParameters(type, raw);
                yield Token.parameterize(ArrayList.class, params.length == 0 ? Object.class : params[0]).get();
            }
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    //null is assignable to anything
                    yield type;
                }

                yield scalar.getClass();
            }
        };

        if (!TypeUtils.isAssignable(elementType, type)) {
            throw new MapperException("Element type '" + elementType + "' not compatible with '" + type.getTypeName() +
                    "'");
        }

        return elementType;
    }
}
