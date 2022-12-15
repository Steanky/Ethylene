package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.TypeHinter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Basic implementation of {@link SignatureMatcher}. Capable of matching based on type hints, type names, or both,
 * depending on what is supported by each signature it manages. It respects signature priority.
 */
public class BasicSignatureMatcher implements SignatureMatcher {
    private final Signature<?>[] signatures;
    private final TypeHinter typeHinter;
    private final boolean matchLength;

    /**
     * Creates a new instance of this class
     *
     * @param signatures the signatures array; note that this is copied so it is safe to modify the array later
     * @param typeHinter the {@link TypeHinter} used to extract type information when matching type hints
     */
    public BasicSignatureMatcher(@NotNull Signature<?> @NotNull [] signatures, @NotNull TypeHinter typeHinter,
        boolean matchLength) {
        Signature<?>[] copy = new Signature[signatures.length];
        System.arraycopy(signatures, 0, copy, 0, signatures.length);
        Arrays.sort(copy);
        this.signatures = copy;
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.matchLength = matchLength;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private MatchingSignature matchingFromObject(Signature signature, Object providedObject) {
        Objects.requireNonNull(providedObject);

        Collection<Signature.TypedObject> objectData = signature.objectData(providedObject);

        int length = signature.length(null);
        if (matchLength && length > -1 && length != objectData.size()) {
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
            Iterable<Map.Entry<String, Token<?>>> signatureIterable = () -> signature.argumentTypes().iterator();

            for (Map.Entry<String, Token<?>> signatureType : signatureIterable) {
                if (typeCollectionIterator.hasNext()) {
                    Signature.TypedObject typedObject = typeCollectionIterator.next();
                    if (typeHinter.getHint(typedObject.type()) != typeHinter.getHint(signatureType.getValue())) {
                        return null;
                    }
                }
                else {
                    break;
                }
            }
        }

        return new MatchingSignature(signature, null, typeCollection, length);
    }

    private MatchingSignature matchingFromElement(Signature<?> signature, ConfigElement providedElement) {
        Objects.requireNonNull(providedElement);

        boolean matchNames = signature.matchesArgumentNames();
        if (matchNames && !providedElement.isNode()) {
            return null;
        }

        Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();

        int signatureLength = signature.length(providedElement);
        if (matchLength && elementCollection.size() != signatureLength) {
            return null;
        }

        boolean matchTypeHints = signature.matchesTypeHints();
        if (!(matchNames || matchTypeHints)) {
            return new MatchingSignature(signature, elementCollection, null, signatureLength);
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

            for (Map.Entry<String, Token<?>> signatureType : signature.argumentTypes()) {
                if (elementIterator.hasNext()) {
                    ConfigElement element = elementIterator.next();
                    if (!typeHinter.assignable(element, signatureType.getValue())) {
                        return null;
                    }
                }
                else {
                    break;
                }
            }
        }

        return new MatchingSignature(signature, targetCollection, null, signatureLength);
    }

    private MatchingSignature signatureForElement(Token<?> typeToken, ConfigElement providedElement,
        Object providedObject) {
        MatchingSignature bestSignature = null;

        for (Signature<?> signature : signatures) {
            if (!signature.returnType().isSubclassOf(typeToken)) {
                continue;
            }

            MatchingSignature matching;
            if (providedElement == null) {
                matching = matchingFromObject(signature, providedObject);
            } else {
                matching = matchingFromElement(signature, providedElement);
            }

            if (!matchLength && matching != null) {
                if (bestSignature == null) {
                    bestSignature = matching;
                }
                else if (bestSignature.size() < matching.size()) {
                    bestSignature = matching;
                }
            }

            if (matching != null && matchLength) {
                return matching;
            }
        }

        if (bestSignature != null) {
            return bestSignature;
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
