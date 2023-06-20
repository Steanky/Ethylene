package com.github.steanky.ethylene.mapper.signature.constructor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.annotation.Name;
import com.github.steanky.ethylene.mapper.annotation.Order;
import com.github.steanky.ethylene.mapper.annotation.Priority;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureParameter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/**
 * {@link Signature} that uses a constructor to create its objects. This does not support building objects.
 */
public class ConstructorSignature<T> extends PrioritizedBase implements Signature<T> {
    private static final Comparator<? super Field> COMPARATOR = Comparator.comparing(field -> {
        Order order = field.getAnnotation(Order.class);
        if (order != null) {
            return order.value();
        }

        return 0;
    });
    private final Token<T> genericReturnType;
    private final Reference<Class<?>> rawClassReference;
    private final String rawClassName;
    private final Reference<Class<?>>[] parameterTypes;
    private final String[] parameterTypeNames;
    //constructor objects are not retained by the classloader, so they might be garbage collected early
    //use soft reference to reduce the frequency of this occurrence, and resolve the actual constructor at runtime if it
    //is necessary to do so
    private Reference<Constructor<?>> constructorReference;
    private boolean matchesNames;
    private Info info;
    //similarly to constructors, fields are not tied to the classloader, keep a soft reference and be prepared to
    //re-create as necessary
    private Reference<Map<String, Field>> namedFieldsReference = new SoftReference<>(null);
    private Reference<Field[]> fieldsReference = new SoftReference<>(null);
    /**
     * Creates a new instance of this class.
     *
     * @param constructor       the {@link Constructor} used to create new objects and from which to obtain signature
     *                          information
     * @param genericReturnType the full generic type of the object created by the constructor
     */
    @SuppressWarnings("unchecked")
    public ConstructorSignature(@NotNull Constructor<?> constructor, @NotNull Token<T> genericReturnType) {
        super(computePriority(constructor));
        this.genericReturnType = Objects.requireNonNull(genericReturnType);
        this.constructorReference = new SoftReference<>(Objects.requireNonNull(constructor));

        Class<?> declaringClass = constructor.getDeclaringClass();
        this.rawClassReference = new WeakReference<>(declaringClass);
        this.rawClassName = declaringClass.getTypeName();

        Class<?>[] params = constructor.getParameterTypes();
        this.parameterTypes = new Reference[params.length];
        this.parameterTypeNames = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> referent = params[i];
            parameterTypes[i] = new WeakReference<>(referent);
            parameterTypeNames[i] = referent.getTypeName();
        }

        ReflectionUtils.validateNotAbstract(genericReturnType);
    }

    private static int computePriority(Constructor<?> constructor) {
        Priority priority = constructor.getAnnotation(Priority.class);
        if (priority == null) {
            return 0;
        }

        return priority.value();
    }

    private static Map.Entry<String, SignatureParameter> makeEntry(Parameter parameter, boolean parameterHasName,
        Map<String, ConfigElement> defaultMethodMap) {
        Name parameterName = parameter.getAnnotation(Name.class);
        Token<?> parameterType = Token.ofType(parameter.getParameterizedType());
        if (parameterName != null) {
            String name = parameterName.value();
            return Entry.of(name, SignatureParameter.parameter(parameterType,
                defaultMethodMap.get(name)));
        }

        String name = parameterHasName ? parameter.getName() : null;
        return Entry.of(name, SignatureParameter.parameter(parameterType, name == null ? null : defaultMethodMap.get(name)));
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, SignatureParameter>> argumentTypes() {
        return resolveInfo().typeCollection;
    }

    @Override
    public @NotNull @Unmodifiable Map<String, Token<?>> genericMappings() {
        return resolveInfo().varMappings;
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull T object) {
        Info info = resolveInfo();
        Collection<Map.Entry<String, SignatureParameter>> types = info.typeCollection;

        Class<?> declaringClass = ReflectionUtils.resolve(rawClassReference, rawClassName);
        boolean widenAccess = declaringClass.isAnnotationPresent(Widen.class);

        Field[] fields = initFields(declaringClass, widenAccess);

        int i = 0;
        Collection<TypedObject> typedObjects = new ArrayList<>(types.size());
        Map<String, Field> fieldMap = null;
        for (Map.Entry<String, SignatureParameter> typeEntry : types) {
            Field field;
            String name;
            if (matchesNames) {
                if (fieldMap == null) {
                    fieldMap = resolveNamedFields(fields);
                }

                field = fieldMap.get(name = typeEntry.getKey());
                if (field == null) {
                    break;
                }
            } else {
                if (i == fields.length) {
                    break;
                }

                field = fields[i++];
                name = ReflectionUtils.getFieldName(field);
            }

            if (widenAccess && !field.trySetAccessible()) {
                break;
            }

            try {
                typedObjects.add(
                    new TypedObject(name, Token.ofType(field.getGenericType()), FieldUtils.readField(field, object),
                        info.defaultValueMap.get(name)));
            } catch (IllegalAccessException ignored) {
                break;
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
            throw new MapperException("ConstructorSignature does not support pre-initialized building objects");
        }

        try {
            //it is the caller's responsibility to check argument length!
            return (T) resolveConstructor().newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean matchesArgumentNames() {
        //make sure the type collection is generated
        resolveInfo();
        return matchesNames;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public int length(@Nullable ConfigElement element) {
        return resolveConstructor().getParameterCount();
    }

    @Override
    public @NotNull Token<T> returnType() {
        return genericReturnType;
    }

    private Field[] initFields(Class<?> declaringClass, boolean widenAccess) {
        Field[] fields = fieldsReference.get();
        if (fields != null) {
            return fields;
        }

        fields = widenAccess ? declaringClass.getDeclaredFields() : declaringClass.getFields();

        if (matchesNames) {
            resolveNamedFields(fields);
        } else {
            Arrays.sort(fields, COMPARATOR);
        }

        fieldsReference = new SoftReference<>(fields);
        return fields;
    }

    private Map<String, Field> resolveNamedFields(Field[] fields) {
        Map<String, Field> fieldMap = namedFieldsReference.get();
        if (fieldMap != null) {
            return fieldMap;
        }

        fieldMap = new HashMap<>(fields.length);
        for (Field field : fields) {
            fieldMap.put(ReflectionUtils.getFieldName(field), field);
        }

        namedFieldsReference = new SoftReference<>(fieldMap);
        return fieldMap;
    }

    private Info resolveInfo() {
        if (info != null) {
            return info;
        }

        Constructor<?> constructor = resolveConstructor();
        if (constructor.getParameterCount() == 0) {
            //use empty list if we can
            return info = new Info(List.of(), Map.of(), Map.of());
        }

        Map<String, ConfigElement> map = ReflectionUtils.constructDefaultValueMap(constructor.getDeclaringClass());
        Parameter[] parameters = constructor.getParameters();
        if (parameters.length == 1) {
            //alternatively use singleton list
            Parameter first = parameters[0];

            Map.Entry<String, SignatureParameter> entry = makeEntry(first, first.isNamePresent(), map);
            matchesNames = entry.getKey() != null;

            Type type = first.getParameterizedType();

            Map<String, Token<?>> varMapping;
            if (entry.getKey() != null && type instanceof TypeVariable<?> variable) {
                varMapping = Map.of(entry.getKey(), Token.ofType(variable));
            } else {
                varMapping = Map.of();
            }

            return info = new Info(List.of(entry), varMapping, map);
        }

        //use a backing ArrayList for n > 1 length
        List<Map.Entry<String, SignatureParameter>> entryList = new ArrayList<>(parameters.length);
        Map<String, Token<?>> varMapping = new HashMap<>(parameters.length);

        Parameter first = parameters[0];

        boolean parameterHasName = first.isNamePresent();
        Map.Entry<String, SignatureParameter> firstEntry = makeEntry(first, parameterHasName, map);
        matchesNames = firstEntry.getKey() != null;

        Type firstType = first.getParameterizedType();
        if (firstEntry.getKey() != null && firstType instanceof TypeVariable<?> variable) {
            varMapping.put(firstEntry.getKey(), Token.ofType(variable));
        }

        entryList.add(firstEntry);

        boolean firstNonNullName = firstEntry.getKey() != null;
        for (int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            Map.Entry<String, SignatureParameter> entry = makeEntry(parameter, parameterHasName, map);
            if (firstNonNullName == (entry.getKey() == null)) {
                throw new MapperException("Inconsistent parameter naming");
            }

            Type type = parameter.getParameterizedType();
            if (entry.getKey() != null && type instanceof TypeVariable<?> variable) {
                varMapping.put(entry.getKey(), Token.ofType(variable));
            }

            entryList.add(entry);
        }

        return info = new Info(List.copyOf(entryList), Map.copyOf(varMapping), map);
    }

    private Constructor<?> resolveConstructor() {
        Constructor<?> constructor = constructorReference.get();
        if (constructor != null) {
            return constructor;
        }

        Class<?> declaringClass = ReflectionUtils.resolve(rawClassReference, rawClassName);
        try {
            constructor = declaringClass.getConstructor(resolveParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No valid constructor found for type '" + rawClassName + "'", e);
        }

        constructorReference = new SoftReference<>(constructor);
        return constructor;
    }

    private Class<?>[] resolveParameterTypes() {
        Class<?>[] parameterClasses = new Class[parameterTypes.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            Class<?> referent = parameterTypes[i].get();
            if (referent == null) {
                throw new IllegalStateException("Class named '" + parameterTypeNames[i] + "' no longer exists");
            }

            parameterClasses[i] = referent;
        }

        return parameterClasses;
    }

    private record Info(Collection<Map.Entry<String, SignatureParameter>> typeCollection, Map<String, Token<?>> varMappings,
                        Map<String, ConfigElement> defaultValueMap) {
    }
}
