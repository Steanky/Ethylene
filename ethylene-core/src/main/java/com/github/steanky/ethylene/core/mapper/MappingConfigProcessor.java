package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.mapper.signature.OrderedSignature;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.TypeSignatureMatcher;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private final Token<T> token;
    private final TypeSignatureMatcher.Source typeFactorySource;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull TypeSignatureMatcher.Source typeFactorySource) {
        this.token = Objects.requireNonNull(token);
        this.typeFactorySource = Objects.requireNonNull(typeFactorySource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Type rootType = token.get();
            TypeSignatureMatcher rootFactory = typeFactorySource.matcherFor(rootType);
            ClassEntry rootEntry = new ClassEntry(rootType, element, rootFactory);

            Reference reference = GraphTransformer.process(rootEntry, nodeEntry -> {
                        OrderedSignature orderedSignature = nodeEntry.signatureMatcher.signature(nodeEntry.element);

                        Signature signature = orderedSignature.signature();
                        int signatureSize = orderedSignature.size();

                        Iterator<ConfigElement> elementIterator = orderedSignature.elementIterable().iterator();
                        Iterator<Entry<String, Type>> typeEntryIterator = signature.types().iterator();

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
                                TypeSignatureMatcher nextMatcher = typeFactorySource.matcherFor(nextType);

                                return Entry.of(null, new ClassEntry(nextType, nextElement, nextMatcher));
                            }
                        }, new GraphTransformer.Output<>(nodeEntry.reference, new BiConsumer<>() {
                            private int i = 0;

                            @Override
                            public void accept(Object key, Reference value) {
                                args[i++] = value.ref;

                                if (i == args.length) {
                                    nodeEntry.reference.ref = signature.makeObject(args);
                                }
                            }
                        }));
                    }, potentialContainer -> potentialContainer.element.isContainer(),
                    scalar -> new Reference(scalar.element.asScalar()));
            return (T) reference.ref;
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T t) throws ConfigProcessException {
        return null;
    }

    private record ClassEntry(Type type, ConfigElement element, TypeSignatureMatcher signatureMatcher, Reference reference) {
        private ClassEntry(Type type, ConfigElement configElement, TypeSignatureMatcher typeSignatureProvider) {
            this(type, configElement, typeSignatureProvider, new Reference(null));
        }
    }

    //a simple mutable reference to an object
    private static class Reference {
        private Object ref;

        private Reference(Object ref) {
            this.ref = ref;
        }
    }
}