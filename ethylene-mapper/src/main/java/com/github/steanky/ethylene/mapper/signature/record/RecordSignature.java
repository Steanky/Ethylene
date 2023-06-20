package com.github.steanky.ethylene.mapper.signature.record;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureParameter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/**
 * {@link Signature} implementation meant to create record types. This does not support building objects.
 *
 * @param <T> the actual type of the record
 */
public class RecordSignature<T> extends PrioritizedBase implements Signature<T> {
    private final Token<T> genericReturnType;
    private final Reference<Class<?>> rawClassReference;
    private final String rawClassName;
    //cache constructor and RecordComponent array, be prepared to re-generate them if necessary since they can get
    //garbage-collected
    private Reference<Constructor<?>> constructorReference = new SoftReference<>(null);
    private Reference<RecordComponent[]> recordComponentsReference = new SoftReference<>(null);
    //safe, does not actually retain strong references to Type objects
    private Info info;

    /**
     * Creates a new instance of this class.
     *
     * @param genericReturnType the return type of this signature
     */
    public RecordSignature(@NotNull Token<T> genericReturnType) {
        super(0);
        this.genericReturnType = Objects.requireNonNull(genericReturnType);

        Class<?> rawClass = genericReturnType.rawType();
        if (!rawClass.isRecord()) {
            throw new MapperException("'" + rawClass + "' is not a record");
        }

        this.rawClassReference = new WeakReference<>(rawClass);
        this.rawClassName = rawClass.getName();

        ReflectionUtils.validateNotAbstract(genericReturnType);
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, SignatureParameter>> argumentTypes() {
        return resolveInfo().types;
    }

    @Override
    public @NotNull @Unmodifiable Map<String, Token<?>> genericMappings() {
        return resolveInfo().varMappings;
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull T object) {
        RecordComponent[] components = resolveComponents();
        Class<?> rawClass = ReflectionUtils.resolve(rawClassReference, rawClassName);
        boolean widenAccess = rawClass.isAnnotationPresent(Widen.class);
        Collection<TypedObject> typedObjects = new ArrayList<>(components.length);

        for (RecordComponent recordComponent : components) {
            try {
                Method accessor = recordComponent.getAccessor();
                if (widenAccess) {
                    if (!accessor.trySetAccessible()) {
                        throw new MapperException("Failed to widen record accessor");
                    }
                }

                typedObjects.add(
                    new TypedObject(recordComponent.getName(), Token.ofType(recordComponent.getGenericType()),
                        accessor.invoke(object)));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MapperException(e);
            }
        }

        return typedObjects;
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new LinkedConfigNode(sizeHint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull T buildObject(@Nullable T buildingObject, Object @NotNull [] args) {
        if (buildingObject != null) {
            throw new MapperException("Pre-initialized building objects are not supported by this signature");
        }

        try {
            return (T) resolveConstructor().newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean matchesArgumentNames() {
        return true;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public int length(@Nullable ConfigElement element) {
        return resolveInfo().types.size();
    }

    @Override
    public @NotNull Token<T> returnType() {
        return genericReturnType;
    }

    private Constructor<?> resolveConstructor() {
        Constructor<?> cached = constructorReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> rawClass = ReflectionUtils.resolve(rawClassReference, rawClassName);
        boolean widenAccess = rawClass.isAnnotationPresent(Widen.class);
        Class<?>[] types = resolveComponentTypes();

        try {
            if (widenAccess) {
                Constructor<?> constructor = rawClass.getDeclaredConstructor(types);
                if (!constructor.trySetAccessible()) {
                    throw new MapperException("Failed to widen access for record constructor '" + constructor + "'");
                }

                constructorReference = new SoftReference<>(constructor);
                return constructor;
            }

            Constructor<?> constructor = rawClass.getConstructor(types);
            constructorReference = new SoftReference<>(constructor);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new MapperException(e);
        }
    }

    private Class<?>[] resolveComponentTypes() {
        RecordComponent[] recordComponents = resolveComponents();
        Class<?>[] types = new Class[recordComponents.length];

        for (int i = 0; i < recordComponents.length; i++) {
            types[i] = recordComponents[i].getType();
        }

        return types;
    }

    private Info resolveInfo() {
        if (info != null) {
            return info;
        }

        RecordComponent[] recordComponents = resolveComponents();
        if (recordComponents.length == 0) {
            return info = new Info(List.of(), Map.of());
        }

        if (recordComponents.length == 1) {
            RecordComponent component = recordComponents[0];
            Type type = component.getGenericType();
            String name = component.getName();

            Map<String, Token<?>> varMappings;
            if (type instanceof TypeVariable<?> variable) {
                varMappings = Map.of(name, Token.ofType(variable));
            } else {
                varMappings = Map.of();
            }

            return info = new Info(List.of(Map.entry(name, SignatureParameter.parameter(Token.ofType(type)))), varMappings);
        }

        List<Map.Entry<String, SignatureParameter>> underlyingList = new ArrayList<>(recordComponents.length);
        Map<String, Token<?>> varMappings = new HashMap<>(recordComponents.length);
        for (RecordComponent component : recordComponents) {
            Type type = component.getGenericType();
            String name = component.getName();

            if (type instanceof TypeVariable<?> variable) {
                varMappings.put(name, Token.ofType(variable));
            }

            underlyingList.add(Map.entry(component.getName(), SignatureParameter.parameter(Token.ofType(type))));
        }

        return info = new Info(List.copyOf(underlyingList), Map.copyOf(varMappings));
    }

    private RecordComponent[] resolveComponents() {
        RecordComponent[] cached = recordComponentsReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> objectClass = ReflectionUtils.resolve(rawClassReference, rawClassName);
        RecordComponent[] recordComponents = objectClass.getRecordComponents();
        recordComponentsReference = new SoftReference<>(recordComponents);
        return recordComponents;
    }

    private record Info(Collection<Map.Entry<String, SignatureParameter>> types, Map<String, Token<?>> varMappings) {
    }
}
