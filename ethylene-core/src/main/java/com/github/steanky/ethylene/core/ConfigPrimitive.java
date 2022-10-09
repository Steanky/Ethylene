package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class represents a "primitive" type. A type is considered "primitive" if and only if it subclasses
 * {@link String}, {@link Number}, {@link Boolean}, {@link Character}, or is a null value. Therefore, all Java
 * primitives as well as String are compatible.
 */
public final class ConfigPrimitive implements ConfigElement {
    /**
     * The shared {@link ConfigPrimitive} containing null.
     */
    public static final ConfigPrimitive NULL = new ConfigPrimitive(null);

    private final Object object;

    private ConfigPrimitive(@Nullable Object object) {
        this.object = validateType(object);
    }

    /**
     * Returns a new ConfigPrimitive instance wrapping the provided {@link Object}. The object may only subclass one of
     * a number of restricted types; otherwise, an {@link IllegalArgumentException} will be thrown.
     *
     * @param object the object to wrap
     * @throws IllegalArgumentException if the provided object is a type other than a String, Number, Boolean, or
     *                                  Character and is not null
     * @return a ConfigPrimitive instance
     */
    public static @NotNull ConfigPrimitive of(@Nullable Object object) {
        if (object == null) {
            return NULL;
        }

        return new ConfigPrimitive(object);
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

    private static Object validateType(Object object) {
        if (!isPrimitive(object)) {
            throw new IllegalArgumentException("Object " + object + " not a valid type for ConfigPrimitive");
        }

        return object;
    }

    private static <TReturn> TReturn convert(Object object, Class<TReturn> classType) {
        if (classType.isInstance(object)) {
            return classType.cast(object);
        }

        throw new IllegalStateException("Element may not be converted to " + classType.getSimpleName());
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
        return convert(object, Boolean.class);
    }

    @Override
    public boolean isNumber() {
        return object instanceof Number;
    }

    @Override
    public @NotNull Number asNumber() {
        return convert(object, Number.class);
    }

    @Override
    public boolean isString() {
        return object instanceof String || object instanceof Character;
    }

    @Override
    public @NotNull String asString() {
        if (object instanceof Character character) {
            //don't distinguish between char and string
            return character.toString();
        }

        return convert(object, String.class);
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
        if (this == obj) {
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

    @Override
    public String toString() {
        return Objects.toString(object);
    }
}