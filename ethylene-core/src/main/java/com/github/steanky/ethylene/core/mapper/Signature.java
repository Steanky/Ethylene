package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;

public interface Signature {
    boolean matches(@NotNull List<Entry<String, Type>> arguments);
}
