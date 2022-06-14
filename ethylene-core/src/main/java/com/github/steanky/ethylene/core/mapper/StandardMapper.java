package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class StandardMapper implements ObjectMapper {
    protected static class Output {
        protected final Object[] data;
        protected final ObjectConstructor constructor;
        protected int size = 0;

        protected Output(@NotNull Object[] data, @NotNull ObjectConstructor constructor) {
            this.data = data;
            this.constructor = constructor;
        }

        Output(@NotNull ObjectConstructor signature) {
            this(new Object[signature.spec().length], signature);
        }

        protected void put(int index, Object data) {
            this.data[index] = data;
            size++;
        }

        protected boolean isFinished() {
            return size == data.length;
        }
    }

    protected record Node(@NotNull Type type, @NotNull ConfigElement element, @NotNull Output output) {}

    protected ConstructorSource constructorSource;

    public StandardMapper(@NotNull ConstructorSource constructorSource) {
        this.constructorSource = Objects.requireNonNull(constructorSource);
    }

    @Override
    public Object construct(@NotNull Token<?> token, @NotNull ConfigElement data) throws ConfigProcessException {
        Deque<Node> stack = new ArrayDeque<>();

        Type type = token.get();
        stack.push(new Node(type, data, new Output(constructorSource.build(type, data))));

        while(!stack.isEmpty()) {
            Node current = stack.pop();


        }

        return null;
    }

    @Override
    public @NotNull ConfigElement deconstruct(@NotNull Token<?> token, Object data) throws ConfigProcessException {
        return null;
    }

    protected void processNode(@NotNull Deque<Node> stack, Node node) {

    }
}
