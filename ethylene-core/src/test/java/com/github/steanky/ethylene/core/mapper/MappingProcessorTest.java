package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class MappingProcessorTest {
    public static class TestObject1 {
        private final String first;
        private final String second;
        private final int third;

        public TestObject1(String first, String second, int third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    private static <T> MappingProcessor<T> makePrimitiveProcessor(@NotNull Token<T> token) {
        ScalarMapper mapper = Mockito.mock(ScalarMapper.class);
        Mockito.when(mapper.convertScalar(Mockito.any(), Mockito.any())).thenAnswer(invocation ->
                ((ConfigElement)invocation.getArgument(1)).asScalar());

        return new MappingProcessor<>(token, Mockito.mock(BuilderResolver.class), mapper);
    }

    private static <T> MappingProcessor<T> makeObjectProcessor(@NotNull Token<T> token) {
        ScalarMapper mapper = Mockito.mock(ScalarMapper.class);
        Mockito.when(mapper.convertScalar(Mockito.any(), Mockito.any())).thenAnswer(invocation ->
                ((ConfigElement)invocation.getArgument(1)).asScalar());

        BuilderResolver resolver = new ConstructorBuilderResolver(null, new BasicTypeHinter());
        return new MappingProcessor<>(token, resolver, mapper);
    }

    @Test
    void rootScalarMapping() throws ConfigProcessException {
        assertEquals("string", makePrimitiveProcessor(new Token<String>() {})
                .dataFromElement(new ConfigPrimitive("string")));
        assertEquals(5, makePrimitiveProcessor(new Token<Integer>() {})
                .dataFromElement(new ConfigPrimitive(5)));
        assertEquals(5.5, makePrimitiveProcessor(new Token<Double>() {})
                .dataFromElement(new ConfigPrimitive(5.5)));
        assertEquals(5.5F, makePrimitiveProcessor(new Token<Float>() {})
                .dataFromElement(new ConfigPrimitive(5.5F)));
        assertEquals(true, makePrimitiveProcessor(new Token<Boolean>() {})
                .dataFromElement(new ConfigPrimitive(true)));
    }

    @Test
    void simpleFlatObject() throws ConfigProcessException {
        ConfigNode testObjectNode = new LinkedConfigNode(3);
        testObjectNode.put("first", new ConfigPrimitive("first_value"));
        testObjectNode.put("second", new ConfigPrimitive("second_value"));
        testObjectNode.put("third", new ConfigPrimitive(3));

        MappingProcessor<TestObject1> testObjectProcessor = makeObjectProcessor(new Token<>() {});
    }
}