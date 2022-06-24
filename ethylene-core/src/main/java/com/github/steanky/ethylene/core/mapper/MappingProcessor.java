package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class MappingProcessor<T> implements ConfigProcessor<T> {
    public record NodeInfo(Type type, ConfigElement element) {}

    private final Token<T> token;
    private final BuilderResolver builderResolver;
    private final ScalarMapper scalarMapper;

    public MappingProcessor(@NotNull Token<T> token, @NotNull BuilderResolver builderResolver,
                            @NotNull ScalarMapper scalarMapper) {
        this.token = Objects.requireNonNull(token);
        this.builderResolver = Objects.requireNonNull(builderResolver);
        this.scalarMapper = Objects.requireNonNull(scalarMapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        Type root = token.get();
        NodeInfo rootNode = new NodeInfo(root, element);

        try {
            ObjectBuilder object = GraphTransformer.process(rootNode, info -> {
                //info.element may be either a Node or List
                ObjectBuilder newBuilder = builderResolver.forType(info.type, info.element.asContainer());
                GraphTransformer.Output<ObjectBuilder, String> output = new GraphTransformer.Output<>(newBuilder,
                        (k, v) -> newBuilder.appendParameter(v));

                return new GraphTransformer.Node<>(info, makeIterable(info, newBuilder), output);
            }, info -> info.element.isContainer(), scalarInfo -> new ScalarObjectBuilder(scalarMapper.convertScalar(
                    scalarInfo.type, scalarInfo.element)));

            return (T) object.build();
        }
        catch (MappingException mappingException) {
            throw new ConfigProcessException(mappingException);
        }
    }

    private Iterable<Entry<String, NodeInfo>> makeIterable(NodeInfo info, ObjectBuilder builder) {
        TypeHinter.TypeHint hint = builder.typeHint();
        Collection<ConfigEntry> entryCollection = info.element.asContainer().entryCollection();

        switch (hint) {
            case LIST_LIKE -> {
                Type arg = ReflectionUtils.getGenericParameter(info.type, 0);
                return () -> new Iterator<>() {
                    private final Iterator<ConfigEntry> configEntryIterator = entryCollection.iterator();

                    @Override
                    public boolean hasNext() {
                        return configEntryIterator.hasNext();
                    }

                    @Override
                    public Entry<String, NodeInfo> next() {
                        ConfigEntry entry = configEntryIterator.next();
                        return Entry.of(entry.getFirst(), new NodeInfo(arg, entry.getSecond()));
                    }
                };
            }
            case MAP_LIKE -> {
                Type key = ReflectionUtils.getGenericParameter(info.type, 0);
                Type value = ReflectionUtils.getGenericParameter(info.type, 1);

                return () -> new Iterator<>() {
                    private final Iterator<ConfigEntry> configEntryIterator = entryCollection.iterator();

                    @Override
                    public boolean hasNext() {
                        return configEntryIterator.hasNext();
                    }

                    @Override
                    public Entry<String, NodeInfo> next() {
                        return null;
                    }
                };
            }
        }

        Type[] args = builder.getArgumentTypes();
        if(args.length != entryCollection.size()) {
            throw new MappingException("Mismatched argument lengths");
        }

        return () -> new Iterator<>() {
            private final Iterator<ConfigEntry> configEntryIterator = entryCollection.iterator();
            private int i;

            @Override
            public boolean hasNext() {
                return configEntryIterator.hasNext();
            }

            @Override
            public Entry<String, NodeInfo> next() {
                ConfigEntry entry = configEntryIterator.next();
                return Entry.of(entry.getFirst(), new NodeInfo(args[i++], entry.getSecond()));
            }
        };
    }

    @Override
    public @NotNull ConfigElement elementFromData(T o) throws ConfigProcessException {
        return null;
    }
}
