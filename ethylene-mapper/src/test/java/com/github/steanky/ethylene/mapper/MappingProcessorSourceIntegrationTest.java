package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.annotation.Include;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("CollectionAddedToSelf")
class MappingProcessorSourceIntegrationTest {
    @Test
    void simpleCustomSignature() throws ConfigProcessException {
        Signature<ObjectWithCustomSignature> signature = Signature.<ObjectWithCustomSignature>builder(new Token<>() {
                                                                                                      }, (ignored,
                args) -> new ObjectWithCustomSignature((int) args[0]), object -> List.of(object.value),
            Map.entry("value", Token.INTEGER)).build();

        MappingProcessorSource source = MappingProcessorSource.builder().withCustomSignature(signature).build();
        ConfigProcessor<ObjectWithCustomSignature> processor =
            source.processorFor(Token.ofClass(ObjectWithCustomSignature.class));

        ConfigNode node = ConfigNode.of("value", 10);
        ObjectWithCustomSignature result = processor.dataFromElement(node);
        assertEquals(10, result.value);

        ConfigElement element = processor.elementFromData(result);
        assertEquals(node, element);
    }

    @Test
    void selfReferentialObject() throws ConfigProcessException {
        MappingProcessorSource source = MappingProcessorSource.builder().build();
        ConfigProcessor<SelfReferentialObject> processor =
            source.processorFor(Token.ofClass(SelfReferentialObject.class));

        ConfigNode node = ConfigNode.of();
        node.put("self", node);

        SelfReferentialObject self = processor.dataFromElement(node);
        assertSame(self, self.self);
    }

    private static class ObjectWithCustomSignature {
        private final int value;

        public ObjectWithCustomSignature(int value) {
            this.value = value;
        }
    }

    @Widen
    @com.github.steanky.ethylene.mapper.annotation.Builder(com.github.steanky.ethylene.mapper.annotation.Builder.BuilderType.FIELD)
    @Include
    private static class SelfReferentialObject {
        private final SelfReferentialObject self = null;

        private SelfReferentialObject() {

        }
    }

    @Nested
    class Builder {
        private static MappingProcessorSource standardSource() {
            return MappingProcessorSource.builder().withStandardSignatures().withStandardTypeImplementations().build();
        }

        @Test
        void boundedWildcard() throws ConfigProcessException {
            MappingProcessorSource mappingProcessor = standardSource();
            ConfigProcessor<Collection<? extends String>> processor = mappingProcessor.processorFor(new Token<>() {
            });
            ConfigList data = ConfigList.of("this", "is", "a", "test");
            Collection<? extends String> strings = processor.dataFromElement(data);

            assertLinesMatch(strings.stream().map(Function.identity()), data.stream().map(ConfigElement::asString));

            ConfigElement element = processor.elementFromData(strings);
            assertEquals(data, element);
        }

        @Test
        void wildcard() throws ConfigProcessException {
            MappingProcessorSource mappingProcessor = standardSource();
            ConfigProcessor<Collection<?>> processor = mappingProcessor.processorFor(new Token<>() {
            });
            ConfigList data = ConfigList.of(0, "string", 0.5F, 0.5D, 154566546546564L);
            Collection<?> objects = processor.dataFromElement(data);

            assertArrayEquals(data.stream().map(ConfigElement::asScalar).toArray(), objects.toArray());

            ConfigElement element = processor.elementFromData(objects);
            assertEquals(data, element);
        }

        @Test
        void privateRecord() throws ConfigProcessException {
            MappingProcessorSource source = standardSource();
            ConfigProcessor<PrivateRecord> processor = source.processorFor(Token.ofClass(PrivateRecord.class));

            ConfigNode node = ConfigNode.of("value", "test");
            PrivateRecord record = processor.dataFromElement(node);

            assertEquals("test", record.value);

            ConfigNode newNode = processor.elementFromData(record).asNode();
            assertEquals("test", newNode.getStringOrThrow("value"));
        }

        @Test
        void enums() throws ConfigProcessException {
            MappingProcessorSource source = standardSource();

            ConfigProcessor<TestEnum> processor = source.processorFor(Token.ofClass(TestEnum.class));
            TestEnum value = processor.dataFromElement(ConfigPrimitive.of("SECOND"));
            assertEquals(TestEnum.SECOND, value);

            ConfigElement element = processor.elementFromData(value);
            assertEquals("SECOND", element.asString());
        }

        @Test
        void customSignature() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder().withScalarSignature(
                ScalarSignature.of(Token.ofClass(UUID.class), element -> UUID.fromString(element.asString()), uuid ->
                    ConfigPrimitive.of(uuid.toString()))).withStandardSignatures().withStandardTypeImplementations()
                .build();

            ConfigProcessor<RecordWithCustomObjectInSignature> proc = source.processorFor(
                Token.ofClass(RecordWithCustomObjectInSignature.class));

            RecordWithCustomObjectInSignature r = proc.dataFromElement(ConfigNode.of("uuid",
                ConfigPrimitive.of("59be2a51-f5cb-4ae9-ae26-bfbd6968cf93")));

            assertNotNull(r);
            assertEquals(UUID.fromString("59be2a51-f5cb-4ae9-ae26-bfbd6968cf93"), r.uuid);
        }

        private enum TestEnum {
            FIRST, SECOND, THIRD
        }

        @Widen
        private record PrivateRecord(String value) {
        }

        public record RecordWithCustomObjectInSignature(UUID uuid) {}
    }
}