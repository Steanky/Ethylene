package com.github.steanky.ethylene.mapper.annotation;

import java.lang.annotation.*;
import com.github.steanky.ethylene.mapper.signature.Signature;

/**
 * Specifies the priority of a constructor. This will be the priority of the {@link Signature} created from the
 * constructor. Larger values have a higher priority, and will be checked first. When there is ambiguity, constructors
 * with a higher priority will be called before those with a lower priority.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Priority {
    /**
     * The value of this annotation, which may be any integer.
     *
     * @return the value of this annotation
     */
    int value();
}
