package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapObjectBuilder implements ObjectBuilder {
    private final Collection<ObjectBuilder> entries;
    private Map<Object, Object> currentObject;
    private boolean isBuilding;

    public MapObjectBuilder(int initialSize) {
        this.entries = new ArrayList<>(initialSize);
    }

    @Override
    public void appendParameter(@NotNull ObjectBuilder parameter) {
        entries.add(parameter);
    }

    @Override
    public Object build() {
        currentObject = new HashMap<>(entries.size());

        isBuilding = true;
        for(ObjectBuilder builder : entries) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) builder.buildOrGetCurrent();

            ObjectBuilder keyBuilder = (ObjectBuilder) entry.getKey();
            ObjectBuilder valueBuilder = (ObjectBuilder) entry.getValue();

            Object keyObject = keyBuilder.buildOrGetCurrent();
            Object valueObject = valueBuilder.buildOrGetCurrent();

            currentObject.put(keyObject, valueObject);
        }
        isBuilding = false;

        return currentObject;
    }

    @Override
    public Object getCurrentObject() {
        return currentObject;
    }

    @Override
    public Type @NotNull [] getArgumentTypes() {
        throw new IllegalStateException("No valid argument types for MapObjectBuilder");
    }

    @Override
    public boolean isBuilding() {
        return isBuilding;
    }

    @Override
    public @NotNull TypeHinter.TypeHint typeHint() {
        return TypeHinter.TypeHint.MAP_LIKE;
    }
}
