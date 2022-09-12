package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.TypeHinter;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
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

    @Override
    public @NotNull MatchingSignature signature(@NotNull Type desiredType, ConfigElement providedElement,
            Object providedObject) {
        for (Signature signature : signatures) {
            if (!TypeUtils.isAssignable(signature.returnType(), desiredType)) {
                continue;
            }

            if (providedElement == null) {
                //object -> ConfigElement
                Objects.requireNonNull(providedObject);

                Collection<Signature.TypedObject> objectData = signature.objectData(providedObject);

                int length = signature.length(null);
                if (length > -1 && length != objectData.size()) {
                    continue;
                }

                length = objectData.size();
                boolean matchNames = signature.matchesArgumentNames();
                boolean matchTypeHints = signature.matchesTypeHints();

                if (!(matchNames || matchTypeHints)) {
                    return new MatchingSignature(signature, null, objectData, length);
                }

                outer:
                {
                    Collection<Signature.TypedObject> typeCollection;
                    if (matchNames) {
                        typeCollection = new ArrayList<>(objectData.size());
                        Map<String, Signature.TypedObject> objectDataMap = new HashMap<>(objectData.size());
                        for (Signature.TypedObject entry : objectData) {
                            objectDataMap.put(entry.name(), entry);
                        }

                        Iterable<Entry<String, Type>> signatureTypes = signature.argumentTypes();
                        for (Entry<String, Type> entry : signatureTypes) {
                            Signature.TypedObject typedObject = objectDataMap.get(entry.getFirst());
                            if (typedObject == null) {
                                break outer;
                            }

                            typeCollection.add(typedObject);
                        }
                    } else {
                        typeCollection = objectData;
                    }

                    if (matchTypeHints) {
                        Iterator<Signature.TypedObject> typeCollectionIterator = typeCollection.iterator();
                        Iterator<Entry<String, Type>> signatureIterator = signature.argumentTypes().iterator();

                        while (typeCollectionIterator.hasNext()) {
                            if (typeHinter.getHint(typeCollectionIterator.next().type()) !=
                                    typeHinter.getHint(signatureIterator.next().getSecond())) {
                                break outer;
                            }
                        }
                    }

                    return new MatchingSignature(signature, null, typeCollection, length);
                }

                continue;
            }

            //ConfigElement - object
            Objects.requireNonNull(providedElement);

            boolean matchNames = signature.matchesArgumentNames();
            if (matchNames && !providedElement.isNode()) {
                continue;
            }

            Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();
            int length = signature.length(providedElement);
            if (elementCollection.size() != length) {
                continue;
            }

            boolean matchTypeHints = signature.matchesTypeHints();
            if (!(matchNames || matchTypeHints)) {
                return new MatchingSignature(signature, elementCollection, null, length);
            }

            outer:
            {
                Iterable<Entry<String, Type>> signatureTypes;
                Collection<ConfigElement> targetCollection;
                if (matchNames) {
                    signatureTypes = signature.argumentTypes();

                    ConfigNode providedNode = providedElement.asNode();
                    targetCollection = new ArrayList<>(elementCollection.size());

                    //this ensures that the order is respected when matching names
                    for (Entry<String, Type> entry : signatureTypes) {
                        String name = entry.getFirst();
                        ConfigElement element = providedNode.get(name);
                        if (element == null) {
                            break outer;
                        }

                        targetCollection.add(element);
                    }
                } else {
                    targetCollection = elementCollection;
                }

                if (matchTypeHints) {
                    Iterator<ConfigElement> elementIterator = targetCollection.iterator();
                    Iterator<Entry<String, Type>> signatureTypeIterator = signature.argumentTypes().iterator();

                    while (elementIterator.hasNext()) {
                        if (!typeHinter.assignable(elementIterator.next(), signatureTypeIterator.next().getSecond())) {
                            break outer;
                        }
                    }
                }

                return new MatchingSignature(signature, targetCollection, null, length);
            }
        }

        throw new MapperException("unable to find matching signature for element '" + providedElement + "'");
    }
}
