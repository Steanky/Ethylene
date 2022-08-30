package com.github.steanky.ethylene.core.mapper;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicTypeResolverTest {
    private final BasicTypeResolver basicTypeResolver;

    public BasicTypeResolverTest() {
        basicTypeResolver = new BasicTypeResolver();
    }

    @Test
    void collectionTest() {
        basicTypeResolver.registerTypeImplementation(Collection.class, ArrayList.class);
        assertEquals(ArrayList.class, basicTypeResolver.resolveType(Collection.class));
    }

    @Test
    void listTest() {
        basicTypeResolver.registerTypeImplementation(Collection.class, ArrayList.class);
        assertEquals(ArrayList.class, basicTypeResolver.resolveType(List.class));
    }

    @Test
    void collectionAndSetTest() {
        basicTypeResolver.registerTypeImplementation(Collection.class, ArrayList.class);
        basicTypeResolver.registerTypeImplementation(Set.class, HashSet.class);

        for (int i = 0; i < 10; i++) {
            assertEquals(ArrayList.class, basicTypeResolver.resolveType(Collection.class));
            assertEquals(HashSet.class, basicTypeResolver.resolveType(Set.class));
        }
    }
}