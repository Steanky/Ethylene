package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class BasicTypeResolver implements TypeResolver {
    private static final Object PRESENT = new Object();

    private final TypeHinter typeHinter;

    private final Cache<Class<?>, Reference<Class<?>>> typeCache;
    private final Cache<Class<?>, Reference<Class<?>>> exactCache;
    private final Cache<Class<?>, Object> hierarchyWalkFailures;

    public BasicTypeResolver(@NotNull TypeHinter typeHinter,
        @NotNull Collection<Map.Entry<Class<?>, Class<?>>> typeImplementations) {
        this.typeHinter = Objects.requireNonNull(typeHinter);

        int size = typeImplementations.size();
        typeCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();
        exactCache = Caffeine.newBuilder().initialCapacity(size).weakKeys().build();
        hierarchyWalkFailures = Caffeine.newBuilder().weakKeys().build();

        registerTypeImplementations(typeImplementations);
    }

    private void registerTypeImplementations(Collection<Map.Entry<Class<?>, Class<?>>> typeImplementations) {
        for (Map.Entry<Class<?>, Class<?>> entry : typeImplementations) {
            Class<?> implementation = entry.getKey();
            Class<?> superclass = entry.getValue();

            Objects.requireNonNull(implementation);
            Objects.requireNonNull(superclass);
            if (!TypeUtils.isAssignable(implementation, superclass)) {
                throw new MapperException(
                    "Implementation class '" + implementation.getTypeName() + "' is not assignable to superclass '" +
                        superclass.getTypeName() + "'");
            }

            if (typeCache.getIfPresent(superclass) != null) {
                throw new MapperException(
                    "An implementation class is already registered to superclass '" + superclass.getTypeName() + "'");
            }

            typeCache.put(superclass, new WeakReference<>(implementation));
            exactCache.put(superclass, new WeakReference<>(implementation));
        }
    }

    @Override
    public @NotNull Token<?> resolveType(@NotNull Token<?> type, @Nullable ConfigElement element) {
        Objects.requireNonNull(type);

        ElementType hint = typeHinter.getHint(type);
        if (type.isArrayType() || type.isPrimitiveOrWrapper()) {
            if (element != null && element.type() != hint) {
                throw new MapperException("Incompatible type assignment '" + hint + "' to '" + type + "'");
            }

            return type;
        }

        Class<?> raw = type.rawType();
        Reference<Class<?>> classReference = exactCache.getIfPresent(raw);
        if (classReference != null) {
            Class<?> referent = classReference.get();
            if (referent != null) {
                Token<?> referentType = Token.ofType(referent);

                //exact cache had a hit, we can avoid walking the class hierarchy
                if (type.isParameterized()) {
                    return referentType.parameterize(type.subtypeVariables(referent));
                }

                return referentType;
            }

            //this should not happen, if the entry has no reference to the class, the cache shouldn't either
            exactCache.invalidate(raw);
        }

        if (hierarchyWalkFailures.getIfPresent(raw) == null) {
            //no exact hit, we have to walk the class hierarchy this time, and we haven't previously failed at doing so
            for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
                Reference<Class<?>> superclassReference = typeCache.getIfPresent(superclass);
                if (superclassReference != null) {
                    Class<?> referent = superclassReference.get();
                    if (referent == null) {
                        //should not be possible
                        typeCache.invalidate(superclass);
                        continue;
                    }

                    //we'll never need to walk the hierarchy again for this exact type since it's in exactCache
                    //generic parameters might be different next time, though, and we'll have to construct them
                    exactCache.put(raw, superclassReference);

                    Token<?> referentToken = Token.ofType(referent);
                    if (type.isParameterized()) {
                        return referentToken.parameterize(type.subtypeVariables(referent));
                    }

                    return referentToken;
                }
            }

            //prevent us from redundantly walking the hierarchy again if we failed to find anything this time
            hierarchyWalkFailures.put(raw, PRESENT);
        }

        //there was no defined "implementation" class, so try to intelligently guess one that will work with our element
        if (element == null || hint == element.type()) {
            //trivial case: null elements are compatible with everything, directly-compatible hints are also good
            return type;
        }

        //use the element's preferred type as a last-ditch effort
        Token<?> elementType = typeHinter.getPreferredType(element, type);

        //fail if the preferred type isn't assignable
        if (!elementType.isSubclassOf(type)) {
            throw new MapperException(
                "Inferred element type '" + elementType + "' not compatible with '" + type.getTypeName() + "'");
        }

        return elementType;
    }
}
