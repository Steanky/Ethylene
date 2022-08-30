package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class MappingConfigProcessorIntegrationTest {
    private final MappingConfigProcessor<List<String>> stringListProcessor;
    private final MappingConfigProcessor<List<Object>> objectListProcessor;
    private final MappingConfigProcessor<List<List<String>>> listListStringProcessor;
    private final MappingConfigProcessor<List<List<String>[]>> reallyStupidProcessor;

    public MappingConfigProcessorIntegrationTest() {
        TypeHinter typeHinter = new BasicTypeHinter();
        BasicTypeResolver typeResolver = new BasicTypeResolver();
        typeResolver.registerTypeImplementation(Collection.class, ArrayList.class);
        TypeFactory.Source source = new BasicTypeFactorySource(typeHinter, typeResolver, false,
                false);

        this.stringListProcessor = new MappingConfigProcessor<>(new Token<>() {}, source);
        this.objectListProcessor = new MappingConfigProcessor<>(new Token<>() {}, source);
        this.listListStringProcessor = new MappingConfigProcessor<>(new Token<>() {}, source);
        this.reallyStupidProcessor = new MappingConfigProcessor<>(new Token<>() {}, source);
    }

    @Test
    void basicStringList() {
        List<String> stringList = assertDoesNotThrow(() -> stringListProcessor.dataFromElement(ConfigList.of("a",
                "b", "c")));

        assertLinesMatch(List.of("a", "b", "c"), stringList);
    }

    @Test
    void basicObjectList() {
        List<Object> stringList = assertDoesNotThrow(() -> objectListProcessor.dataFromElement(ConfigList.of("a",
                "b", "c")));

        assertEquals(List.of("a", "b", "c"), stringList);
    }

    @Test
    void listListProcessor() {
        List<List<String>> listListString = assertDoesNotThrow(() -> listListStringProcessor
                .dataFromElement(ConfigList.of(ConfigList.of("a", "b"),
                        ConfigList.of("c", "d"))));

        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), listListString);
    }

    @Test
    void reallyStupidProcessor() {
        List<List<String>[]> stupidString = assertDoesNotThrow(() -> reallyStupidProcessor.dataFromElement(
                ConfigList.of(
                        ConfigList.of(ConfigList.of("a", "b", "c"), ConfigList.of("d", "e", "f")),
                        ConfigList.of(ConfigList.of("g", "h", "i"), ConfigList.of("j", "k", "l"))
                )
        ));

        List<String>[] stupidStringArray = new List[] { List.of("a", "b", "c"), List.of("d", "e", "f") };
        List<String>[] stupidStringArray2 = new List[] { List.of("g", "h", "i"), List.of("j", "k", "l") };

        assertEquals(2, stupidString.size());

        assertArrayEquals(stupidStringArray, stupidString.get(0));
        assertArrayEquals(stupidStringArray2, stupidString.get(1));
    }
}