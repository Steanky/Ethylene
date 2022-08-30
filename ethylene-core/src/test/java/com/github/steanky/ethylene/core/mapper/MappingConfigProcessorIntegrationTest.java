package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MappingConfigProcessorIntegrationTest {
    private final MappingConfigProcessor<List<String>> listProcessor;

    public MappingConfigProcessorIntegrationTest() {
        TypeHinter typeHinter = new BasicTypeHinter();
        BasicTypeResolver typeResolver = new BasicTypeResolver();
        typeResolver.registerTypeImplementation(Collection.class, ArrayList.class);

        this.listProcessor = new MappingConfigProcessor<>(new Token<>() {}, new BasicTypeFactorySource(typeHinter,
                typeResolver, false, false), typeHinter);
    }

    @Test
    void basicStringList() {
        List<String> stringList = assertDoesNotThrow(() -> listProcessor.dataFromElement(ConfigList.of("a",
                "b", "c")));

        assertLinesMatch(List.of("a", "b", "c"), stringList);
    }
}