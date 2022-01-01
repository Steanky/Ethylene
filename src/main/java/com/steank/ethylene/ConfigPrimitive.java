package com.steank.ethylene;

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
    private ElementType type;

    /**
     * Creates a new ConfigPrimitive instance wrapping the provided {@link Object}. The object may only subclass one of
     * a number of restricted types; otherwise, an {@link IllegalArgumentException} will be thrown.
     * @param object the object to wrap
     * @throws IllegalArgumentException if the provided object is a type other than a String, Number, or Boolean, and is
     * not null
     */
    public ConfigPrimitive(@Nullable Object object) {
        this.type = getType(object);
        this.object = object;
    }

    private static ElementType getType(Object object) {
        if (object instanceof String) {
            return ElementType.STRING;
        }
        else if (object instanceof Number) {
            return ElementType.NUMBER;
        }
        else if (object instanceof Boolean) {
            return ElementType.BOOLEAN;
        }
        else if (object == null) {
            return ElementType.NULL;
        }
        else {
            throw new IllegalArgumentException("Objects of type " + object.getClass().getName() + " cannot be " +
                    "used with ConfigPrimitive");
        }
    }

    private <T> T convert(ElementType elementType, Class<T> classType) {
        if(type == elementType) {
            return classType.cast(object);
        }

        throw new IllegalStateException("Element may not be converted to " + classType.getSimpleName());
    }

    @Override
    public @NotNull String asString() {
        return convert(ElementType.STRING, String.class);
    }

    @Override
    public @NotNull Number asNumber() {
        return convert(ElementType.NUMBER, Number.class);
    }

    @Override
    public boolean asBoolean() {
        return convert(ElementType.BOOLEAN, Boolean.class);
    }

    @Override
    public @NotNull ElementType getType() {
        return type;
    }

    /**
     * Returns the object wrapped by this ConfigPrimitive.
     * @return The object wrapped by this ConfigPrimitive, which may be null if this ConfigPrimitive is ElementType.NULL
     */
    public @Nullable Object getObject() {
        return object;
    }

    /**
     * Sets the object wrapped by this ConfigPrimitive.
     * @param object the new object
     * @throws IllegalArgumentException if the provided object is not a valid type
     */
    public void setObject(@Nullable Object object) {
        this.type = getType(object);
        this.object = object;
    }
}