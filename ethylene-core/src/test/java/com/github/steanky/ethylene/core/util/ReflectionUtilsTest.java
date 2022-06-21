package com.github.steanky.ethylene.core.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {
    private static final String classType = null;
    private static final List<String> parameterizedType = null;
    private static final List<? extends String> upperBound = null;
    private static final List<? super String> lowerBound = null;
    private static final List<String>[] genericArray = null;

    private static final List<?> wildcard = null;

    @SuppressWarnings("rawtypes")
    private static final List raw = null;

    private static final List<? extends List<String>> nested = null;
    private static final Map<String, Boolean>[][][][] deepGenericArray = null;

    private static class UnsupportedCustomType implements Type { }

    private static <T extends String> T uselessMethod() {
        return null;
    }

    private static <A extends String, B extends A, C extends B, D extends C, E extends D> E[][][][][] cursed() {
        return null;
    }

    @Nested
    class UnderlyingClass {
        @Test
        void classType() {
            assertEquals(String.class, ReflectionUtils.getUnderlyingClass("".getClass()));
        }

        @Test
        void parameterizedType() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("parameterizedType").getGenericType();
            assertEquals(List.class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void upperBoundedWildcard() throws NoSuchFieldException {
            Type type = ((ParameterizedType)ReflectionUtilsTest.class.getDeclaredField("upperBound").getGenericType())
                    .getActualTypeArguments()[0];
            assertEquals(String.class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void lowerBoundedWildcard() throws NoSuchFieldException {
            Type type = ((ParameterizedType)ReflectionUtilsTest.class.getDeclaredField("lowerBound").getGenericType())
                    .getActualTypeArguments()[0];
            assertEquals(Object.class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void genericArray() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("genericArray").getGenericType();
            assertEquals(List[].class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void typeVariable() throws NoSuchMethodException {
            Type type = ReflectionUtilsTest.class.getDeclaredMethod("uselessMethod").getGenericReturnType();
            assertEquals(String.class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void wildcard() throws NoSuchFieldException {
            Type type = ((ParameterizedType)ReflectionUtilsTest.class.getDeclaredField("wildcard").getGenericType())
                    .getActualTypeArguments()[0];
            assertEquals(Object.class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void cursedReturnType() throws NoSuchMethodException {
            Type type = ReflectionUtilsTest.class.getDeclaredMethod("cursed").getGenericReturnType();
            assertEquals(String[][][][][].class, ReflectionUtils.getUnderlyingClass(type));
        }

        @Test
        void throwsWhenTypeUnsupported() {
            assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getUnderlyingClass(
                    new UnsupportedCustomType()));
        }
    }

    @Nested
    class GetGenericParameter {
        @Test
        void parameterizedType() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("parameterizedType").getGenericType();
            assertEquals(String.class, ReflectionUtils.getGenericParameter(type, 0));
        }

        @Test
        void rawType() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("raw").getGenericType();
            assertEquals(Object.class, ReflectionUtils.getGenericParameter(type, 0));
        }

        @Test
        void wildcardType() throws NoSuchFieldException {
            Type type = ((ParameterizedType)ReflectionUtilsTest.class.getDeclaredField("nested").getGenericType())
                    .getActualTypeArguments()[0];
            assertEquals(String.class, ReflectionUtils.getGenericParameter(type, 0));
        }

        @Test
        void classType() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("classType").getGenericType();
            assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getGenericParameter(type, 0));
        }

        @Test
        void genericArray() throws NoSuchFieldException {
            Type type = ReflectionUtilsTest.class.getDeclaredField("deepGenericArray").getGenericType();
            assertEquals(Boolean.class, ReflectionUtils.getGenericParameter(type, 1));
        }
    }
}