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
    private final boolean matchNames;
    private final boolean matchTypeHints;


    public BasicSignatureMatcher(@NotNull Signature @NotNull [] signatures, @NotNull TypeHinter typeHinter,
            boolean matchNames, boolean matchTypeHints) {
        this.signatures = Objects.requireNonNull(signatures);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.matchNames = matchNames;
        this.matchTypeHints = matchTypeHints;
    }

    @Override
    public @NotNull MatchingSignature signature(@NotNull ConfigElement providedElement, @NotNull Type desiredType) {
        for (Signature signature : signatures) {
            if (!TypeUtils.isAssignable(signature.returnType(), desiredType)) {
                continue;
            }

            ElementType signatureHint = signature.typeHint();
            Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();

            int length = signature.length(providedElement);
            if (elementCollection.size() != length) {
                continue;
            }

            if (signatureHint == ElementType.LIST || !(matchNames || matchTypeHints)) {
                return new MatchingSignature(signature, elementCollection, length);
            }

            boolean hasArgumentNames = signature.hasArgumentNames();
            Iterable<Entry<String, Type>> signatureTypes = signature.argumentTypes();

            outer:
            {
                Collection<ConfigElement> targetCollection;
                if (matchNames) {
                    if (!hasArgumentNames) {
                        targetCollection = elementCollection;
                    }
                    else {
                        if (!providedElement.isNode()) {
                            continue;
                        }

                        ConfigNode providedNode = providedElement.asNode();
                        targetCollection = new ArrayList<>(elementCollection.size());
                        for (Entry<String, Type> entry : signatureTypes) {
                            String name = entry.getFirst();
                            ConfigElement element = providedNode.get(name);
                            if (element == null) {
                                break outer;
                            }

                            targetCollection.add(element);
                        }
                    }
                }
                else {
                    targetCollection = elementCollection;
                }

                if (matchTypeHints) {
                    Iterator<ConfigElement> element = elementCollection.iterator();
                    for (Entry<String, Type> entry : signatureTypes) {
                        if (!typeHinter.getHint(entry.getSecond()).compatible(element.next())) {
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
