package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.mapper.signature.MatchingSignature;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Objects;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private final Token<T> token;
    private final SignatureMatcher.Source signatureMatcherSource;
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull SignatureMatcher.Source signatureMatcherSource,
            @NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver) {
        this.token = Objects.requireNonNull(token);
        this.signatureMatcherSource = Objects.requireNonNull(signatureMatcherSource);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Type rootType = typeResolver.resolveType(token.get(), element);
            SignatureMatcher rootFactory = signatureMatcherSource.matcherFor(rootType, element);

            return (T) GraphTransformer.process(new ClassEntry(rootType, element, rootFactory), nodeEntry -> {
                        ConfigElement nodeElement = nodeEntry.element;
                        MatchingSignature matchingSignature = nodeEntry.signatureMatcher.signature(nodeEntry.type,
                                nodeElement, null);

                        Signature signature = matchingSignature.signature();
                        int signatureSize = matchingSignature.size();

                        Iterator<ConfigElement> elementIterator = matchingSignature.elements().iterator();
                        Iterator<Entry<String, Type>> typeEntryIterator = signature.argumentTypes().iterator();

                        Object buildingObject = signature.hasBuildingObject() ?
                                signature.initBuildingObject(nodeElement) : null;
                        nodeEntry.reference.setValue(buildingObject);

                        Object[] args = new Object[signatureSize];

                        return new GraphTransformer.Node<>(nodeEntry, new Iterator<>() {
                            private int i = 0;

                            @Override
                            public boolean hasNext() {
                                return i < signatureSize;
                            }

                            @Override
                            public Entry<Object, ClassEntry> next() {
                                i++;

                                ConfigElement nextElement = elementIterator.next();
                                Type nextType = typeResolver.resolveType(typeEntryIterator.next().getSecond(), nextElement);
                                SignatureMatcher nextMatcher = signatureMatcherSource.matcherFor(nextType, nextElement);

                                return Entry.of(null, new ClassEntry(nextType, nextElement, nextMatcher));
                            }
                        }, new GraphTransformer.Output<>(nodeEntry.reference, new GraphTransformer.Accumulator<>() {
                            private int i = 0;

                            @Override
                            public void accept(Object key, Mutable<Object> value, boolean circular) {
                                args[i++] = value.getValue();

                                if (i == args.length) {
                                    nodeEntry.reference.setValue(signature.buildObject(buildingObject, args));
                                }
                            }
                        }));
                    }, potentialContainer -> potentialContainer.element.isContainer(),
                    scalar -> new MutableObject<>(scalar.element.asScalar()), entry -> entry.element).getValue();
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T data) throws ConfigProcessException {
        try {
            Type rootType = typeResolver.resolveType(token.get(), null);
            SignatureMatcher rootMatcher = signatureMatcherSource.matcherFor(rootType, null);
            ElementEntry rootEntry = new ElementEntry(rootType, data, rootMatcher);

            return GraphTransformer.process(rootEntry, nodeEntry -> {
                        Object nodeObject = nodeEntry.object;
                        MatchingSignature typeSignature = nodeEntry.signatureMatcher.signature(nodeEntry.type,
                                null, nodeObject);
                        Signature signature = typeSignature.signature();
                        int size = typeSignature.size();

                        ConfigContainer target = signature.typeHint() == ElementType.LIST ? new ArrayConfigList(size) :
                                new LinkedConfigNode(size);
                        nodeEntry.element.setValue(target);

                        Iterator<Signature.TypedObject> typedObjectIterator = typeSignature.objects().iterator();

                        return new GraphTransformer.Node<ElementEntry, Mutable<ConfigElement>, String>(nodeEntry, new Iterator<>() {
                            private int i = 0;

                            @Override
                            public boolean hasNext() {
                                return i < size;
                            }

                            @Override
                            public Entry<String, ElementEntry> next() {
                                i++;

                                Signature.TypedObject typedObject = typedObjectIterator.next();
                                Type objectType = typeResolver.resolveType(typedObject.type(), null);
                                SignatureMatcher thisMatcher = signatureMatcherSource.matcherFor(objectType, null);

                                return Entry.of(typedObject.name(), new ElementEntry(objectType, typedObject.value(),
                                        thisMatcher));
                            }
                        }, new GraphTransformer.Output<>(nodeEntry.element, (key, value, circular) -> {
                            if (key == null) {
                                target.asList().add(value.getValue());
                            }
                            else {
                                target.asNode().put(key, value.getValue());
                            }
                        }));
                    }, potentialContainer -> typeHinter.getHint(potentialContainer.type) != ElementType.SCALAR,
                    scalar -> new MutableObject<>(new ConfigPrimitive(scalar.object)),
                    entry -> entry.object).getValue();
        }
        catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    private record ElementEntry(Type type, Object object, SignatureMatcher signatureMatcher,
            Mutable<ConfigElement> element) {
        private ElementEntry(Type type, Object object, SignatureMatcher signatureMatcher) {
            this(type, object, signatureMatcher, new MutableObject<>(null));
        }
    }

    private record ClassEntry(Type type, ConfigElement element, SignatureMatcher signatureMatcher,
            Mutable<Object> reference) {
        private ClassEntry(Type type, ConfigElement element, SignatureMatcher signatureMatcher) {
            this(type, element, signatureMatcher, new MutableObject<>(null));
        }
    }
}