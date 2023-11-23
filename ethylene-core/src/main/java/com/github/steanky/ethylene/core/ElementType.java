package com.github.steanky.ethylene.core;

/**
 * Represents a specific kind of {@link ConfigElement}.
 */
public enum ElementType {
    /**
     * A map-like {@link ConfigElement}, with String keys and ConfigElement values. Order is not considered significant
     * when determining equality between node types.
     */
    NODE,

    /**
     * A list-like {@link ConfigElement}, with an indexed arrangement of {@link ConfigElement} values whose order is
     * considered significant.
     */
    LIST,

    /**
     * A value-like {@link ConfigElement}, usually (but not always) some kind of primitive value, or a string. Does not
     * contain other ConfigElements.
     */
    SCALAR;

    /**
     * Determines if this type is a scalar.
     *
     * @return true if this type is a scalar, false otherwise
     */
    public boolean isScalar() {
        return this == SCALAR;
    }

    /**
     * Determines if this type is a list.
     *
     * @return true if this type is a list, false otherwise
     */
    public boolean isList() {
        return this == LIST;
    }

    /**
     * Determines if this type is a node.
     *
     * @return true if this type is a node, false otherwise
     */
    public boolean isNode() {
        return this == NODE;
    }

    /**
     * Determines if this type is a container (which may contain other {@link ConfigElement}s).
     *
     * @return true if this type is a container, false otherwise
     */
    public boolean isContainer() {
        return this == NODE || this == LIST;
    }
}
