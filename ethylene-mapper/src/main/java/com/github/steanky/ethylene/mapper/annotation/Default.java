package com.github.steanky.ethylene.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;
import com.github.steanky.ethylene.core.ConfigElement;

/**
 * Indicates that the annotated method should be used as a supplier of a default value for a parameter. The annotated
 * method must be public, static, and return a {@link ConfigElement}. The method will be called once, at an
 * indeterminate time, and its return value will be cached.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Default {
    /**
     * The name of the parameter for which to supply a default for.
     * @return the name of the parameter
     */
    @NotNull String value();
}
