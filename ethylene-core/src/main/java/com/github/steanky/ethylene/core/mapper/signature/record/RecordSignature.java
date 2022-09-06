package com.github.steanky.ethylene.core.mapper.signature.record;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.annotation.Widen;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;

public class RecordSignature implements Signature {
    private final Type returnType;
    private final Class<?> raw;

    //fields are lazily initialized by getArgumentTypes()
    private int length;
    private Constructor<?> canonicalConstructor;
    private Collection<Entry<String, Type>> argumentTypes;

    public RecordSignature(@NotNull Type returnType) {
        this.returnType = Objects.requireNonNull(returnType);
        this.raw = TypeUtils.getRawType(returnType, null);

        if (!raw.isRecord()) {
            throw new MapperException("expected record: " + returnType);
        }
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return initArgumentTypes();
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        initArgumentTypes();

        Collection<TypedObject> typedObjects = new ArrayList<>(length);
        RecordComponent[] recordComponents = object.getClass().getRecordComponents();
        for (RecordComponent recordComponent : recordComponents) {
            try {
                typedObjects.add(new TypedObject(recordComponent.getName(), recordComponent.getGenericType(),
                        recordComponent.getAccessor().invoke(object)));
            } catch (IllegalAccessException | InvocationTargetException ignored) {}
        }

        return typedObjects;
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new LinkedConfigNode(sizeHint);
    }

    @Override
    public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        if (buildingObject != null) {
            throw new MapperException("pre-initialized building objects are not supported");
        }

        initArgumentTypes();
        try {
            return canonicalConstructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean matchesArgumentNames() {
        return true;
    }

    @Override
    public boolean hasBuildingObject() {
        return false;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public int length(@Nullable ConfigElement element) {
        initArgumentTypes();
        return length;
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return returnType;
    }

    private Collection<Entry<String, Type>> initArgumentTypes() {
        if (argumentTypes != null) {
            return argumentTypes;
        }

        boolean widenAccess = raw.isAnnotationPresent(Widen.class);

        RecordComponent[] recordComponents = raw.getRecordComponents();
        if (recordComponents.length == 0) {
            canonicalConstructor = getConstructor(widenAccess);
            return argumentTypes = Collections.emptyList();
        }

        if (recordComponents.length == 1) {
            RecordComponent component = recordComponents[0];

            length = 1;
            canonicalConstructor = getConstructor(widenAccess, component.getType());
            return argumentTypes = Collections.singleton(Entry.of(component.getName(), component.getGenericType()));
        }

        List<Entry<String, Type>> underlyingList = new ArrayList<>(recordComponents.length);
        Class<?>[] types = new Class[recordComponents.length];

        int i = 0;
        for (RecordComponent component : recordComponents) {
            types[i++] = component.getType();
            underlyingList.add(Entry.of(component.getName(), component.getGenericType()));
        }

        length = recordComponents.length;
        canonicalConstructor = getConstructor(widenAccess, types);
        return argumentTypes = Collections.unmodifiableCollection(underlyingList);
    }

    private Constructor<?> getConstructor(boolean widenAccess, Class<?> ... types) {
        try {
            if (widenAccess) {
                Constructor<?> constructor = raw.getDeclaredConstructor(types);
                if (!constructor.canAccess(null) && !constructor.trySetAccessible()) {
                    throw new MapperException("failed to widen access for record constructor " + constructor);
                }
            }

            return raw.getConstructor(types);
        } catch (NoSuchMethodException e) {
            throw new MapperException(e);
        }
    }
}
