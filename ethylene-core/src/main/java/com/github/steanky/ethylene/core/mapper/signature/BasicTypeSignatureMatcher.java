package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.*;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

public class BasicTypeSignatureMatcher implements TypeSignatureMatcher {
    private final Signature[] signatures;
    private final TypeHinter typeHinter;
    private final boolean matchNames;
    private final boolean matchTypeHints;


    public BasicTypeSignatureMatcher(@NotNull Signature[] signatures, @NotNull TypeHinter typeHinter,
            boolean matchNames, boolean matchTypeHints) {
        this.signatures = Objects.requireNonNull(signatures);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.matchNames = matchNames;
        this.matchTypeHints = matchTypeHints;
    }

    @Override
    public @NotNull OrderedSignature signature(@NotNull ConfigElement providedElement, @NotNull Type desiredType) {
        for (Signature signature : signatures) {
            if (!TypeUtils.isAssignable(signature.returnType(), desiredType)) {
                continue;
            }

            TypeHinter.Hint signatureHint = signature.typeHint();
            Collection<ConfigElement> elementCollection = providedElement.asContainer().elementCollection();
            int signatureLength = signature.length(providedElement);
            if (signatureHint == TypeHinter.Hint.CONTAINER_LIKE || !(matchNames || matchTypeHints)) {
                return new OrderedSignature(signature, elementCollection, signatureLength);
            }

            if (!providedElement.isNode()) {
                throw new MapperException("expected ConfigNode");
            }

            ConfigNode providedNode = providedElement.asNode();
            boolean hasArgumentNames = signature.hasArgumentNames();
            Iterable<Entry<String, Type>> signatureTypes = signature.argumentTypes();

            outer:
            {
                Collection<ConfigElement> targetCollection;
                if (matchNames) {
                    if (!hasArgumentNames) {
                        targetCollection = providedNode.values();
                    }
                    else {
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
                    targetCollection = providedNode.values();
                }

                if (matchTypeHints) {
                    Iterator<ConfigElement> element = elementCollection.iterator();
                    for (Entry<String, Type> entry : signatureTypes) {
                        if (!typeHinter.getHint(entry.getSecond()).compatible(element.next())) {
                            break outer;
                        }
                    }
                }

                return new OrderedSignature(signature, targetCollection, signatureLength);
            }

        }

        throw new MapperException("unable to find matching signature for element '" + providedElement);
    }
}
