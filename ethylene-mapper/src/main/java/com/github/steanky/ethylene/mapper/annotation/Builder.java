package com.github.steanky.ethylene.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Annotation a serializable class may use to specify how it would like to be built (constructed): by field assignment,
 * constructor, or record component.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Builder {
    /**
     * The value of this annotation, which must be non-null.
     *
     * @return the annotation value
     */
    @NotNull BuilderType value();

    /**
     * The method of constructing a given object.
     */
    enum BuilderType {
        /**
         * Construct this object first using a parameterless constructor, and then assign fields reflectively to build
         * it. Objects constructed in this way support circular references.
         */
        FIELD,

        /**
         * Construct this object using a specific constructor. Objects made this way do not support circular
         * references.
         */
        CONSTRUCTOR,

        /**
         * Construct this object using its record components. Not valid if the class is a non-record. Objects made this
         * way do not support circular references.
         */
        RECORD
    }
}
