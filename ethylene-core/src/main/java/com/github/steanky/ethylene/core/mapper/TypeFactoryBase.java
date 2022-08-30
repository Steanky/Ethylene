package com.github.steanky.ethylene.core.mapper;

public abstract class TypeFactoryBase implements TypeFactory {
    protected static void validateArguments(int signatureLength, int configLength, int objectLength) {
        if (signatureLength != configLength || signatureLength != objectLength) {
            throw new MapperException("mismatched number of arguments, signature expected " + signatureLength);
        }
    }
}
