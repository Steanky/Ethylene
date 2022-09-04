package com.github.steanky.ethylene.core.mapper.signature.constructor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

public class ConstructorSignature implements Signature {
    private final Constructor<?> constructor;

    private boolean matchesNames;
    private Collection<Entry<String, Type>> types;

    public ConstructorSignature(@NotNull Constructor<?> constructor) {
        this.constructor = Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return getTypeCollection();
    }

    @Override
    public Object makeObject(@NotNull Object[] args) {
        try {
            //it is the caller's responsibility to check argument length!
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }

    @Override
    public boolean hasArgumentNames() {
        //make sure the type collection is generated
        getTypeCollection();

        return matchesNames;
    }

    @Override
    public int length(@NotNull ConfigElement element) {
        return constructor.getParameterCount();
    }

    @Override
    public ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return constructor.getDeclaringClass();
    }

    private Collection<Entry<String, Type>> getTypeCollection() {
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
            return types = Collections.singleton(makeEntry(first, matchesNames = first.isNamePresent()));
        }

        //use a backing ArrayList for n > 1 length
        List<Entry<String, Type>> entryList = new ArrayList<>(parameters.length);

        Parameter first = parameters[0];
        boolean hasName = matchesNames = first.isNamePresent();

        entryList.add(makeEntry(first, hasName));
        for (int i = 1; i < parameters.length; i++) {
            entryList.add(makeEntry(parameters[i], hasName));
        }

        return types = Collections.unmodifiableCollection(entryList);
    }

    private static Entry<String, Type> makeEntry(Parameter parameter, boolean hasName) {
        return Entry.of(hasName ? parameter.getName() : null, parameter.getParameterizedType());
    }
}
