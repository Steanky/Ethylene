package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/**
 * General ConfigProcessor implementation used to convert subclasses of Number.
 *
 * @param <TNumber> the subclass of Number to convert
 */
class NumberConfigProcessor<TNumber extends Number> implements ConfigProcessor<TNumber> {
    private final Function<Number, TNumber> converter;

    /**
     * Creates a new instance of this class using the provided conversion function, which will generally be used to
     * convert a Number to a more specific implementation.
     *
     * @param converter the conversion function
     */
    NumberConfigProcessor(@NotNull Function<Number, TNumber> converter) {
        this.converter = Objects.requireNonNull(converter);
    }

    @Override
    public TNumber dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        if (!element.isNumber()) {
            throw new ConfigProcessException("Element must be a number");
        }

        return converter.apply(element.asNumber());
    }

    @Override
    public @NotNull ConfigElement elementFromData(TNumber number) {
        return new ConfigPrimitive(number);
    }
}
