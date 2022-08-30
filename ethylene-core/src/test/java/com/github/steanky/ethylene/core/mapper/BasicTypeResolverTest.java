package com.github.steanky.ethylene.core.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicTypeResolverTest {
    private static class NonAbstract {}

    private static class NonAbstractChild extends NonAbstract {}

    @Test
    void throwsWhenRegisterNonAbstract() {
        BasicTypeResolver basicAbstractTypeResolver = new BasicTypeResolver();
        assertThrows(MapperException.class, () ->
                basicAbstractTypeResolver.registerTypeImplementation(NonAbstract.class,
                        NonAbstractChild.class));
    }

}