package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
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

        //first, see if we can serialize the root element
        ScalarMapper.Result result = scalarMapper.convertScalar(root, element);
        if(result.successful()) {
            return (T) result.value();
        }

        ObjectBuilder[] objectBuilders = builderResolver.forType(token.get());

        for(ObjectBuilder builder : objectBuilders) {

        }

        return null;
    }

    @Override
    public @NotNull ConfigElement elementFromData(T o) throws ConfigProcessException {
        return null;
    }
}
