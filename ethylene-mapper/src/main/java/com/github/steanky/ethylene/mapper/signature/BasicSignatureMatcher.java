package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.TypeHinter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BasicSignatureMatcher implements SignatureMatcher {
    private final Signature[] signatures;
    private final TypeHinter typeHinter;

    public BasicSignatureMatcher(@NotNull Signature @NotNull [] signatures, @NotNull TypeHinter typeHinter) {
        Signature[] copy = new Signature[signatures.length];
        System.arraycopy(signatures, 0, copy, 0, signatures.length);
        Arrays.sort(copy, Comparator.comparing(Signature::priority).reversed());
        this.signatures = copy;
        this.typeHinter = Objects.requireNonNull(typeHinter);
    }

    private MatchingSignature matchingFromObject(Signature signature, Object providedObject) {
        Objects.requireNonNull(providedObject);

        Collection<Signature.TypedObject> objectData = signature.objectData(providedObject);

        int length = signature.length(null);
        if (length > -1 && length != objectData.size()) {
            return null;
        }

        length = objectData.size();
        boolean matchNames = signature.matchesArgumentNames();
        boolean matchTypeHints = signature.matchesTypeHints();

        if (!(matchNames || matchTypeHints)) {
            return new MatchingSignature(signature, null, objectData, length);
        }

        Collection<Signature.TypedObject> typeCollection;
        if (matchNames) {
            typeCollection = new ArrayList<>(objectData.size());
            Map<String, Signature.TypedObject> objectDataMap = new HashMap<>(objectData.size());
            for (Signature.TypedObject entry : objectData) {
                objectDataMap.put(entry.name(), entry);
            }

            Iterable<Map.Entry<String, Token<?>>> signatureTypes = signature.argumentTypes();
            for (Map.Entry<String, Token<?>> entry : signatureTypes) {
                Signature.TypedObject typedObject = objectDataMap.get(entry.getKey());
                if (typedObject == null) {
                    return null;
                }

                typeCollection.add(typedObject);
            }
        } else {
            typeCollection = objectData;
        }

        if (matchTypeHints) {
            Iterator<Signature.TypedObject> typeCollectionIterator = typeCollection.iterator();
            Iterator<Map.Entry<String, Token<?>>> signatureIterator = signature.argumentTypes().iterator();

            while (typeCollectionIterator.hasNext()) {
                if (typeHinter.getHint(typeCollectionIterator.next().type()) !=
                    typeHinter.getHint(signatureIterator.next().getValue())) {
                    return null;
                }
            }
        }

        return new MatchingSignature(signature, null, typeCollection, length);
    }

    private MatchingSignature matchingFromElement(Signature signature, ConfigElement providedElement) {
        Objects.requireNonNull(providedElement);

        boolean matchNames = signature.matchesArgumentNames();
        if (matchNames && !providedElement.isNode()) {
            return null;
        }

        Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();
        int length = signature.length(providedElement);
        if (elementCollection.size() != length) {
            return null;
        }

        boolean matchTypeHints = signature.matchesTypeHints();
        if (!(matchNames || matchTypeHints)) {
            return new MatchingSignature(signature, elementCollection, null, length);
        }


        Iterable<Map.Entry<String, Token<?>>> signatureTypes;
        Collection<ConfigElement> targetCollection;
        if (matchNames) {
            signatureTypes = signature.argumentTypes();

            ConfigNode providedNode = providedElement.asNode();
            targetCollection = new ArrayList<>(elementCollection.size());

            //this ensures that the order is respected when matching names
            for (Map.Entry<String, Token<?>> entry : signatureTypes) {
                String name = entry.getKey();
                ConfigElement element = providedNode.get(name);
                if (element == null) {
                    return null;
                }

                targetCollection.add(element);
            }
        } else {
            targetCollection = elementCollection;
        }

        if (matchTypeHints) {
            Iterator<ConfigElement> elementIterator = targetCollection.iterator();
            Iterator<Map.Entry<String, Token<?>>> signatureTypeIterator = signature.argumentTypes().iterator();

            while (elementIterator.hasNext()) {
                if (!typeHinter.assignable(elementIterator.next(), signatureTypeIterator.next().getValue())) {
                    return null;
                }
            }
        }

        return new MatchingSignature(signature, targetCollection, null, length);
    }

    private MatchingSignature signatureForElement(Token<?> typeToken, ConfigElement providedElement,
        Object providedObject) {
        for (Signature signature : signatures) {
            if (!signature.returnType().isSubclassOf(typeToken)) {
                continue;
            }

            MatchingSignature matching;
            if (providedElement == null) {
                matching = matchingFromObject(signature, providedObject);
            } else {
                matching = matchingFromElement(signature, providedElement);
            }

            if (matching != null) {
                return matching;
            }
        }

        throw new MapperException("Unable to find matching signature for element '" + providedElement + "'");
    }

    @Override
    public @NotNull MatchingSignature signatureForElement(@NotNull Token<?> desiredType,
        @NotNull ConfigElement providedElement) {
        return signatureForElement(desiredType, providedElement, null);
    }

    @Override
    public @NotNull MatchingSignature signatureForObject(@NotNull Token<?> desiredType, @NotNull Object object) {
        return signatureForElement(desiredType, null, object);
    }
}
