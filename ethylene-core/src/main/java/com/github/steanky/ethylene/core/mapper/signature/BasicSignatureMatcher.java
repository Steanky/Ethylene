package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.*;
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
    public @NotNull MatchingSignature signature(@NotNull ConfigElement providedElement, @NotNull Type desiredType) {
        for (Signature signature : signatures) {
            if (!TypeUtils.isAssignable(signature.returnType(), desiredType)) {
                continue;
            }

            Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();
            int length = signature.length(providedElement);
            if (elementCollection.size() != length) {
                continue;
            }

            boolean matchNames = signature.matchesArgumentNames();
            boolean matchTypeHints = signature.matchesTypeHints();

            if (!(matchNames || matchTypeHints)) {
                return new MatchingSignature(signature, elementCollection, length);
            }

            Iterable<Entry<String, Type>> signatureTypes = signature.argumentTypes();

            outer:
            {
                Collection<ConfigElement> targetCollection;
                if (matchNames) {
                    if (!providedElement.isNode()) {
                        continue;
                    }

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
                }
                else {
                    targetCollection = elementCollection;
                }

                if (matchTypeHints) {
                    Iterator<ConfigElement> elementIterator = targetCollection.iterator();
                    Iterator<Entry<String, Type>> signatureTypeIterator = signatureTypes.iterator();

                    while (elementIterator.hasNext()) {
                        Type signatureType = signatureTypeIterator.next().getSecond();
                        Type preferredElementType = typeHinter.getPreferredType(elementIterator.next(), signatureType);

                        if (!TypeUtils.isAssignable(preferredElementType, signatureType)) {
                            break outer;
                        }
                    }
                }

                return new MatchingSignature(signature, targetCollection, length);
            }
        }

        throw new MapperException("unable to find matching signature for element '" + providedElement);
    }
}
