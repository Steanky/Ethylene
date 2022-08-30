package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

public class ConstructorTypeFactory extends TypeFactoryBase {
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

        outer:
        for (Signature signature : signatures) {
            SignatureElement[] elements = signature.elements();

            if (elements.length == entries.size()) {
                if (matchParameterNames || matchParameterTypeHints) {
                    Map<Object, ConfigElement> identifiers = null;
                    if (matchParameterNames) {
                        identifiers = new LinkedHashMap<>(elements.length);
                        for (ConfigEntry entry : entries) {
                            identifiers.put(entry.getFirst(), entry.getSecond());
                        }

                        for (SignatureElement element : elements) {
                            if (!identifiers.containsKey(element.identifier())) {
                                break outer;
                            }
                        }
                    }

                    if (matchParameterTypeHints) {
                        if (!matchParameterNames) {
                            Iterator<ConfigEntry> iterator = entries.iterator();
                            for (SignatureElement element : elements) {
                                if (!typeHinter.getHint(element.type()).compatible(iterator.next().getSecond())) {
                                    break outer;
                                }
                            }

                            continue;
                        }

                        for (SignatureElement element : elements) {
                            ConfigElement configElement = identifiers.get(element.identifier());
                            if (configElement == null || !typeHinter.getHint(element.type())
                                    .compatible(configElement)) {
                                break outer;
                            }
                        }
                    }
                }

                return signature;
            }
        }

        throw new MapperException("unable to find constructor to make '" + owner + "' from " + providedElement);
    }

    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull ConfigElement providedElement,
            @NotNull Object... objects) {
        try {
            //if matching names, we have to re-order the object array
            if (matchParameterNames) {
                SignatureElement[] signatureElements = signature.elements();
                Collection<ConfigEntry> entryCollection = providedElement.asContainer().entryCollection();

                validateLengths(signatureElements.length, entryCollection.size(), objects.length);

                Map<Object, Object> elementMap = new HashMap<>(entryCollection.size());
                Iterator<ConfigEntry> configEntryIterator = entryCollection.iterator();
                for (Object object : objects) {
                    elementMap.put(configEntryIterator.next().getFirst(), object);
                }

                Object[] newArgs = new Object[signatureElements.length];
                for (int i = 0; i < signatureElements.length; i++) {
                    newArgs[i] = elementMap.get(signatureElements[i].identifier());
                }

                return constructors.get(signature.index()).newInstance(newArgs);
            }

            //no need to reorder
            return constructors.get(signature.index()).newInstance(objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MapperException(e);
        }
    }
}
