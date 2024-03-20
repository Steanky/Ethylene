package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.path.ConfigPath;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.objectListProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.listListStringProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.reallyStupidProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.customClassProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.customNamedClassProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
        this.objectProcessor = new MappingConfigProcessor<>(new Token<>() {
        }, source, typeHinter, typeResolver, scalarSource, false);
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

        public record Child(double data) {}

        public record Data(int x, Child child) {}

        public record IntArrayContaining(int[] values) {

        }

        @Test
        void errorHandling() {
            ConfigProcessor<Data> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            assertThrows(ConfigProcessException.class, () -> processor.dataFromElement(ConfigElement.of("{x=10}")));
        }

        @Test
        void intArray() throws ConfigProcessException {
            ConfigProcessor<IntArrayContaining> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            IntArrayContaining containing = processor.dataFromElement(ConfigElement.of("{values=[0, 1, 2, 3, 4]}"));
            assertArrayEquals(new int[] {0, 1, 2, 3, 4}, containing.values);
        }

        @Test
        void upcastFloat() throws ConfigProcessException {
            ConfigProcessor<Data> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            Data d = processor.dataFromElement(ConfigElement.of("{x=10, child={data=10F}}"));
            assertEquals(10.0D, d.child.data);
        }

        @Test
        void parameterlessClass() throws ConfigProcessException {
            ConfigProcessor<ParameterlessClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

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
            }, source, typeHinter, typeResolver, scalarSource, false);

            ConfigList map =
                ConfigList.of(ConfigNode.of("key", 0, "value", "first"), ConfigNode.of("key", 1, "value", "second"));

            Map<Integer, String> data = processor.dataFromElement(map);
            assertEquals("first", data.get(0));
            assertEquals("second", data.get(1));
        }

        @Test
        void customSignature() throws ConfigProcessException {
            ConfigProcessor<Map.Entry<Integer, String>> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            ConfigNode node = ConfigNode.of("key", 10, "value", "string_value");
            Map.Entry<Integer, String> entry = processor.dataFromElement(node);

            assertEquals(10, entry.getKey());
            assertEquals("string_value", entry.getValue());

            ConfigElement element = processor.elementFromData(Map.entry(10, "vegetals"));
            assertTrue(element.isNode());

            assertEquals(10, element.atOrThrow(ConfigPath.of("key")).asNumberOrThrow().intValue());
            assertEquals("vegetals", element.atOrThrow(ConfigPath.of("value")).asStringOrThrow());
        }

        @Test
        void recordBuilder() throws ConfigProcessException {
            ConfigProcessor<SimpleRecord> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            ConfigNode dataNode = ConfigNode.of("stringList", ConfigList.of("a", "b", "c"), "value", true);

            SimpleRecord simpleRecord = processor.dataFromElement(dataNode);
            assertEquals(new SimpleRecord(true, List.of("a", "b", "c")), simpleRecord);
        }

        @Test
        void accessWidenedFieldConstructor() throws ConfigProcessException {
            ConfigProcessor<AccessWidenedFieldClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

            ConfigNode dataNode = ConfigNode.of("string", "value", "bool", true, "selfReference", null);
            AccessWidenedFieldClass obj = processor.dataFromElement(dataNode);

            assertTrue(obj.bool);
            assertEquals("value", obj.string);
        }

        @SuppressWarnings("CollectionAddedToSelf")
        @Test
        void selfReferentialAccessWidenedFieldConstructor() throws ConfigProcessException {
            ConfigProcessor<AccessWidenedFieldClass> processor = new MappingConfigProcessor<>(new Token<>() {
            }, source, typeHinter, typeResolver, scalarSource, false);

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
    class Maps {
        @Test
        void mapOfType() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder()
                .withStandardTypeImplementations()
                .withStandardSignatures()
                .ignoringLengths().build();


            ConfigProcessor<ConcurrentHashMap<String, CopyOnWriteArraySet<String>>> proc =
                source.processorFor(new Token<>() {});

            ConcurrentHashMap<String, CopyOnWriteArraySet<String>> empty = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, CopyOnWriteArraySet<String>> severalItems = new ConcurrentHashMap<>();
            severalItems.computeIfAbsent("item", ignored -> new CopyOnWriteArraySet<>()).add("test");

            assertEquals(ConfigList.of(), proc.elementFromData(empty));

            assertEquals(ConfigList.of(ConfigNode.of("key", "item",
                "value", ConfigList.of("test"))), proc.elementFromData(severalItems));
        }

        @Test
        void string() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder()
                .withStandardTypeImplementations()
                .withStandardSignatures()
                .ignoringLengths().build();

            ConfigProcessor<String> stringProcessor = source.processorFor(Token.ofClass(String.class));
            assertEquals(ConfigPrimitive.of("test"), stringProcessor.elementFromData("test"));
        }
    }

    @Nested
    class Defaulting {
        public record Data(String key, int value) {
            @Default("value")
            public static ConfigElement valueDefault() {
                return ConfigPrimitive.of(-1);
            }
        }

        @Default("""
            {
                key='default_key',
                value=69,
                value2=420F
            }
            """)
        public record Data2(String key, int value, float value2) {}

        public record Data3(@Default("'default_key'") String key, @Default("0") int value) {

        }

        @Test
        void defaults() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder().ignoringLengths().build();

            ConfigProcessor<Data> processor = source.processorFor(new Token<>() {});
            Data data = processor.dataFromElement(ConfigNode.of("key", "KEY"));

            assertEquals(new Data("KEY", -1), data);

            ConfigProcessor<Data2> processor2 = source.processorFor(Token.ofClass(Data2.class));
            Data2 data2 = processor2.dataFromElement(ConfigNode.of());

            assertEquals(new Data2("default_key", 69, 420F), data2);

            Data3 data3 = source.processorFor(Token.ofClass(Data3.class)).dataFromElement(ConfigNode.of());

            assertEquals(new Data3("default_key", 0), data3);
        }

        @Test
        void defaultOverrides() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder().ignoringLengths().build();

            ConfigProcessor<Data> processor = source.processorFor(new Token<>() {});
            Data data = processor.dataFromElement(ConfigNode.of("key", "KEY", "value", 69));

            assertEquals(new Data("KEY", 69), data);
        }

        @Test
        void avoidsWritingDefaults() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder().ignoringLengths().build();

            ConfigProcessor<Data> processor = source.processorFor(new Token<>() {});
            ConfigElement element = processor.elementFromData(new Data("some key", -1));

            assertEquals(ConfigNode.of("key", "some key"), element);
        }

        @Test
        void writesDefaultsWhenForced() throws ConfigProcessException {
            MappingProcessorSource source = MappingProcessorSource.builder().ignoringLengths()
                .writingDefaults().build();

            ConfigProcessor<Data> processor = source.processorFor(new Token<>() {});
            ConfigElement element = processor.elementFromData(new Data("some key", -1));

            assertEquals(ConfigNode.of("key", "some key", "value", -1), element);
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
            public @NotNull Iterator<Integer> iterator() {
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
        void wrongSetImpl() {
            MappingProcessorSource source = MappingProcessorSource.builder()
                .withStandardSignatures()
                .withTypeImplementation(SetImpl.class, Set.class).ignoringLengths().build();

            ConfigProcessor<Set<String>> processor = source.processorFor(new Token<>() {});
            assertThrows(ConfigProcessException.class, () -> processor.dataFromElement(ConfigList.of("a", "b", "c")));
        }
    }
}