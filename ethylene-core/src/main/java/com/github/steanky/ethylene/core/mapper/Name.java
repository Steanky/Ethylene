package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Name {
    @NotNull String value();
}
