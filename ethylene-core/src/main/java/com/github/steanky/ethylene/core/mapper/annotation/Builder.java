package com.github.steanky.ethylene.core.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Builder {
    @NotNull BuilderType value();
}
