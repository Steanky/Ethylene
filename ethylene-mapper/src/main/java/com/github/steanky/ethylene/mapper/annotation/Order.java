package com.github.steanky.ethylene.mapper.annotation;

import java.lang.annotation.*;

/**
 * Specifies the order in which constructor fields should be read from, so that they correspond to the order of the
 * actual constructor parameters. Smaller values come before larger ones.
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {
    /**
     * The value of this annotation, which may be any integer.
     *
     * @return the value of this annotation
     */
    int value();
}
