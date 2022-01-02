package com.github.steank.ethylene;

import com.github.steank.ethylene.collection.ConfigList;
import com.github.steank.ethylene.collection.ConfigNode;

/**
 * Used to indicate what type a {@link ConfigElement} is.
 */
public enum ElementType {
    /**
     * The node type, referring to a {@link ConfigElement} of type {@link ConfigNode}.
     */
    NODE,

    /**
     * The list type, referring to a {@link ConfigElement} of type {@link ConfigList}.
     */
    LIST,

    /**
     * The string type, referring to a {@link ConfigPrimitive} which contains a string.
     */
    STRING,

    /**
     * The number type, referring to a {@link ConfigPrimitive} which contains a number.
     */
    NUMBER,

    /**
     * The boolean type, referring to a {@link ConfigPrimitive} which contains a boolean.
     */
    BOOLEAN,

    /**
     * The null type, referring to a {@link ConfigPrimitive} which contains null.
     */
    NULL;

    /**
     * Determines if this ElementType is a node.
     * @return true if this == NODE, false otherwise
     */
    public boolean isNode() {
        return this == NODE;
    }

    /**
     * Determines if this ElementType is a list.
     * @return true if this == LIST, false otherwise
     */
    public boolean isList() {
        return this == LIST;
    }

    /**
     * Determines if this ElementType is a string.
     * @return true if this == STRING, false otherwise
     */
    public boolean isString() {
        return this == STRING;
    }

    /**
     * Determines if this ElementType is a number.
     * @return true if this == NUMBER, false otherwise
     */
    public boolean isNumber() {
        return this == NUMBER;
    }

    /**
     * Determines if this ElementType is a boolean.
     * @return true if this == BOOLEAN, false otherwise
     */
    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    /**
     * Determines if this ElementType is null.
     * @return true if this == NULL, false otherwise
     */
    public boolean isNull() {
        return this == NULL;
    }
}
