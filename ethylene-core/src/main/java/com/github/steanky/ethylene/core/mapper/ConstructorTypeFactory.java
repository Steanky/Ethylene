package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

public class ConstructorTypeFactory implements TypeFactory {
    private final Class<?> owner;
    private final TypeHinter typeHinter;
    private final ArrayList<Constructor<?>> constructors;
    private final Signature[] signatures;
    private final boolean matchParameterNames;
    private final boolean matchParameterTypeHints;

    public ConstructorTypeFactory(@NotNull Class<?> owner, @NotNull TypeHinter typeHinter, boolean matchParameterNames,
            boolean matchParameterTypeHints) {
        this.owner = Objects.requireNonNull(owner);
        this.typeHinter = Objects.requireNonNull(typeHinter);

        Constructor<?>[] allConstructors = owner.getConstructors();
        this.constructors = new ArrayList<>(allConstructors.length);
        for (Constructor<?> constructor : allConstructors) {
            if (constructor.canAccess(null)) {
                this.constructors.add(constructor);
            }
        }

        this.constructors.trimToSize();
        this.signatures = new Signature[constructors.size()];
        this.matchParameterNames = matchParameterNames;
        this.matchParameterTypeHints = matchParameterTypeHints;

        for (int i = 0; i < constructors.size(); i++) {
            Constructor<?> constructor = constructors.get(i);
            Parameter[] constructorParameters = constructor.getParameters();
            SignatureElement[] signatureElements = new SignatureElement[constructorParameters.length];

            for (int j = 0; j < constructorParameters.length; j++) {
                Parameter constructorParameter = constructorParameters[j];
                signatureElements[j] = new SignatureElement(constructorParameter.getParameterizedType(),
                        constructorParameter.getName());
            }

            signatures[i] = new Signature(i, signatureElements);
        }
    }

    @Override
    public @NotNull Signature signature(@NotNull ConfigElement providedElement) {
        if (!providedElement.isContainer()) {
            throw new MapperException("container element required to construct objects of type '" + owner + "'");
        }

        ConfigContainer configContainer = providedElement.asContainer();
        Collection<ConfigEntry> entries = configContainer.entryCollection();
        for (Signature signature : signatures) {
            SignatureElement[] elements = signature.elements();

            if (elements.length == entries.size()) {
                if (matchParameterNames || matchParameterTypeHints) {
                    Iterator<ConfigEntry> entryIterator = entries.iterator();
                    for (int i = 0; i < entries.size(); i++) {
                        SignatureElement element = elements[i];
                        ConfigEntry entry = entryIterator.next();

                        if (matchParameterNames) {
                            if (!Objects.equals(element.identifier(), entry.getFirst())) {
                                break;
                            }
                        }

                        if (matchParameterTypeHints) {
                            TypeHinter.Hint typeHint = typeHinter.getHint(element.type());
                            if (!typeHint.compatible(entry.getSecond())) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        throw new MapperException("unable to find constructor to make '" + owner + "' from " + providedElement);
    }

    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull Object... objects) {
        try {
            return constructors.get(signature.index()).newInstance(objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }
}
