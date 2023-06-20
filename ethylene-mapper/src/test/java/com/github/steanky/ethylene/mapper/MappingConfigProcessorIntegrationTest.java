package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.annotation.*;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureBuilderSelector;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureParameter;
import com.github.steanky.ethylene.mapper.signature.constructor.ConstructorSignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class MappingConfigProcessorIntegrationTest {
    private final TypeHinter typeHinter;
    private final BasicTypeResolver typeResolver;
    private final BasicSignatureMatcherSource source;
    private final ScalarSource scalarSource;

    private final MappingConfigProcessor<List<String>> stringListProcessor;
    private final MappingConfigProcessor<List<Object>> objectListProcessor;
    private final MappingConfigProcessor<List<List<String>>> listListStringProcessor;
    private final MappingConfigProcessor<List<Collection<String>[]>> reallyStupidProcessor;
    private final MappingConfigProcessor<CustomClass> customClassProcessor;
    private final MappingConfigProcessor<CustomNamedClass> customNamedClassProcessor;
    private final MappingConfigProcessor<Object> objectProcessor;

    public MappingConfigProcessorIntegrationTest() {
        Signature<Map.Entry<?, ?>> mapEntry = Signature.builder(new Token<Map.Entry<?, ?>>() {
                                                                }, (entry, objects) -> Map.entry(objects.get(0),
                objects.get(1)),
            (entry) -> List.of(entry.getKey(), entry.getValue()), Entry.of("key", SignatureParameter.parameter(new Token<>() {
            })), Entry.of("value", SignatureParameter.parameter(new Token<>() {
            }))).matchingTypeHints().matchingNames().build();

        typeHinter = new BasicTypeHinter(Set.of());
        typeResolver = new BasicTypeResolver(typeHinter,
            Set.of(Entry.of(ArrayList.class, Collection.class), Entry.of(HashSet.class, Set.class),
                Entry.of(HashMap.class, Map.class)));
        source = new BasicSignatureMatcherSource(typeHinter,
            new BasicSignatureBuilderSelector(ConstructorSignatureBuilder.INSTANCE, Set.of()), Set.of(mapEntry), false);
        scalarSource = new BasicScalarSource(typeHinter, Set.of());

        this.stringListProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.objectListProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.listListStringProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.reallyStupidProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.customClassProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.customNamedClassProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
        this.objectProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource);
    }

    public static class CustomClass {
        private final List<String> strings;
        private final int value;
        private final Set<Integer> intSet;

        public CustomClass(@NotNull List<String> strings, int value, @NotNull Set<Integer> intSet) {
            this.strings = strings;
            this.value = value;
            this.intSet = intSet;
        }
    }

    public static class CustomNamedClass {
        private final List<String> strings;
        private final int value;
        private final Set<Integer> intSet;

        public CustomNamedClass(@Name("strings") @NotNull List<String> strings,
            @Name("intSet") @NotNull Set<Integer> intSet, @Name("value") int value) {
            this.strings = strings;
            this.value = value;
            this.intSet = intSet;
        }
    }

    public static class ParameterlessClass {
        public ParameterlessClass() {
        }
    }

    @Widen
    @Include
    @Builder(Builder.BuilderType.FIELD)
    public static class AccessWidenedFieldClass {
        @Exclude private final String excludedField = "excluded";
        private String string;
        private boolean bool;
        private AccessWidenedFieldClass selfReference;

        private AccessWidenedFieldClass() {
        }
    }

    public record SimpleRecord(boolean value, List<String> stringList) {
    }

    @Nested
    class Lists {
        @Test
        void listToElement() throws ConfigProcessException {
            List<String> stringList = List.of("a", "b", "c");
            ConfigElement element = stringListProcessor.elementFromData(stringList);

            assertEquals(stringList,
                element.asList().stream().map(ConfigElement::asString).collect(Collectors.toList()));
        }

        @Test
        void nullArrayElements() throws ConfigProcessException {
            List<String> stringList = new ArrayList<>(3);
            stringList.add(null);
            stringList.add("string");
            stringList.add(null);

            ConfigElement element = stringListProcessor.elementFromData(stringList);
            assertEquals(stringList,
                element.asList().stream().map(ConfigElement::asScalar).collect(Collectors.toList()));
        }

        @Test
        void listListString() {
            List<List<String>> topLevel = new ArrayList<>();
            List<String> first = List.of("a", "b");
            List<String> second = List.of("c");
            topLevel.add(null);
            topLevel.add(first);
            topLevel.add(second);

            assertDoesNotThrow(() -> listListStringProcessor.elementFromData(topLevel));
        }

        @Test
        void basicStringList() {
            List<String> stringList =
                assertDoesNotThrow(() -> stringListProcessor.dataFromElement(ConfigList.of("a", "b", "c")));

            assertLinesMatch(List.of("a", "b", "c"), stringList);
        }

        @Test
        void basicObjectList() {
            List<Object> stringList =
                assertDoesNotThrow(() -> objectListProcessor.dataFromElement(ConfigList.of("a", "b", "c", 1, 2, 3)));

            assertEquals(List.of("a", "b", "c", 1, 2, 3), stringList);
        }

        @Test
        void listListProcessor() {
            List<List<String>> listListString = assertDoesNotThrow(() -> listListStringProcessor.dataFromElement(
                ConfigList.of(ConfigList.of("a", "b"), ConfigList.of("c", "d"))));

            assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), listListString);
        }

        @Test
        void reallyStupidProcessor() {
            List<Collection<String>[]> stupidString = assertDoesNotThrow(() -> reallyStupidProcessor.dataFromElement(
                ConfigList.of(ConfigList.of(ConfigList.of("a", "b", "c"), ConfigList.of("d", "e", "f")),
                    ConfigList.of(ConfigList.of("g", "h", "i"), ConfigList.of("j", "k", "l")))));

            List<String>[] stupidStringArray = new List[]{List.of("a", "b", "c"), List.of("d", "e", "f")};
            List<String>[] stupidStringArray2 = new List[]{List.of("g", "h", "i"), List.of("j", "k", "l")};

            assertEquals(2, stupidString.size());

            assertArrayEquals(stupidStringArray, stupidString.get(0));
            assertArrayEquals(stupidStringArray2, stupidString.get(1));
        }

        @Test
        void integerList() throws ConfigProcessException {
            Object integerList = objectProcessor.dataFromElement(ConfigList.of(1, 2, 3));
            assertEquals(List.of(1, 2, 3), integerList);
        }

        @SuppressWarnings("CollectionAddedToSelf")
        @Test
        void selfReferentialList() throws ConfigProcessException {
            ConfigList list = ConfigList.of("a");
            list.add(list);
            list.add(list);
            list.add(ConfigPrimitive.of(1));

            List<Object> objectList = objectListProcessor.dataFromElement(list);
            assertEquals("a", objectList.get(0));
            assertEquals(objectList, objectList.get(1));
            assertEquals(objectList, objectList.get(2));
            assertEquals(1, objectList.get(3));
        }
    }

    @Nested
    class Objects {
        @Test
        void parameterlessClass() throws ConfigProcessException {
            ConfigProcessor<ParameterlessClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ParameterlessClass parameterlessClass = processor.dataFromElement(ConfigNode.of());
            assertNotNull(parameterlessClass);

            ConfigElement element = processor.elementFromData(new ParameterlessClass());
            assertNotNull(element);
            assertTrue(element.isNode(), "element is not a node");
            assertTrue(element.asNode().isEmpty(), "element node is not empty");
        }

        @Test
        void map() throws ConfigProcessException {
            ConfigProcessor<Map<Integer, String>> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ConfigList map =
                ConfigList.of(ConfigNode.of("key", 0, "value", "first"), ConfigNode.of("key", 1, "value", "second"));

            Map<Integer, String> data = processor.dataFromElement(map);
            assertEquals("first", data.get(0));
            assertEquals("second", data.get(1));
        }

        @Test
        void customSignature() throws ConfigProcessException {
            ConfigProcessor<Map.Entry<Integer, String>> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ConfigNode node = ConfigNode.of("key", 10, "value", "string_value");
            Map.Entry<Integer, String> entry = processor.dataFromElement(node);

            assertEquals(10, entry.getKey());
            assertEquals("string_value", entry.getValue());

            ConfigElement element = processor.elementFromData(Map.entry(10, "vegetals"));
            assertTrue(element.isNode());

            assertEquals(10, element.getNumberOrThrow("key").intValue());
            assertEquals("vegetals", element.getStringOrThrow("value"));
        }

        @Test
        void recordBuilder() throws ConfigProcessException {
            ConfigProcessor<SimpleRecord> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ConfigNode dataNode = ConfigNode.of("stringList", ConfigList.of("a", "b", "c"), "value", true);

            SimpleRecord simpleRecord = processor.dataFromElement(dataNode);
            assertEquals(new SimpleRecord(true, List.of("a", "b", "c")), simpleRecord);
        }

        @Test
        void accessWidenedFieldConstructor() throws ConfigProcessException {
            ConfigProcessor<AccessWidenedFieldClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ConfigNode dataNode = ConfigNode.of("string", "value", "bool", true, "selfReference", null);
            AccessWidenedFieldClass obj = processor.dataFromElement(dataNode);

            assertTrue(obj.bool);
            assertEquals("value", obj.string);
        }

        @SuppressWarnings("CollectionAddedToSelf")
        @Test
        void selfReferentialAccessWidenedFieldConstructor() throws ConfigProcessException {
            ConfigProcessor<AccessWidenedFieldClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource);

            ConfigNode dataNode = ConfigNode.of("string", "value", "bool", true);
            dataNode.put("selfReference", dataNode);

            AccessWidenedFieldClass obj = processor.dataFromElement(dataNode);

            assertTrue(obj.bool);
            assertEquals("value", obj.string);
            assertSame(obj, obj.selfReference);
        }

        @Test
        void customObject() {
            ConfigNode node =
                ConfigNode.of("strings", ConfigList.of("a", "b", "c"), "value", 69, "intSet", ConfigList.of(1, 2, 3));
            CustomClass custom = assertDoesNotThrow(() -> customClassProcessor.dataFromElement(node));

            assertEquals(List.of("a", "b", "c"), custom.strings);
            assertEquals(69, custom.value);
            assertEquals(Set.of(1, 2, 3), custom.intSet);
        }

        @Test
        void customNamedObject() {
            ConfigNode node =
                ConfigNode.of("strings", ConfigList.of("a", "b", "c"), "value", 69, "intSet", ConfigList.of(1, 2, 3));
            CustomNamedClass custom = assertDoesNotThrow(() -> customNamedClassProcessor.dataFromElement(node));

            assertEquals(List.of("a", "b", "c"), custom.strings);
            assertEquals(69, custom.value);
            assertEquals(Set.of(1, 2, 3), custom.intSet);
        }
    }

    @Nested
    class Sets {
        public static class SetImpl extends AbstractSet<Integer> {
            private final Set<Integer> integers;

            public SetImpl(int size) {
                this.integers = new HashSet<>(size);
            }

            @Override
            public Iterator<Integer> iterator() {
                return integers.iterator();
            }

            @Override
            public int size() {
                return integers.size();
            }

            @Override
            public boolean add(Integer integer) {
                return integers.add(integer);
            }

            @Override
            public boolean remove(Object o) {
                return integers.remove(o);
            }
        }

        @Test
        void setImpl() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder()
                .withStandardSignatures()
                .withTypeImplementation(SetImpl.class, Set.class).ignoringLengths().build();

            ConfigProcessor<Set<Integer>> processor = source.processorFor(new Token<>() {});
            Set<Integer> set = processor.dataFromElement(ConfigList.of(0, 1, 2, 3));

            assertEquals(SetImpl.class, set.getClass());
            assertEquals(set, Set.of(0, 1, 2, 3));
        }

        @Test
        void wrongSetImpl() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder()
                .withStandardSignatures()
                .withTypeImplementation(SetImpl.class, Set.class).ignoringLengths().build();

            ConfigProcessor<Set<String>> processor = source.processorFor(new Token<>() {});
            assertThrows(ConfigProcessException.class, () -> processor.dataFromElement(ConfigList.of("a", "b", "c")));
        }
    }
}