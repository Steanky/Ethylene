package com.github.steanky.ethylene.mapper.signature.field;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.annotation.Exclude;
import com.github.steanky.ethylene.mapper.annotation.Include;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * {@link Signature} based on field assignment. This signature supports building objects. Fields may be included and
 * excluded using {@link Include} and {@link Exclude}. Member access will be widened if the type itself is annotated
 * with {@link Widen}.
 *
 * @param <T> the actual type of the object this signature creates
 */
public class FieldSignature<T> extends PrioritizedBase implements Signature<T> {
    private final Token<T> genericReturnType;

    private final Reference<Class<?>> rawTypeReference;
    private final String rawTypeName;

    //fields are lazily initialized by initTypes
    private Reference<SignatureData> signatureDataReference = new SoftReference<>(null);

    private Collection<Map.Entry<String, Token<?>>> types;

    /**
     * Creates a new instance of this class.
     *
     * @param genericReturnType the actual type returned by this signature
     */
    public FieldSignature(@NotNull Token<T> genericReturnType) {
        super(0);
        this.genericReturnType = Objects.requireNonNull(genericReturnType);

        Class<?> rawType = genericReturnType.rawType();
        this.rawTypeReference = new WeakReference<>(rawType);
        this.rawTypeName = rawType.getName();
    }

    private static Constructor<?> getConstructor(Class<?> rawClass, boolean widenAccess) {
        try {
            if (widenAccess) {
                Constructor<?> constructor = rawClass.getDeclaredConstructor();

                if (!constructor.trySetAccessible()) {
                    throw new MapperException("Failed to widen constructor access for '" + constructor + "'");
                }

                return constructor;
            }

            return rawClass.getConstructor();
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
            throw new MapperException("Class '" + cls + "' is annotated with both @Exclude and @Include");
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
                throw new MapperException("Field '" + field + "' annotated with both @Exclude and @Include");
            }

            if (defaultExclude) {
                if (!includePresent) {
                    //exclude fields by default, require @Include annotation
                    continue;
                }
            } else if (excludePresent) {
                //include fields by default, require @Exclude
                continue;
            } else if (!widenAccess && (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers))) {
                //if not widening access:
                //if neither Include nor Exclude is present, assign only public non-final variables
                //if widening access: try to widen everything by default
                continue;
            }

            if (widenAccess && !field.trySetAccessible()) {
                throw new MapperException("Failed to widen access for field '" + field + "'");
            }

            participatingFields.add(field);
        }

        participatingFields.trimToSize();
        return participatingFields;
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, Token<?>>> argumentTypes() {
        return resolveTypes();
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull T object) {
        SignatureData data = resolveData();

        Collection<TypedObject> typedObjects = new ArrayList<>(data.fields.size());
        for (Field field : data.fields) {
            String name = ReflectionUtils.getFieldName(field);

            try {
                typedObjects.add(
                    new TypedObject(name, Token.ofType(field.getGenericType()), FieldUtils.readField(field, object)));
            } catch (IllegalAccessException ignored) {
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
        return true;
    }

    @Override
    public @NotNull T initBuildingObject(@NotNull ConfigElement element) {
        return getBuildingObject();
    }

    @Override
    public @NotNull T buildObject(@Nullable T buildingObject, Object @NotNull [] args) {
        try {
            if (buildingObject != null) {
                finishObject(buildingObject, args);
                return buildingObject;
            }

            T object = getBuildingObject();
            finishObject(object, args);
            return object;
        } catch (IllegalAccessException e) {
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
        return resolveTypes().size();
    }

    @Override
    public @NotNull Token<T> returnType() {
        return genericReturnType;
    }

    private Collection<Map.Entry<String, Token<?>>> resolveTypes() {
        if (types != null) {
            return types;
        }

        SignatureData data = resolveData();
        if (data.fields.isEmpty()) {
            return types = List.of();
        }

        if (data.fields.size() == 1) {
            Field first = data.fields.get(0);
            return types = List.of(Entry.of(ReflectionUtils.getFieldName(first), Token.ofType(first.getGenericType())));
        }

        Collection<Map.Entry<String, Token<?>>> typeCollection = new ArrayList<>(data.fields.size());
        for (Field field : data.fields) {
            typeCollection.add(Entry.of(ReflectionUtils.getFieldName(field), Token.ofType(field.getGenericType())));
        }

        return types = Collections.unmodifiableCollection(typeCollection);
    }

    private SignatureData resolveData() {
        SignatureData cached = signatureDataReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> rawClass = ReflectionUtils.resolve(rawTypeReference, rawTypeName);
        boolean widenAccess = rawClass.isAnnotationPresent(Widen.class);
        Constructor<?> constructor = getConstructor(rawClass, widenAccess);
        List<Field> participatingFields = getFields(rawClass, widenAccess);

        cached = new SignatureData(constructor, participatingFields);
        signatureDataReference = new SoftReference<>(cached);
        return cached;
    }

    @SuppressWarnings("unchecked")
    private T getBuildingObject() {
        SignatureData data = resolveData();

        try {
            return (T) data.constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    private void finishObject(Object buildingObject, Object[] args) throws IllegalAccessException {
        SignatureData data = resolveData();
        for (int i = 0; i < args.length; i++) {
            data.fields.get(i).set(buildingObject, args[i]);
        }
    }

    private record SignatureData(Constructor<?> constructor, List<Field> fields) {
    }
}
