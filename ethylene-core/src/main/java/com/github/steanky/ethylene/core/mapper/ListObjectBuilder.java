package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListObjectBuilder implements ObjectBuilder {
    private final List<ObjectBuilder> list;
    private final boolean outputArray;

    private List<Object> currentObject;
    private boolean isBuilding;

    public ListObjectBuilder(int size, boolean outputArray) {
        this.list = new ArrayList<>(size);
        this.outputArray = outputArray;
    }

    @Override
    public void appendParameter(@NotNull ObjectBuilder parameter) {
        list.add(parameter);
    }

    @Override
    public Object build() {
        currentObject = new ArrayList<>(list.size());

        isBuilding = true;
        for(ObjectBuilder builder : list) {
            currentObject.add(builder.buildOrGetCurrent());
        }
        isBuilding = false;

        return outputArray ? currentObject.toArray() : currentObject;
    }

    @Override
    public Object getCurrentObject() {
        return currentObject;
    }

    @Override
    public Type @NotNull [] getArgumentTypes() {
        return new Type[0];
    }

    @Override
    public boolean isBuilding() {
        return isBuilding;
    }

    @Override
    public @NotNull TypeHinter.TypeHint typeHint() {
        return TypeHinter.TypeHint.LIST_LIKE;
    }
}
