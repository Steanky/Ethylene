package com.github.steanky.ethylene.mapper.annotation;

import java.lang.annotation.*;

/**
 * Specifies that a specific field should be included when building an object; or when applied to a type, specifies that
 * all fields should be included unless explicitly excluded.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Include {
}
