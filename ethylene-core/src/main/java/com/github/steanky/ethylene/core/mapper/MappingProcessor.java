package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
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

        ObjectBuilder object = GraphTransformer.process(rootNode, info -> {
            //info.element may be either a Node or List
            ObjectBuilder newBuilder = builderResolver.forType(info.type, info.element.asContainer());
            GraphTransformer.Output<ObjectBuilder, String> output = new GraphTransformer.Output<>(newBuilder,
                    (k, v) -> newBuilder.appendParameter(v));

            return new GraphTransformer.Node<>(info, makeIterable(info, newBuilder), output);
        }, info -> info.element.isContainer(), scalarInfo -> new SimpleObjectBuilder(scalarMapper.convertScalar(
                scalarInfo.type, scalarInfo.element)));

        return (T) object.build();
    }

    private Iterable<Entry<String, NodeInfo>> makeIterable(NodeInfo info, ObjectBuilder builder) {
        ConfigContainer container = info.element.asContainer();

        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Entry<String, NodeInfo> next() {
                return null;
            }
        };
    }

    @Override
    public @NotNull ConfigElement elementFromData(T o) throws ConfigProcessException {
        return null;
    }
}
