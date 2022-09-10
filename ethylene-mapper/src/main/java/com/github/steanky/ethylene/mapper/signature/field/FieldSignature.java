package com.github.steanky.ethylene.mapper.signature.field;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.annotation.Exclude;
import com.github.steanky.ethylene.mapper.annotation.Include;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public class FieldSignature implements Signature {
    private final Type type;
    private final Class<?> rawType;

    //fields are lazily initialized by initTypes
    private Constructor<?> parameterlessConstructor;
    private List<Field> participatingFields;
    private Collection<Entry<String, Type>> types;

    public FieldSignature(@NotNull Type type) {
        this.type = Objects.requireNonNull(type);
        this.rawType = ReflectionUtils.rawType(type);
    }

    private static Constructor<?> getConstructor(Class<?> cls, boolean widenAccess) {
        try {
            if (widenAccess) {
                Constructor<?> constructor = cls.getDeclaredConstructor();
                if (!constructor.trySetAccessible()) {
                    throw new MapperException("failed to widen constructor access " + constructor);
                }

                return constructor;
            }

            return cls.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MapperException(e);
        }
    }

    private static List<Field> getFields(Class<?> cls, boolean widenAccess) {
        Field[] fields = widenAccess ? cls.getDeclaredFields() : cls.getFields();
        ArrayList<Field> participatingFields = new ArrayList<>(fields.length);

        boolean defaultExclude = cls.isAnnotationPresent(Exclude.class);
        boolean defaultInclude = cls.isAnnotationPresent(Include.class);
        if (defaultExclude && defaultInclude) {
            throw new MapperException("class '" + cls + "' annotated with both @Exclude and @Include");
        }

        if (!defaultExclude && !defaultInclude) {
            defaultExclude = true;
        }

        for (Field field : fields) {
            if (!field.getDeclaringClass().equals(cls)) {
                continue;
            }

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                continue;
            }

            boolean includePresent = field.isAnnotationPresent(Include.class);
            boolean excludePresent = field.isAnnotationPresent(Exclude.class);
            if (includePresent && excludePresent) {
                throw new MapperException("field '" + field + "' annotated with both @Exclude and @Include");
            }

            if (defaultExclude) {
                if (!field.isAnnotationPresent(Include.class)) {
                    //exclude fields by default, require @Include annotation
                    continue;
                }
            }
            else if (field.isAnnotationPresent(Exclude.class)) {
                //include fields by default, require @Exclude
                continue;
            }
            else if (!widenAccess && (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers))) {
                //if not widening access:
                //if neither Include nor Exclude is present, assign only public non-final variables
                //if widening access: try to widen everything by default
                continue;
            }

            if (widenAccess && !field.trySetAccessible()) {
                throw new MapperException("failed to widen access for field " + field);
            }

            participatingFields.add(field);
        }

        participatingFields.trimToSize();
        return participatingFields;
    }

    private Collection<Entry<String, Type>> initTypes() {
        if (types != null) {
            return types;
        }

        boolean widenAccess = rawType.isAnnotationPresent(Widen.class);
        this.parameterlessConstructor = getConstructor(rawType, widenAccess);
        this.participatingFields = getFields(rawType, widenAccess);

        if (this.participatingFields.isEmpty()) {
            return types = Collections.emptyList();
        }

        Collection<Entry<String, Type>> typeCollection = new ArrayList<>(participatingFields.size());
        for (Field field : participatingFields) {
            typeCollection.add(Entry.of(ReflectionUtils.getFieldName(field), field.getGenericType()));
        }

        return types = Collections.unmodifiableCollection(typeCollection);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return initTypes();
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        initTypes();
        Collection<TypedObject> typedObjects = new ArrayList<>(participatingFields.size());
        for (Field field : participatingFields) {
            String name = ReflectionUtils.getFieldName(field);

            try {
                typedObjects.add(new TypedObject(name, field.getGenericType(), FieldUtils.readField(field, object)));
            }
            catch (IllegalAccessException ignored) {}
        }

        return typedObjects;
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new LinkedConfigNode(sizeHint);
    }

    @Override
    public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        try {
            initTypes();
            if (buildingObject != null) {
                finishObject(buildingObject, args);
                return buildingObject;
            }

            Object object = getBuildingObject();
            finishObject(object, args);
            return object;
        } catch (IllegalAccessException e) {
            throw new MapperException(e);
        }
    }

    private void finishObject(Object buildingObject, Object[] args) throws IllegalAccessException {
        for (int i = 0; i < args.length; i++) {
            participatingFields.get(i).set(buildingObject, args[i]);
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
        return initTypes().size();
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return type;
    }

    @Override
    public boolean hasBuildingObject() {
        return true;
    }

    @Override
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        return getBuildingObject();
    }

    private Object getBuildingObject() {
        try {
            return parameterlessConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }
}
