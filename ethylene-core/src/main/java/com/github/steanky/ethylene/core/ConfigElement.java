package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a particular value from a configuration file. Specialized sub-interfaces include {@link ConfigNode} and
 * {@link ConfigList}. A direct implementation is {@link ConfigPrimitive}. This interface specifies methods to easily
 * convert to implementations as needed, which will all throw {@link IllegalStateException} by default.t
 */
public interface ConfigElement {
    /**
     * Determines if this ConfigElement represents a {@link ConfigNode}.
     * @return true if {@link ConfigElement#asNode()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNode() {
        return false;
    }

    /**
     * Converts this ConfigElement into a {@link ConfigNode}.
     * @return this element as a ConfigNode object
     * @throws IllegalStateException if this element is not a ConfigNode
     */
    default @NotNull ConfigNode asNode() {
        throw new IllegalStateException("Element may not be converted to ConfigNode");
    }

    /**
     * Determines if this ConfigElement represents a {@link ConfigList}.
     * @return true if {@link ConfigElement#asList()} will succeed without throwing an exception; false otherwise
     */
    default boolean isList() {
        return false;
    }

    /**
     * Converts this ConfigElement into a {@link ConfigList}.
     * @return this element as a ConfigList object
     * @throws IllegalStateException if this element is not a ConfigList
     */
    default @NotNull ConfigList asList() {
        throw new IllegalStateException("Element may not be converted to ConfigArray");
    }

    /**
     * Determines if this ConfigElement represents a string.
     * @return true if {@link ConfigElement#asString()} will succeed without throwing an exception; false otherwise
     */
    default boolean isString() {
        return false;
    }

    /**
     * Converts this ConfigElement into a string.
     * @return this element as a string
     * @throws IllegalStateException if this element is not a {@link ConfigPrimitive} containing a string
     */
    default @NotNull String asString() {
        throw new IllegalStateException("Element may not be converted to String");
    }

    /**
     * Determines if this ConfigElement represents a Number.
     * @return true if {@link ConfigElement#asNumber()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * Converts this ConfigElement into a Number.
     * @return this element as a Number
     * @throws IllegalStateException if this element cannot be converted into a Number
     */
    default @NotNull Number asNumber() {
        throw new IllegalStateException("Element may not be converted to Number");
    }

    /**
     * Determines if this ConfigElement represents a boolean.
     * @return true if {@link ConfigElement#asBoolean()} will succeed without throwing an exception; false otherwise
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Converts this ConfigElement into a boolean.
     * @return this element as a boolean
     * @throws IllegalStateException if this element cannot be converted into a boolean
     */
    default boolean asBoolean() {
        throw new IllegalStateException("Element may not be converted to boolean");
    }

    /**
     * Determines if this ConfigElement represents a null value.
     * @return true if this ConfigElement represents null, false otherwise
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Determines if this ConfigElement represents an object. This is true for {@link ConfigPrimitive} and should be
     * true for specialized, direct implementations of this interface that do not, themselves, hold on to ConfigElement
     * instances. It should be false for {@link ConfigNode} and {@link ConfigList}.
     * @return true if {@link ConfigElement#asObject()} will succeed without throwing an exception; false otherwise
     */
    default boolean isObject() {
        return false;
    }

    /**
     * Converts this ConfigElement into an object.
     * @return this element as an object
     * @throws IllegalStateException if this element cannot be converted into an object
     */
    default Object asObject() { throw new IllegalStateException("Element may not be converted to Object"); }
}