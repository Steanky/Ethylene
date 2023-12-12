package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class represents a "primitive" type. A type is considered "primitive" if and only if it subclasses
 * {@link String}, {@link Number}, {@link Boolean}, {@link Character}, or is a null value. Therefore, all Java
 * primitives as well as String are compatible.
 * <p>
 * ConfigPrimitive instances are immutable. They may be obtained through calling {@link ConfigPrimitive#of(Object)} with
 * one of the above types as an argument.
 */
public final class ConfigPrimitive implements ConfigElement {
    /**
     * The shared {@link ConfigPrimitive} containing null.
     */
    public static final ConfigPrimitive NULL = new ConfigPrimitive(null);

    /**
     * The shared {@link ConfigPrimitive} containing a boolean 'true'.
     */
    public static final ConfigPrimitive TRUE = new ConfigPrimitive(true);

    /**
     * The shared {@link ConfigPrimitive} containing a boolean 'false'.
     */
    public static final ConfigPrimitive FALSE = new ConfigPrimitive(false);

    /**
     * The shared {@link ConfigPrimitive} containing an empty string.
     */
    public static final ConfigPrimitive EMPTY_STRING = new ConfigPrimitive("");

    private static final class LongCache {
        private static final long LOW = -128;
        private static final long HIGH = 127;

        private static final ConfigPrimitive[] CACHE;

        static {
            CACHE = new ConfigPrimitive[(int)(HIGH - LOW + 1)];
            int j = 0;
            for (long i = LOW; i <= HIGH; i++, j++) {
                CACHE[j] = new ConfigPrimitive(i);
            }
        }
    }

    private static final class IntegerCache {
        private static final int LOW = -128;
        private static final int HIGH = 127;

        private static final ConfigPrimitive[] CACHE;

        static {
            CACHE = new ConfigPrimitive[HIGH - LOW + 1];
            for (int i = LOW, j = 0; i <= HIGH; i++, j++) {
                CACHE[j] = new ConfigPrimitive(i);
            }
        }
    }

    private static final class CharacterCache {
        private static final int HIGH = 127;

        private static final ConfigPrimitive[] CACHE;

        static {
            CACHE = new ConfigPrimitive[HIGH + 1];
            for (int i = 0; i <= HIGH; i++) {
                CACHE[i] = new ConfigPrimitive((char)i);
            }
        }
    }

    private static final class ShortCache {
        private static final short LOW = -128;
        private static final short HIGH = 127;

        private static final ConfigPrimitive[] CACHE;

        static {
            CACHE = new ConfigPrimitive[HIGH - LOW + 1];
            for (short i = LOW, j = 0; i <= HIGH; i++, j++) {
                CACHE[j] = new ConfigPrimitive(i);
            }
        }
    }

    private static final class ByteCache {
        private static final ConfigPrimitive[] CACHE;

        static {
            CACHE = new ConfigPrimitive[256];
            int j = 0;
            for (byte i = Byte.MIN_VALUE; j < 256; i++, j++) {
                CACHE[j] = new ConfigPrimitive(i);
            }
        }
    }

    private final Object object;

    private ConfigPrimitive(@Nullable Object object) {
        this.object = object;
    }

    /**
     * Returns a new ConfigPrimitive instance wrapping the provided {@link Object}. The object may only subclass one of
     * a number of restricted types; otherwise, an {@link IllegalArgumentException} will be thrown.
     *
     * @param object the object to wrap
     * @return a ConfigPrimitive instance
     * @throws IllegalArgumentException if the provided object is a type other than a String, Number, Boolean, or
     *                                  Character and is not null
     */
    public static @NotNull ConfigPrimitive of(@Nullable Object object) {
        if (object == null) {
            return NULL;
        }

        if (object instanceof Boolean b) {
            return b ? TRUE : FALSE;
        }

        if (object instanceof Number number) {
            return ofNonNull(number);
        }

        if (object instanceof String string) {
            return ofNonNull(string);
        }

        if (object instanceof Character c) {
            return of((char)c);
        }

        throw new IllegalArgumentException(
            "Object of type " + object.getClass() + " not valid for ConfigPrimitive");
    }

    private static ConfigPrimitive ofNonNull(Number value) {
        if (value instanceof Long l) {
            return of((long)l);
        }
        else if(value instanceof Integer i) {
            return of((int)i);
        }
        else if(value instanceof Short s) {
            return of((short)s);
        }
        else if(value instanceof Byte b) {
            return of((byte)b);
        }

        //Double, Float, BigInteger?
        return new ConfigPrimitive(value);
    }

    private static ConfigPrimitive ofNonNull(String value) {
        return value.isEmpty() ? EMPTY_STRING : new ConfigPrimitive(value);
    }

    /**
     * Number specialization of {@link ConfigPrimitive#of(Object)}.
     * @param value the value from which to create a ConfigPrimitive
     * @return a ConfigPrimitive containing the given number
     */
    public static @NotNull ConfigPrimitive of(@Nullable Number value) {
        if (value == null) {
            return NULL;
        }

        return ofNonNull(value);
    }

    /**
     * String specialization of {@link ConfigPrimitive#of(Object)}.
     * @param value the value from which to create a ConfigPrimitive
     * @return a ConfigPrimitive containing the given string
     */
    public static @NotNull ConfigPrimitive of(@Nullable String value) {
        if (value == null) {
            return NULL;
        }

        return ofNonNull(value);
    }

    /**
     * Primitive boolean specialization of {@link ConfigPrimitive#of(Object)}. Avoids boxing.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Primitive long specialization of {@link ConfigPrimitive#of(Object)}. May avoid boxing for cached values.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(long value) {
        if (value >= LongCache.LOW && value <= LongCache.HIGH) {
            return LongCache.CACHE[(int)(value + (-LongCache.LOW))];
        }

        return new ConfigPrimitive(value);
    }

    /**
     * Primitive integer specialization of {@link ConfigPrimitive#of(Object)}. May avoid boxing for cached values.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(int value) {
        if (value >= IntegerCache.LOW && value <= IntegerCache.HIGH) {
            return IntegerCache.CACHE[value + (-IntegerCache.LOW)];
        }

        return new ConfigPrimitive(value);
    }

    /**
     * Primitive character specialization of {@link ConfigPrimitive#of(Object)}. May avoid boxing for cached values.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(char value) {
        if (value <= CharacterCache.HIGH) {
            return CharacterCache.CACHE[value];
        }

        return new ConfigPrimitive(value);
    }

    /**
     * Primitive short specialization of {@link ConfigPrimitive#of(Object)}. May avoid boxing for cached values.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(short value) {
        if (value >= ShortCache.LOW && value <= ShortCache.HIGH) {
            return ShortCache.CACHE[value + (-ShortCache.LOW)];
        }

        return new ConfigPrimitive(value);
    }

    /**
     * Primitive byte specialization of {@link ConfigPrimitive#of(Object)}. Avoids boxing.
     * @param value the value from which to create a {@link ConfigPrimitive}
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(byte value) {
        return ByteCache.CACHE[((int)value) + (-((int)Byte.MIN_VALUE))];
    }

    /**
     * Determines if the given object may be used to construct a {@link ConfigPrimitive}.
     *
     * @param object the object in question
     * @return true if the object is a valid type for ConfigPrimitive; false otherwise
     */
    public static boolean isPrimitive(@Nullable Object object) {
        return object == null || object instanceof String || object instanceof Number || object instanceof Boolean ||
            object instanceof Character;
    }

    private static @NotNull Object validateNonNull(Object object) {
        if (object == null) {
            throw new IllegalStateException("Cannot convert null primitive to type");
        }

        return object;
    }

    @Override
    public boolean isNull() {
        return object == null;
    }

    @Override
    public @NotNull ElementType type() {
        return ElementType.SCALAR;
    }

    @Override
    public boolean isBoolean() {
        return object instanceof Boolean;
    }

    @Override
    public boolean asBoolean() {
        if (validateNonNull(object) instanceof Boolean b) {
            return b;
        }

        throw new IllegalStateException("Element may not be converted to boolean");
    }

    @Override
    public boolean isNumber() {
        return object instanceof Number;
    }

    @Override
    public @NotNull Number asNumber() {
        if (validateNonNull(object) instanceof Number number) {
            return number;
        }

        throw new IllegalStateException("Element may not be converted to Number");
    }

    @Override
    public boolean isString() {
        return object instanceof String || object instanceof Character;
    }

    @Override
    public @NotNull String asString() {
        if (validateNonNull(object) instanceof Character character) {
            //don't distinguish between char and string
            return character.toString();
        }

        if (object instanceof String string) {
            return string;
        }

        throw new IllegalStateException("Element may not be converted to String");
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public Object asScalar() {
        return object;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof ConfigPrimitive primitive) {
            return Objects.equals(object, primitive.object);
        }

        return false;
    }

    private static final String LONG_POSTFIX = "L";
    private static final String FLOAT_POSTFIX = "F";
    private static final String SHORT_POSTFIX = "S";
    private static final String BYTE_POSTFIX = "B";

    private static String escapeAndQuote(String input) {
        StringBuilder builder = new StringBuilder(input.length() + 4);
        builder.append('\'');

        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (character == '\\' || character == '\'') {
                builder.append('\\');
            }

            builder.append(character);
        }
        builder.append('\'');
        return builder.toString();
    }

    @Override
    public String toString() {
        if (object instanceof Number) {
            if (object instanceof Long l) {
                return l + LONG_POSTFIX;
            }
            else if (object instanceof Float f) {
                return f + FLOAT_POSTFIX;
            }
            else if (object instanceof Short s) {
                return s + SHORT_POSTFIX;
            }
            else if (object instanceof Byte b) {
                return b + BYTE_POSTFIX;
            }
        }

        if (object instanceof String s) {
            return escapeAndQuote(s);
        }
        else if(object instanceof Character character) {
            return escapeAndQuote(Character.toString(character));
        }

        return Objects.toString(object);
    }
}