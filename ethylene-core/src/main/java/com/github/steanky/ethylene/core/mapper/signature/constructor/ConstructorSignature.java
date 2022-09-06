package com.github.steanky.ethylene.core.mapper.signature.constructor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.annotation.Name;
import com.github.steanky.ethylene.core.mapper.annotation.Order;
import com.github.steanky.ethylene.core.mapper.annotation.Widen;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public class ConstructorSignature implements Signature {
    private final Constructor<?> constructor;
    private final Type genericReturnType;

    private boolean matchesNames;
    private Collection<Entry<String, Type>> types;

    private Field[] fields;
    private Map<String, Field> namedFields;

    public ConstructorSignature(@NotNull Constructor<?> constructor, @NotNull Type genericReturnType) {
        this.constructor = Objects.requireNonNull(constructor);
        this.genericReturnType = Objects.requireNonNull(genericReturnType);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return initTypeCollection();
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        initTypeCollection();

        Collection<TypedObject> typedObjects = new ArrayList<>(types.size());
        Class<?> declaringClass = constructor.getDeclaringClass();
        boolean widenAccess = declaringClass.isAnnotationPresent(Widen.class);

        initFields(declaringClass, widenAccess);

        int i = 0;
        for (Entry<String, Type> typeEntry : types) {
            Field field;
            String name;
            if (matchesNames) {
                field = namedFields.get(name = typeEntry.getKey());
                if (field == null) {
                    break;
                }
            }
            else {
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
                typedObjects.add(new TypedObject(name, field.getGenericType(), FieldUtils.readField(field, object)));
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

    @Override
    public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        if (buildingObject != null) {
            throw new MapperException("ConstructorSignature does not support pre-initialized building objects");
        }

        try {
            //it is the caller's responsibility to check argument length!
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean matchesArgumentNames() {
        //make sure the type collection is generated
        initTypeCollection();
        return matchesNames;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public int length(@Nullable ConfigElement element) {
        return constructor.getParameterCount();
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return genericReturnType;
    }

    private void initFields(Class<?> declaringClass, boolean widenAccess) {
        if (fields != null) {
            return;
        }

        fields = widenAccess ? declaringClass.getDeclaredFields() : declaringClass.getFields();
        if (matchesNames) {
            namedFields = new HashMap<>(fields.length);
            for (Field field : fields) {
                namedFields.put(ReflectionUtils.getFieldName(field), field);
            }
        }
        else {
            Arrays.sort(fields, Comparator.comparing(field -> {
                Order order = field.getAnnotation(Order.class);
                if (order != null) {
                    return order.value();
                }

                return 0;
            }));
        }
    }

    private Collection<Entry<String, Type>> initTypeCollection() {
        if (types != null) {
            return types;
        }

        if (constructor.getParameterCount() == 0) {
            //use empty list if we can
            return types = Collections.emptyList();
        }

        Parameter[] parameters = constructor.getParameters();
        if (parameters.length == 1) {
            //alternatively use singleton list
            Parameter first = parameters[0];
            Entry<String, Type> entry = makeEntry(first, first.isNamePresent());
            matchesNames = entry.getFirst() != null;
            return types = Collections.singleton(entry);
        }

        //use a backing ArrayList for n > 1 length
        List<Entry<String, Type>> entryList = new ArrayList<>(parameters.length);

        Parameter first = parameters[0];

        boolean parameterHasName = first.isNamePresent();
        Entry<String, Type> firstEntry = makeEntry(first, parameterHasName);
        matchesNames = firstEntry.getFirst() != null;

        entryList.add(firstEntry);

        boolean firstNonNullName = firstEntry.getFirst() != null;
        for (int i = 1; i < parameters.length; i++) {
            Entry<String, Type> entry = makeEntry(parameters[i], parameterHasName);
            if (firstNonNullName == (entry.getFirst() == null)) {
                throw new MapperException("inconsistent parameter naming");
            }

            entryList.add(entry);
        }

        return types = Collections.unmodifiableCollection(entryList);
    }

    private static Entry<String, Type> makeEntry(Parameter parameter, boolean parameterHasName) {
        Name parameterName = parameter.getAnnotation(Name.class);
        return Entry.of(parameterHasName ? parameter.getName() : (parameterName != null ? parameterName.value() : null),
                parameter.getParameterizedType());
    }
}
