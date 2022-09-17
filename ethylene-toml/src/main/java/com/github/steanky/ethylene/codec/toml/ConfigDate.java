package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Objects;

import com.github.steanky.ethylene.core.ConfigPrimitive;

/**
 * Represents a TOML date. This is a scalar whose object type is {@link Temporal}.
 * @see ConfigElement
 * @see ConfigPrimitive
 */
@SuppressWarnings("ClassCanBeRecord")
public class ConfigDate implements ConfigElement {
    private final Temporal temporal;

    /**
     * Construct a new ConfigDate wrapping the provided {@link Temporal}.
     *
     * @param temporal the date to wrap
     * @throws NullPointerException if date is null
     */
    public ConfigDate(@NotNull Temporal temporal) {
        this.temporal = Objects.requireNonNull(temporal);
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public @NotNull String asString() {
        //override asString to avoid requiring the user to handle Date objects directly
        return temporal.toString();
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public @NotNull Object asScalar() {
        return temporal;
    }

    @Override
    public ElementType type() {
        return ElementType.SCALAR;
    }

    /**
     * Gets the {@link Temporal} wrapped by this ConfigDate object.
     *
     * @return the wrapped Temporal
     */
    public @NotNull Temporal getTemporal() {
        return temporal;
    }
}
