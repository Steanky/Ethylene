package com.github.steanky.ethylene.core.mapper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenTest {
    //fields retain generic type information, objects cannot, so this is a convenient way to do type comparison
    private static final List<String> generic = null;
    private static final List<String>[] genericArray = null;
    private static final List<? extends String> upperBoundedGeneric = null;
    private static final List<? super String> lowerBoundedGeneric = null;

    static class Token2<T, Integer> extends Token<T> {

    }

    @Test
    void multiTypeParameterSubclassThrows() {
        assertThrows(IllegalStateException.class, () -> new Token2<>() {});
    }

    @Test
    void classType() {
        Token<String> string = new Token<>() {};
        assertEquals(String.class, string.get());
    }

    @Test
    void arrayType() {
        Token<String[]> stringArray = new Token<>() {};
        assertEquals(String[].class, stringArray.get());
    }

    @Test
    void genericType() throws NoSuchFieldException {
        Field field = TokenTest.class.getDeclaredField("generic");
        Type type = field.getGenericType();

        Token<List<String>> stringList = new Token<>() {};
        assertEquals(type, stringList.get());
    }

    @Test
    void genericArray() throws NoSuchFieldException {
        Field field = TokenTest.class.getDeclaredField("genericArray");
        Type type = field.getGenericType();

        Token<List<String>[]> stringListArray = new Token<>() {};
        assertEquals(type, stringListArray.get());
    }

    @Test
    void upperBoundedGeneric() throws NoSuchFieldException {
        Field field = TokenTest.class.getDeclaredField("upperBoundedGeneric");
        Type type = field.getGenericType();

        Token<List<? extends String>> upperBoundedStringListArray = new Token<>() {};
        assertEquals(type, upperBoundedStringListArray.get());
    }

    @Test
    void lowerBoundedGeneric() throws NoSuchFieldException {
        Field field = TokenTest.class.getDeclaredField("lowerBoundedGeneric");
        Type type = field.getGenericType();

        Token<List<? super String>> lowerBoundedStringListArray = new Token<>() {};
        assertEquals(type, lowerBoundedStringListArray.get());
    }

    @Test
    void parameterlessConstructionThrowsException() {
        //noinspection rawtypes
        assertThrows(IllegalStateException.class, () -> new Token() {});
    }
}