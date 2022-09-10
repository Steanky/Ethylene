package com.github.steanky.ethylene.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Builder {
    enum BuilderType {
        FIELD,
        CONSTRUCTOR,
        RECORD
    }

    @NotNull BuilderType value();
}
