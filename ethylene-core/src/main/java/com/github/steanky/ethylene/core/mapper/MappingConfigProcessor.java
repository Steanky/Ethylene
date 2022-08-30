package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private record ClassEntry(Type type, ConfigElement configElement, TypeFactory typeFactory, Reference reference) {
        private ClassEntry(Type type, ConfigElement configElement, TypeFactory typeFactory) {
            this(type, configElement, typeFactory, new Reference(null));
        }
    }

    private static class Reference {
        private Object ref;

        private Reference(Object ref) {
            this.ref = ref;
        }
    }

    private final Token<T> token;
    private final TypeFactory.Source typeFactorySource;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull TypeFactory.Source typeFactorySource) {
        this.token = Objects.requireNonNull(token);
        this.typeFactorySource = Objects.requireNonNull(typeFactorySource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Type rootType = token.get();
            TypeFactory rootFactory = typeFactorySource.factory(rootType);
            ClassEntry rootEntry = new ClassEntry(rootType, element, rootFactory);

            Reference reference = GraphTransformer.process(rootEntry, classEntry -> {
                Signature signature = classEntry.typeFactory.signature(classEntry.configElement);
                SignatureElement[] signatureElements = signature.elements();
                Object[] args = new Object[signatureElements.length];

                return new GraphTransformer.Node<>(classEntry, () -> new Iterator<>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < signatureElements.length;
                    }

                    @Override
                    public Entry<Object, ClassEntry> next() {
                        SignatureElement nextSignature = signatureElements[i++];
                        Type nextType = nextSignature.type();
                        ConfigElement nextElement = classEntry.configElement.getElement(nextSignature.identifier());
                        TypeFactory nextFactory = typeFactorySource.factory(nextType);

                        return Entry.of(null, new ClassEntry(nextType, nextElement, nextFactory));
                    }
                }, new GraphTransformer.Output<>(classEntry.reference, new BiConsumer<>() {
                    private int i = 0;

                    @Override
                    public void accept(Object key, Reference value) {
                        args[i++] = value.ref;

                        if (i == args.length) {
                            classEntry.reference.ref = classEntry.typeFactory.make(signature,
                                    classEntry.configElement, args);
                        }
                    }
                }));
            }, potentialContainer -> potentialContainer.configElement.isContainer(),
                    scalar -> new Reference(scalar.configElement.asScalar()));


            return (T) reference.ref;
        }
        catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T t) throws ConfigProcessException {
        return null;
    }
}
