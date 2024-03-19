package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
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

    private enum MismatchReason {
        NO_MISMATCH,
        LENGTH_DIFFERS,
        MISSING_PARAMETER,
        WRONG_NODE_TYPE,
        WRONG_PARAMETER_TYPE
    }
    private record MatchResult(MatchingSignature matching, MismatchReason mismatchReason,
                               int matchScore, Collection<Signature.TypedObject> objectData) {}

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
    private MatchResult matchingFromObject(Signature signature, Object providedObject) {
        Objects.requireNonNull(providedObject);

        Collection<Signature.TypedObject> objectData = signature.objectData(providedObject);

        int length = signature.length(null);
        if (matchLength && length > -1 && length != objectData.size()) {
            return new MatchResult(null, MismatchReason.LENGTH_DIFFERS,
                -Math.abs(length - objectData.size()), objectData);
        }

        length = objectData.size();
        boolean matchNames = signature.matchesArgumentNames();
        boolean matchTypeHints = signature.matchesTypeHints();

        if (!(matchNames || matchTypeHints)) {
            return new MatchResult(new MatchingSignature(signature, null, objectData, length),
                MismatchReason.NO_MISMATCH, Integer.MAX_VALUE, objectData);
        }

        Iterable<Map.Entry<String, SignatureParameter>> signatureTypes = signature.argumentTypes();
        Collection<Signature.TypedObject> typeCollection;
        if (matchNames) {
            typeCollection = new ArrayList<>(objectData.size());
            Map<String, Signature.TypedObject> objectDataMap = new HashMap<>(objectData.size());
            for (Signature.TypedObject entry : objectData) {
                objectDataMap.put(entry.name(), entry);
            }

            boolean mismatch = false;
            int matchScore = 0;
            for (Map.Entry<String, SignatureParameter> entry : signatureTypes) {
                Signature.TypedObject typedObject = objectDataMap.get(entry.getKey());
                if (typedObject == null) {
                    if (entry.getValue().defaultOption() != null) {
                        continue;
                    }

                    mismatch = true;
                    continue;
                }

                matchScore++;
                if (!mismatch) {
                    typeCollection.add(typedObject);
                }
            }

            if (mismatch) {
                return new MatchResult(null, MismatchReason.MISSING_PARAMETER, matchScore, objectData);
            }
        } else {
            typeCollection = objectData;
        }

        if (matchTypeHints) {
            Iterator<Signature.TypedObject> typeCollectionIterator = typeCollection.iterator();

            boolean mismatch = false;
            int matchScore = 0;
            for (Map.Entry<String, SignatureParameter> signatureType : signatureTypes) {
                if (!typeCollectionIterator.hasNext()) {
                    break;
                }

                Signature.TypedObject typedObject = typeCollectionIterator.next();
                if (typeHinter.getHint(typedObject.type()) != typeHinter.getHint(signatureType.getValue().type())) {
                    mismatch = true;
                    continue;
                }

                matchScore++;
            }

            if (mismatch) {
                return new MatchResult(null, MismatchReason.WRONG_PARAMETER_TYPE, matchScore, objectData);
            }
        }

        return new MatchResult(new MatchingSignature(signature, null, typeCollection, length),
            MismatchReason.NO_MISMATCH, Integer.MAX_VALUE, objectData);
    }

    private MatchResult matchingFromElement(Signature<?> signature, ConfigElement providedElement) {
        Objects.requireNonNull(providedElement);

        boolean matchNames = signature.matchesArgumentNames();
        if (matchNames && !providedElement.isNode()) {
            return new MatchResult(null, MismatchReason.WRONG_NODE_TYPE, Integer.MIN_VALUE, null);
        }

        Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();

        int signatureLength = signature.length(providedElement);
        if (matchLength && elementCollection.size() != signatureLength) {
            return new MatchResult(null, MismatchReason.LENGTH_DIFFERS,
                -Math.abs(signatureLength - elementCollection.size()), null);
        }

        boolean matchTypeHints = signature.matchesTypeHints();
        if (!(matchNames || matchTypeHints)) {
            return new MatchResult(new MatchingSignature(signature, elementCollection, null, signatureLength),
                MismatchReason.NO_MISMATCH, Integer.MAX_VALUE, null);
        }

        Iterable<Map.Entry<String, SignatureParameter>> signatureTypes = signature.argumentTypes();
        Collection<ConfigElement> targetCollection;
        if (matchNames) {
            ConfigNode providedNode = providedElement.asNode();
            targetCollection = new ArrayList<>(elementCollection.size());

            int matchScore = 0;
            boolean mismatch = false;
            for (Map.Entry<String, SignatureParameter> entry : signatureTypes) {
                String name = entry.getKey();
                ConfigElement element = providedNode.get(name);
                if (element == null) {
                    ConfigElement defaultElement = entry.getValue().defaultOption();
                    if (defaultElement == null) {
                        //this signature is not a match, but keep iterating anyway so we compute a match score
                        mismatch = true;
                        continue;
                    }

                    matchScore++;
                    if (!mismatch) {
                        targetCollection.add(defaultElement);
                    }

                    continue;
                }

                matchScore++;
                if (!mismatch) {
                    targetCollection.add(element);
                }
            }

            if (mismatch) {
                return new MatchResult(null, MismatchReason.MISSING_PARAMETER, matchScore, null);
            }
        } else {
            targetCollection = elementCollection;
        }

        if (matchTypeHints) {
            Iterator<ConfigElement> elementIterator = targetCollection.iterator();

            boolean mismatch = false;
            int matchScore = 0;
            for (Map.Entry<String, SignatureParameter> signatureTypeEntry : signatureTypes) {
                if (!elementIterator.hasNext()) {
                    break;
                }

                ConfigElement element = elementIterator.next();
                if (!typeHinter.assignable(element, signatureTypeEntry.getValue().type())) {
                    mismatch = true;
                    continue;
                }

                matchScore++;
            }

            if (mismatch) {
                return new MatchResult(null, MismatchReason.WRONG_PARAMETER_TYPE, matchScore, null);
            }
        }

        return new MatchResult(new MatchingSignature(signature, targetCollection, null, signatureLength),
            MismatchReason.NO_MISMATCH, Integer.MAX_VALUE, null);
    }

    private MatchingSignature signatureForElement(ConfigElement providedElement,
        Object providedObject) {
        MatchingSignature bestSignature = null;

        MatchResult closestMatch = null;
        Signature<?> closestSignature = null;
        for (Signature<?> signature : signatures) {
            MatchResult matching;
            if (providedElement == null) {
                matching = matchingFromObject(signature, providedObject);
            } else {
                matching = matchingFromElement(signature, providedElement);
            }

            if (closestMatch == null || closestMatch.matchScore < matching.matchScore) {
                closestMatch = matching;
                closestSignature = signature;
            }

            if (matching.matching == null) {
                //no match found
                continue;
            }

            if (matchLength) {
                return matching.matching;
            }

            if (bestSignature == null) {
                bestSignature = matching.matching;
            } else if (bestSignature.size() < matching.matching.size()) {
                bestSignature = matching.matching;
            }
        }

        if (bestSignature != null) {
            return bestSignature;
        }

        throw noSignatureException(closestSignature, providedElement, providedObject, closestMatch);
    }

    private RuntimeException noSignatureException(Signature<?> closestSignature, ConfigElement element,
        Object object, MatchResult matchResult) {
        if (element != null) {
            return switch (matchResult.mismatchReason) {
                case NO_MISMATCH -> new IllegalStateException("Unexpected mismatchReason NO_MISMATCH");
                case LENGTH_DIFFERS, MISSING_PARAMETER -> {
                    ConfigContainer container = element.asContainer();
                    int uniqueLength = closestSignature.uniqueLength();

                    if (closestSignature.matchesArgumentNames()) {
                        yield missingParametersForNode(container.asNode(), closestSignature, uniqueLength);
                    }

                    int signatureLength = closestSignature.length(element);
                    if (signatureLength < 0) {
                        yield new MapperException("Appropriate signature length could not be determined");
                    }

                    yield new MapperException("Signature length differs; was " + container.entryCollection().size() +
                        ", expected " + signatureLength);
                }
                case WRONG_NODE_TYPE ->
                    new MapperException("Wrong element type, expected NODE but was " + element.type());
                case WRONG_PARAMETER_TYPE -> {
                    Collection<ConfigElement> elements;
                    boolean matchNames = closestSignature.matchesArgumentNames();
                    if (matchNames) {
                        ConfigNode node = element.asNode();
                        elements = new ArrayList<>(node.size());

                        for (Map.Entry<String, SignatureParameter> entry : closestSignature.argumentTypes()) {
                            ConfigElement namedElement = node.get(entry.getKey());
                            if (namedElement != null) {
                                elements.add(namedElement);
                            }
                            else {
                                ConfigElement defaultElement = entry.getValue().defaultOption();
                                if (defaultElement != null) {
                                      elements.add(defaultElement);
                                }
                            }
                        }
                    }
                    else {
                        elements = element.asContainer().elementCollection();
                    }

                    Iterator<ConfigElement> elementIterator = elements.iterator();
                    Iterator<Map.Entry<String, SignatureParameter>> entryIterator = closestSignature.argumentTypes().iterator();
                    StringBuilder builder = new StringBuilder("Some parameter(s) are the wrong type: ");

                    int i = 0;
                    while (entryIterator.hasNext()) {
                        Map.Entry<String, SignatureParameter> entry = entryIterator.next();
                        Token<?> parameterType = entry.getValue().type();
                        ConfigElement configElement = elementIterator.next();

                        if (!typeHinter.assignable(configElement, parameterType)) {
                            builder.append(System.lineSeparator()).append("Entry ")
                                .append(matchNames ? entry.getKey() : i).append(" not assignable to ")
                                .append(parameterType.getTypeName());
                        }

                        i++;
                    }

                    yield new MapperException(builder.toString());
                }
            };
        }

        return switch (matchResult.mismatchReason) {
            case NO_MISMATCH, WRONG_NODE_TYPE -> new IllegalStateException("Unexpected mismatchReason " +
                matchResult.mismatchReason);
            case LENGTH_DIFFERS, MISSING_PARAMETER -> {
                int signatureLength = closestSignature.length(null);
                if (!closestSignature.matchesArgumentNames()) {
                    if (signatureLength < 0) {
                        yield new MapperException("Appropriate signature length could not be determined");
                    }

                    yield new MapperException("Signature length differs; was " + matchResult.objectData.size() +
                        ", expected " + signatureLength);
                }

                Iterator<Map.Entry<String, SignatureParameter>> iterator = closestSignature.argumentTypes().iterator();

                Map<String, SignatureParameter> requiredValues = new HashMap<>();
                for (int i = 0; i < signatureLength && iterator.hasNext(); i++) {
                    Map.Entry<String, SignatureParameter> next = iterator.next();
                    SignatureParameter parameter = next.getValue();
                    if (parameter.defaultOption() != null) {
                        requiredValues.put(next.getKey(), parameter);
                    }
                }

                for (Signature.TypedObject typed : matchResult.objectData) {
                    requiredValues.remove(typed.name());
                }

                StringBuilder builder = new StringBuilder("Serializable object ( ");
                builder.append(object.getClass()).append(") missing the following required fields:");
                for (Map.Entry<String, SignatureParameter> entry : requiredValues.entrySet()) {
                    builder.append(System.lineSeparator()).append(entry.getKey());
                }

                yield new MapperException(builder.toString());
            }
            case WRONG_PARAMETER_TYPE -> {
                int signatureLength = closestSignature.length(null);

                Collection<Signature.TypedObject> typeCollection;
                if (closestSignature.matchesArgumentNames()) {
                    typeCollection = new ArrayList<>();
                    Map<String, Signature.TypedObject> objectDataMap = new HashMap<>(matchResult.objectData.size());
                    for (Signature.TypedObject entry : matchResult.objectData) {
                        objectDataMap.put(entry.name(), entry);
                    }

                    Iterator<Map.Entry<String, SignatureParameter>> iterator = closestSignature.argumentTypes().iterator();

                    for (int i = 0; i < signatureLength && iterator.hasNext(); i++) {
                        Map.Entry<String, SignatureParameter> entry = iterator.next();
                        Signature.TypedObject typedObject = objectDataMap.get(entry.getKey());
                        if (entry.getValue().defaultOption() == null && typedObject != null) {
                            typeCollection.add(typedObject);
                        }
                    }
                }
                else {
                    typeCollection = matchResult.objectData;
                }

                Iterator<Map.Entry<String, SignatureParameter>> iterator = closestSignature.argumentTypes().iterator();
                Iterator<Signature.TypedObject> typedObjectIterator = typeCollection.iterator();
                StringBuilder builder = new StringBuilder("Mismatched type(s) in serialized object: ");
                while (iterator.hasNext() && typedObjectIterator.hasNext()) {
                    Map.Entry<String, SignatureParameter> signatureType = iterator.next();
                    Signature.TypedObject typedObject = typedObjectIterator.next();

                    if (typeHinter.getHint(typedObject.type()) != typeHinter.getHint(signatureType.getValue().type())) {
                        builder.append(System.lineSeparator()).append("Parameter ")
                            .append(signatureType.getKey()).append(" of type ")
                            .append(signatureType.getValue().type().getTypeName()).append(" not assignable from ")
                            .append(typedObject.type().getTypeName());
                    }
                }

                yield new MapperException(builder.toString());
            }
        };
    }

    private static RuntimeException missingParametersForNode(ConfigNode node, Signature<?> closestSignature,
        int uniqueLength) {
        List<Map.Entry<String, SignatureParameter>> missingEntries = new ArrayList<>();
        Iterator<Map.Entry<String, SignatureParameter>> iterator = closestSignature.argumentTypes().iterator();

        for (int i = 0; i < uniqueLength && iterator.hasNext(); i++) {
            Map.Entry<String, SignatureParameter> entry = iterator.next();
            String key = entry.getKey();
            if (entry.getValue().defaultOption() == null && !node.containsKey(key)) {
                missingEntries.add(entry);
            }
        }

        StringBuilder message =
            new StringBuilder("Configuration is missing the following non-default parameter(s): ");
        for (Map.Entry<String, SignatureParameter> entry : missingEntries) {
            message.append(System.lineSeparator()).append(entry.getKey()).append(" (")
                .append(entry.getValue().type().getTypeName()).append(")");
        }

        return new MapperException(message.toString());
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
