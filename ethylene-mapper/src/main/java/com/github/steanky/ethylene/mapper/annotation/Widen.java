package com.github.steanky.ethylene.mapper.annotation;

import java.lang.annotation.*;

/**
 * Specifies whether to suppress Java language access control for a specific type, in order to write private or final
 * fields, or construct from private constructors.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Widen {
}
