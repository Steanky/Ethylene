package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.Entry;
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
import java.util.function.BiConsumer;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private final Token<T> token;
    private final SignatureMatcher.Source typeFactorySource;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull SignatureMatcher.Source typeFactorySource) {
        this.token = Objects.requireNonNull(token);
        this.typeFactorySource = Objects.requireNonNull(typeFactorySource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Type rootType = token.get();
            SignatureMatcher rootFactory = typeFactorySource.matcherFor(rootType, element);
            ClassEntry rootEntry = new ClassEntry(rootType, element, rootFactory);

            return (T) GraphTransformer.process(rootEntry, nodeEntry -> {
                        MatchingSignature matchingSignature = nodeEntry.signatureMatcher.signature(nodeEntry.element,
                                nodeEntry.type);

                        Signature signature = matchingSignature.signature();
                        int signatureSize = matchingSignature.size();

                        Iterator<ConfigElement> elementIterator = matchingSignature.elements().iterator();
                        Iterator<Entry<String, Type>> typeEntryIterator = signature.argumentTypes().iterator();

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

                                Type nextType = typeEntryIterator.next().getSecond();
                                ConfigElement nextElement = elementIterator.next();
                                SignatureMatcher nextMatcher = typeFactorySource.matcherFor(nextType, nextElement);

                                return Entry.of(null, new ClassEntry(nextType, nextElement, nextMatcher));
                            }
                        }, new GraphTransformer.Output<>(nodeEntry.reference, new BiConsumer<>() {
                            private int i = 0;

                            @Override
                            public void accept(Object key, Mutable<Object> value) {
                                args[i++] = value.getValue();

                                if (i == args.length) {
                                    nodeEntry.reference.setValue(signature.buildObject(args));
                                }
                            }
                        }));
                    }, potentialContainer -> potentialContainer.element.isContainer(),
                    scalar -> new MutableObject<>(scalar.element.asScalar())).getValue();
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T t) throws ConfigProcessException {
        return null;
    }

    private record ClassEntry(Type type, ConfigElement element, SignatureMatcher signatureMatcher,
            Mutable<Object> reference) {
        private ClassEntry(Type type, ConfigElement configElement, SignatureMatcher typeSignatureProvider) {
            this(type, configElement, typeSignatureProvider, new MutableObject<>(null));
        }
    }
}