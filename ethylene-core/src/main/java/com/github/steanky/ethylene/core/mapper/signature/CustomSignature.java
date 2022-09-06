package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CustomSignature implements Signature {
    private final Collection<Entry<String, Type>> namedTypes;
    private final Type returnType;
    private final boolean hasNames;
    private final Function<? super Object[], ?> creatorFunction;

    public CustomSignature(@NotNull Collection<Entry<String, Type>> namedArgumentTypes, @NotNull Type returnType,
            boolean hasNames, @NotNull Function<? super Object[], ?> creatorFunction) {
        this.namedTypes = List.copyOf(namedArgumentTypes);
        this.returnType = Objects.requireNonNull(returnType);
        this.hasNames = hasNames;
        this.creatorFunction = Objects.requireNonNull(creatorFunction);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return namedTypes;
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return returnType;
    }

    @Override
    public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        return creatorFunction.apply(args);
    }

    @Override
    public boolean matchesArgumentNames() {
        return hasNames;
    }

    @Override
    public int length(@Nullable ConfigElement element) {
        return namedTypes.size();
    }
}
