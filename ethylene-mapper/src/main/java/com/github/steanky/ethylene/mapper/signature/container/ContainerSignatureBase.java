package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureParameter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Shared base class for all containers (maps, collections, and arrays). It supports building objects and caches
 * constructor information for performance.
 *
 * @param <T> the container type
 * @see MapSignature
 * @see CollectionSignature
 * @see ArraySignature
 */
public abstract class ContainerSignatureBase<T> extends PrioritizedBase implements Signature<T> {
    /**
     * The immutable {@link Map.Entry} returned by the {@link Signature#argumentTypes()} iterator for this class.
     */
    protected final Map.Entry<String, SignatureParameter> entry;

    /**
     * The container type token, for use by subclasses.
     */
    protected final Token<T> containerType;

    private Reference<ConstructorInfo> constructorInfoReference = new SoftReference<>(null);

    /**
     * Creates a new instance of this class.
     *
     * @param componentType the "component type" of this container; for example, a Map's component type would be some
     *                      generic {@link Map.Entry}
     * @param containerType the full generic type of the container
     */
    public ContainerSignatureBase(@NotNull Token<?> componentType, @NotNull Token<T> containerType) {
        super(0);
        this.entry = Entry.of(null, SignatureParameter.parameter(componentType));
        this.containerType = containerType;

        ReflectionUtils.validateNotAbstract(containerType);
    }

    /**
     * Resolves the constructor information for this signature, caching values as necessary.
     *
     * @return a {@link ConstructorInfo} instance representing the constructor information
     */
    protected final @NotNull ConstructorInfo resolveConstructor() {
        ConstructorInfo cached = constructorInfoReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> rawClass = containerType.rawType();
        Constructor<?> constructor;
        try {
            constructor = rawClass.getConstructor(int.class);
        } catch (NoSuchMethodException e) {
            constructor = null;
        }

        boolean parameterless;
        if (constructor == null) {
            try {
                constructor = rawClass.getConstructor();
            }
            catch (NoSuchMethodException ignored) {
                throw new MapperException("No suitable collection constructor found for '" + rawClass + "'");
            }

            parameterless = true;
        } else {
            parameterless = false;
        }

        ConstructorInfo constructorInfo = new ConstructorInfo(parameterless, constructor);
        constructorInfoReference = new SoftReference<>(constructorInfo);
        return constructorInfo;
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, SignatureParameter>> argumentTypes() {
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Map.Entry<String, SignatureParameter> next() {
                return entry;
            }
        };
    }

    @Override
    public @NotNull @Unmodifiable Map<String, Token<?>> genericMappings() {
        return Map.of();
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new ArrayConfigList(sizeHint);
    }

    @Override
    public boolean hasBuildingObject() {
        return true;
    }

    @Override
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        if (!element.isContainer()) {
            throw new MapperException("Expected container, got '" + element.type() + "'");
        }

        return makeBuildingObject(element.asContainer());
    }

    @Override
    public boolean matchesArgumentNames() {
        return false;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public int length(@Nullable ConfigElement configElement) {
        if (configElement == null || !configElement.isContainer()) {
            return -1;
        }

        return configElement.asContainer().elementCollection().size();
    }

    @Override
    public int uniqueLength() {
        return 1;
    }

    @Override
    public @NotNull Token<T> returnType() {
        return containerType;
    }

    /**
     * Creates the building object for this container.
     *
     * @param container the {@link ConfigContainer} used to create the building object
     * @return the new building object
     */
    protected abstract @NotNull Object makeBuildingObject(@NotNull ConfigContainer container);

    /**
     * Information about a constructor. Container constructors may be parameterless (in which case they take no
     * parameters) or non-parameterless (in which case they take a single integer value for their initial size).
     *
     * @param parameterless whether this constructor is parameterless
     * @param constructor   the constructor itself
     */
    protected record ConstructorInfo(boolean parameterless, @NotNull Constructor<?> constructor) {
        /**
         * Creates a new instance of this record.
         *
         * @param parameterless whether this constructor is parameterless
         * @param constructor   the constructor itself
         */
        public ConstructorInfo(boolean parameterless, @NotNull Constructor<?> constructor) {
            this.parameterless = parameterless;
            this.constructor = Objects.requireNonNull(constructor);
        }
    }
}
