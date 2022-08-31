package com.github.steanky.ethylene.core.mapper.signature.scalar;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import com.github.steanky.ethylene.core.mapper.signature.OrderedSignature;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.TypeSignatureMatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class ScalarTypeSignatureMatcher implements TypeSignatureMatcher {
    private final Signature signature;

    public ScalarTypeSignatureMatcher(@NotNull Signature signature) {
        this.signature = Objects.requireNonNull(signature);
    }

    @Override
    public @NotNull OrderedSignature signature(@NotNull ConfigElement providedElement) {
        TypeHinter.Hint hint = signature.type();
        if (!hint.compatible(providedElement)) {
            throw new MapperException("incompatible type for '" + hint + "': " + providedElement);
        }

        return new OrderedSignature(signature, Collections.singleton(providedElement), 1);
    }
}
