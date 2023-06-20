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
     * @param signatures  the signatures array; note that this is copied so it is safe to modify the array later
     * @param typeHinter  the {@link TypeHinter} used to extract type information when matching type hints
     * @param matchLength true if the size of the data provided to create objects need not match the signature length
     *                    exactly
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

        Iterable<Map.Entry<String, SignatureParameter>> signatureTypes = signature.argumentTypes();
        Collection<Signature.TypedObject> typeCollection;
        if (matchNames) {
            typeCollection = new ArrayList<>(objectData.size());
            Map<String, Signature.TypedObject> objectDataMap = new HashMap<>(objectData.size());
            for (Signature.TypedObject entry : objectData) {
                objectDataMap.put(entry.name(), entry);
            }

            for (Map.Entry<String, SignatureParameter> entry : signatureTypes) {
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

            for (Map.Entry<String, SignatureParameter> signatureType : signatureTypes) {
                if (typeCollectionIterator.hasNext()) {
                    Signature.TypedObject typedObject = typeCollectionIterator.next();
                    if (typeHinter.getHint(typedObject.type()) != typeHinter.getHint(signatureType.getValue().type())) {
                        return null;
                    }
                } else {
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

        Iterable<Map.Entry<String, SignatureParameter>> signatureTypes = signature.argumentTypes();
        Collection<ConfigElement> targetCollection;
        if (matchNames) {
            ConfigNode providedNode = providedElement.asNode();
            targetCollection = new ArrayList<>(elementCollection.size());

            //this ensures that the order is respected when matching names
            for (Map.Entry<String, SignatureParameter> entry : signatureTypes) {
                String name = entry.getKey();
                ConfigElement element = providedNode.get(name);
                if (element == null) {
                    ConfigElement defaultElement = entry.getValue().defaultOption();
                    if (defaultElement == null) {
                        return null;
                    }

                    targetCollection.add(defaultElement);
                    continue;
                }

                targetCollection.add(element);
            }
        } else {
            targetCollection = elementCollection;
        }

        if (matchTypeHints) {
            Iterator<ConfigElement> elementIterator = targetCollection.iterator();

            for (Map.Entry<String, SignatureParameter> signatureTypeEntry : signatureTypes) {
                if (elementIterator.hasNext()) {
                    ConfigElement element = elementIterator.next();

                    if (!typeHinter.assignable(element, signatureTypeEntry.getValue().type())) {
                        return null;
                    }
                } else {
                    break;
                }
            }
        }

        return new MatchingSignature(signature, targetCollection, null, signatureLength);
    }

    private MatchingSignature signatureForElement(ConfigElement providedElement,
        Object providedObject) {
        MatchingSignature bestSignature = null;

        for (Signature<?> signature : signatures) {
            MatchingSignature matching;
            if (providedElement == null) {
                matching = matchingFromObject(signature, providedObject);
            } else {
                matching = matchingFromElement(signature, providedElement);
            }

            if (matching == null) {
                //no match found
                continue;
            }

            if (!matchLength) {
                if (bestSignature == null) {
                    bestSignature = matching;
                } else if (bestSignature.size() < matching.size()) {
                    bestSignature = matching;
                }
            }

            if (matchLength) {
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
        return signatureForElement(providedElement, null);
    }

    @Override
    public @NotNull MatchingSignature signatureForObject(@NotNull Token<?> desiredType, @NotNull Object object) {
        return signatureForElement(null, object);
    }
}
