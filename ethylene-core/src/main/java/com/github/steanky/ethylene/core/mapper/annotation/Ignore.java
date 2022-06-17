package com.github.steanky.ethylene.core.mapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE})
public @interface Ignore { }
