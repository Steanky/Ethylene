package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a "primitive" type. A type is considered "primitive" if and only if it subclasses
 * {@link String}, {@link Number}, {@link Boolean}, or is a null value. Therefore, all Java primitives except for
 * char (and with the addition of String) are compatible. The exclusion of char is due to the inconsistent support for
 * primitive characters in some file formats; for example, some use single-character strings and have no concept of a
 * "character" type as it exists in Java.
 */
public class ConfigPrimitive implements ConfigElement {
    private Object object;

    /**
     * Creates a new ConfigPrimitive instance wrapping the provided {@link Object}. The object may only subclass one of
     * a number of restricted types; otherwise, an {@link IllegalArgumentException} will be thrown.
     * @param object the object to wrap
     * @throws IllegalArgumentException if the provided object is a type other than a String, Number, or Boolean, and is
     * not null
     */
    public ConfigPrimitive(@Nullable Object object) {
        this.object = validateType(object);
    }

    private static Object validateType(Object object) {
        if(!(object == null || object instanceof String || object instanceof Number || object instanceof Boolean)) {
            throw new IllegalArgumentException("Object " + object + " not a valid type for ConfigPrimitive");
        }

        return object;
    }

    private <T> T convert(Class<T> classType) {
        if(object != null && classType.isAssignableFrom(object.getClass())) {
            return classType.cast(object);
        }

        throw new IllegalStateException("Element may not be converted to " + classType.getSimpleName());
    }

    @Override
    public boolean isString() {
        return object instanceof String;
    }

    @Override
    public @NotNull String asString() {
        return convert(String.class);
    }

    @Override
    public boolean isNumber() {
        return object instanceof Number;
    }

    @Override
    public @NotNull Number asNumber() {
        return convert(Number.class);
    }

    @Override
    public boolean isBoolean() {
        return object instanceof Boolean;
    }

    @Override
    public boolean asBoolean() {
        return convert(Boolean.class);
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public Object asObject() {
        return object;
    }

    /**
     * Sets the object wrapped by this ConfigPrimitive.
     * @param object the new object
     * @throws IllegalArgumentException if the provided object is not a valid type
     */
    public void setObject(@Nullable Object object) {
        this.object = validateType(object);
    }
}