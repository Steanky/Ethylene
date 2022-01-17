package com.github.steanky.ethylene.core.databind;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeTokenTest {
    @Test
    void test() {
        TypeToken<List<String>> typeToken = new TypeToken<>() {};
    }
}