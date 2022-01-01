package com.steank.ethylene;

public enum ElementType {
    NODE,
    ARRAY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL;

    public boolean isNode() {
        return this == NODE;
    }

    public boolean isArray() {
        return this == ARRAY;
    }

    public boolean isString() {
        return this == STRING;
    }

    public boolean isNumber() {
        return this == NUMBER;
    }

    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    public boolean isNull() {
        return this == NULL;
    }
}
