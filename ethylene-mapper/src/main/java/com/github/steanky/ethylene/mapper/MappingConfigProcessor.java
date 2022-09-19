package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.signature.MatchingSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private final Token<T> token;
    private final SignatureMatcher.Source signatureMatcherSource;
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;
    private final ScalarSource scalarSource;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull SignatureMatcher.Source signatureMatcherSource,
            @NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver, @NotNull ScalarSource scalarSource) {
        this.token = Objects.requireNonNull(token);
        this.signatureMatcherSource = Objects.requireNonNull(signatureMatcherSource);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.scalarSource = Objects.requireNonNull(scalarSource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Token<?> rootType = typeResolver.resolveType(token, element);
            SignatureMatcher rootFactory = signatureMatcherSource.matcherFor(rootType);

            return (T) Graph.process(new ClassEntry(rootType, element, rootFactory), nodeEntry -> {
                                ConfigElement nodeElement = nodeEntry.element;
                                MatchingSignature matchingSignature = nodeEntry.signatureMatcher.signature(nodeEntry.type, nodeElement,
                                        null);

                                Signature signature = matchingSignature.signature();
                                int signatureSize = matchingSignature.size();

                                Iterator<ConfigElement> elementIterator = matchingSignature.elements().iterator();
                                Iterator<Entry<String, Token<?>>> typeEntryIterator = signature.argumentTypes().iterator();

                                Object buildingObject =
                                        signature.hasBuildingObject() ? signature.initBuildingObject(nodeElement) : null;
                                nodeEntry.reference.setValue(buildingObject);

                                Object[] args = new Object[signatureSize];

                                return Graph.node(new Iterator<>() {
                                    private int i = 0;

                                    @Override
                                    public boolean hasNext() {
                                        return i < signatureSize;
                                    }

                                    @Override
                                    public Entry<Object, ClassEntry> next() {
                                        if (i++ == signatureSize) {
                                            throw new NoSuchElementException();
                                        }

                                        ConfigElement nextElement = elementIterator.next();
                                        Token<?> nextType = typeResolver.resolveType(typeEntryIterator.next().getSecond(), nextElement);
                                        SignatureMatcher nextMatcher = signatureMatcherSource.matcherFor(nextType);

                                        return Entry.of(null, new ClassEntry(nextType, nextElement, nextMatcher));
                                    }
                                }, Graph.output(nodeEntry.reference, new Graph.Accumulator<>() {
                                    private int i = 0;

                                    @Override
                                    public void accept(Object key, Mutable<Object> value, boolean circular) {
                                        if (circular && !signature.hasBuildingObject()) {
                                            throw new MapperException("Signatures which do not supply building objects may " +
                                                    "not be used to construct circular references");
                                        }

                                        args[i++] = value.getValue();

                                        if (i == args.length) {
                                            nodeEntry.reference.setValue(signature.buildObject(buildingObject, args));
                                        }
                                    }
                                }));
                            }, potentialContainer -> potentialContainer.element.isContainer(),
                            scalar -> new MutableObject<>(scalar.element.asScalar()), entry -> entry.element,
                            Graph.Options.DEPTH_FIRST | Graph.Options.TRACK_REFERENCES | Graph.Options.LAZY_ACCUMULATION)
                    .getValue();
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T data) throws ConfigProcessException {
        try {
            Token<?> rootType = typeResolver.resolveType(token, null);
            SignatureMatcher rootMatcher = signatureMatcherSource.matcherFor(rootType);
            ElementEntry rootEntry = new ElementEntry(rootType, data, rootMatcher);

            return Graph.process(rootEntry, nodeEntry -> {
                                Object nodeObject = nodeEntry.object;
                                MatchingSignature typeSignature = nodeEntry.signatureMatcher.signature(nodeEntry.type, null,
                                        nodeObject);
                                Signature signature = typeSignature.signature();
                                int size = typeSignature.size();

                                ConfigContainer target = signature.initContainer(size);
                                nodeEntry.element.setValue(target);

                                Iterator<Signature.TypedObject> typedObjectIterator = typeSignature.objects().iterator();

                                return Graph.node(new Iterator<>() {
                                    private int i = 0;

                                    @Override
                                    public boolean hasNext() {
                                        return i < size;
                                    }

                                    @Override
                                    public Entry<String, ElementEntry> next() {
                                        if (i++ == size) {
                                            throw new NoSuchElementException();
                                        }

                                        Signature.TypedObject typedObject = typedObjectIterator.next();
                                        Token<?> objectType = typeResolver.resolveType(typedObject.type(), null);
                                        SignatureMatcher thisMatcher = signatureMatcherSource.matcherFor(objectType);

                                        return Entry.of(typedObject.name(),
                                                new ElementEntry(objectType, typedObject.value(), thisMatcher));
                                    }
                                }, Graph.output(nodeEntry.element,
                                        (Graph.Accumulator<String, Mutable<ConfigElement>>) (key, value, circular) -> {
                                            if (target.isList()) {
                                                target.asList().add(value.getValue());
                                            } else {
                                                target.asNode().put(key, value.getValue());
                                            }
                                        }));
                            }, potentialContainer -> {
                                //runtime type is passed to TypeHinter: this should be safe since it doesn't care about generics
                                return potentialContainer.object != null &&
                                        typeHinter.getHint(Token.ofClass(potentialContainer.object.getClass())) != ElementType.SCALAR;
                            }, scalar -> new MutableObject<>(scalarSource.make(scalar.object)), entry -> entry.object,
                            Graph.Options.DEPTH_FIRST | Graph.Options.TRACK_REFERENCES | Graph.Options.LAZY_ACCUMULATION)
                    .getValue();
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    private record ElementEntry(Token<?> type, Object object, SignatureMatcher signatureMatcher,
            Mutable<ConfigElement> element) {
        private ElementEntry(Token<?> type, Object object, SignatureMatcher signatureMatcher) {
            this(type, object, signatureMatcher, new MutableObject<>(null));
        }
    }

    private record ClassEntry(Token<?> type, ConfigElement element, SignatureMatcher signatureMatcher,
            Mutable<Object> reference) {
        private ClassEntry(Token<?> type, ConfigElement element, SignatureMatcher signatureMatcher) {
            this(type, element, signatureMatcher, new MutableObject<>(null));
        }
    }
}