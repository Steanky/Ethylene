package com.github.steanky.ethylene.core.mapper.signature.field;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.annotation.Exclude;
import com.github.steanky.ethylene.core.mapper.annotation.Include;
import com.github.steanky.ethylene.core.mapper.annotation.Name;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.*;

public class FieldSignature implements Signature {
    private final Type type;
    private final Class<?> rawType;
    private final boolean widenAccess;

    private final Constructor<?> parameterlessConstructor;

    private List<Field> participatingFields;
    private Collection<Entry<String, Type>> types;

    public FieldSignature(@NotNull Type type, boolean widenAccess) {
        this.type = Objects.requireNonNull(type);
        this.widenAccess = widenAccess;

        this.rawType = TypeUtils.getRawType(type, null);
        this.parameterlessConstructor = getConstructor(rawType, widenAccess);
    }

    private static Constructor<?> getConstructor(Class<?> cls, boolean widenAccess) {
        try {
            if (widenAccess) {
                Constructor<?> constructor = cls.getConstructor();
                if (!constructor.trySetAccessible()) {
                    throw new MapperException("failed to widen constructor access " + constructor);
                }

                return constructor;
            }

            return cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new MapperException(e);
        }
    }

    private static List<Field> getFields(Class<?> cls, boolean widenAccess) {
        Field[] fields = cls.getDeclaredFields();
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
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            boolean includePresent = field.isAnnotationPresent(Include.class);
            boolean excludePresent = field.isAnnotationPresent(Exclude.class);
            if (includePresent && excludePresent) {
                throw new MapperException("field '" + field + "' annotated with both @Exclude and @Include");
            }

            if (defaultExclude) {
                if (!field.isAnnotationPresent(Include.class)) {
                    continue;
                }
            }
            else if (field.isAnnotationPresent(Exclude.class)) {
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

    private Collection<Entry<String, Type>> getTypes() {
        if (types != null) {
            return types;
        }

        participatingFields = getFields(rawType, widenAccess);
        if (participatingFields.isEmpty()) {
            return types = Collections.emptyList();
        }

        Collection<Entry<String, Type>> typeCollection = new ArrayList<>(participatingFields.size());
        for (Field field : participatingFields) {
            Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
            String name = nameAnnotation == null ? field.getName() : nameAnnotation.value();
            typeCollection.add(Entry.of(name, field.getGenericType()));
        }

        return types = Collections.unmodifiableCollection(typeCollection);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return getTypes();
    }

    @Override
    public Object makeObject(@NotNull Object[] args) {
        try {
            Object object = parameterlessConstructor.newInstance();
            for (int i = 0; i < args.length; i++) {
                participatingFields.get(i).set(object, args[i]);
            }

            return object;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean hasArgumentNames() {
        return true;
    }

    @Override
    public int length(@NotNull ConfigElement element) {
        return getTypes().size();
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return type;
    }
}
