package com.github.steanky.ethylene.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Names a constructor parameter or object field. If this annotation is present, the field or parameter's name is
 * ignored and the value of this annotation is used instead. If this annotation is present on one constructor parameter,
 * it must be included on all of them.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Name {
    /**
     * The value of this annotation, which must be non-null.
     *
     * @return the value of this annotation
     */
    @NotNull String value();
}
