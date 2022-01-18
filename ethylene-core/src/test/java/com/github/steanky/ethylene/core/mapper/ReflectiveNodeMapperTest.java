package com.github.steanky.ethylene.core.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectiveNodeMapperTest {
    private interface Vegetals{}

    @Test
    void test() {
        assertTrue(Vegetals.class.isInterface());
    }
}