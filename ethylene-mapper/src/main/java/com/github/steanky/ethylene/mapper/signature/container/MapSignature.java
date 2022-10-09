package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class MapSignature<T> extends ContainerSignatureBase<T> {
    public MapSignature(@NotNull Token<?> keyType, @NotNull Token<?> valueType, @NotNull Token<T> mapType) {
        super(Token.ofType(Map.Entry.class).parameterize(keyType, valueType), mapType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        Map<Object, Object> map = (Map<Object, Object>) object;

        return new AbstractCollection<>() {
            @Override
            public Iterator<TypedObject> iterator() {
                return new Iterator<>() {
                    private final Iterator<Map.Entry<Object, Object>> entryIterator = map.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    @Override
                    public TypedObject next() {
                        return new TypedObject(null, MapSignature.this.entry.getValue(), entryIterator.next());
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        if (buildingObject != null) {
            Map<Object, Object> buildingMap = (Map<Object, Object>) buildingObject;
            finishMap(buildingMap, args);
            return buildingMap;
        }

        Map<Object, Object> map = getMap(args.length);
        finishMap(map, args);
        return map;
    }

    @SuppressWarnings("unchecked")
    private void finishMap(Map<Object, Object> map, Object[] args) {
        for (Object object : args) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) object;
            map.put(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> getMap(int size) {
        try {
            ConstructorInfo constructorInfo = super.resolveConstructor();
            boolean parameterless = constructorInfo.parameterless();
            Constructor<?> constructor = constructorInfo.constructor();

            return parameterless ? (Map<Object, Object>) constructor.newInstance() :
                (Map<Object, Object>) constructor.newInstance(size);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }

    @Override
    protected @NotNull Object makeBuildingObject(@NotNull ConfigContainer container) {
        return getMap(container.entryCollection().size());
    }
}
