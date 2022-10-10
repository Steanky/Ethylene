package com.github.steanky.ethylene.mapper.signature.record;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.*;

public class RecordSignature<T> extends PrioritizedBase implements Signature<T> {
    private final Token<T> genericReturnType;

    private final Reference<Class<?>> rawClassReference;
    private final String rawClassName;

    //cache constructor and RecordComponent array, be prepared to re-generate them if necessary since they can get
    //garbage-collected
    private Reference<Constructor<?>> constructorReference = new SoftReference<>(null);
    private Reference<RecordComponent[]> recordComponentsReference = new SoftReference<>(null);

    //safe, does not actually retain strong references to Type objects
    private Collection<Map.Entry<String, Token<?>>> argumentTypes;

    public RecordSignature(@NotNull Token<T> genericReturnType) {
        super(0);
        this.genericReturnType = Objects.requireNonNull(genericReturnType);

        Class<?> rawClass = genericReturnType.rawType();
        if (!rawClass.isRecord()) {
            throw new MapperException("'" + rawClass + "' is not a record");
        }

        this.rawClassReference = new WeakReference<>(rawClass);
        this.rawClassName = rawClass.getName();
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, Token<?>>> argumentTypes() {
        return resolveArgumentTypes();
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull T object) {
        RecordComponent[] components = resolveComponents();
        Collection<TypedObject> typedObjects = new ArrayList<>(components.length);

        for (RecordComponent recordComponent : components) {
            try {
                typedObjects.add(
                    new TypedObject(recordComponent.getName(), Token.ofType(recordComponent.getGenericType()),
                        recordComponent.getAccessor().invoke(object)));
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        return typedObjects;
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new LinkedConfigNode(sizeHint);
    }

    @Override
    public boolean hasBuildingObject() {
        return false;
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
        return resolveArgumentTypes().size();
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

    private Collection<Map.Entry<String, Token<?>>> resolveArgumentTypes() {
        if (argumentTypes != null) {
            return argumentTypes;
        }

        RecordComponent[] recordComponents = resolveComponents();
        if (recordComponents.length == 0) {
            return argumentTypes = List.of();
        }

        if (recordComponents.length == 1) {
            RecordComponent component = recordComponents[0];
            return argumentTypes = List.of(Map.entry(component.getName(), Token.ofType(component.getGenericType())));
        }

        List<Map.Entry<String, Token<?>>> underlyingList = new ArrayList<>(recordComponents.length);
        for (RecordComponent component : recordComponents) {
            underlyingList.add(Map.entry(component.getName(), Token.ofType(component.getGenericType())));
        }

        return argumentTypes = Collections.unmodifiableCollection(underlyingList);
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
}
