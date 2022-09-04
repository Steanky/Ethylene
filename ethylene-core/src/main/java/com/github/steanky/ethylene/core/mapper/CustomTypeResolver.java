package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.mapper.signature.TypeSignatureMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface CustomTypeResolver {
    @Nullable TypeSignatureMatcher matcherFor(@NotNull Type type);
}
