package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Objects;

/**
 * A ConfigElement wrapping a date, required since dates have first-class support in TOML.
 */
@SuppressWarnings("ClassCanBeRecord")
public class ConfigDate implements ConfigElement {
    private final Date date;

    /**
     * Construct a new ConfigDate wrapping the provided {@link Date}.
     *
     * @param date the date to wrap
     * @throws NullPointerException if date is null
     */
    public ConfigDate(@NotNull Date date) {
        this.date = Objects.requireNonNull(date);
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public @NotNull String asString() {
        //override asString to avoid requiring the user to handle Date objects directly
        return date.toString();
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public @NotNull Object asScalar() {
        return date;
    }

    @Override
    public ElementType type() {
        return ElementType.SCALAR;
    }

    /**
     * Gets the date wrapped by this ConfigDate object.
     *
     * @return the wrapped date
     */
    public @NotNull Date getDate() {
        return date;
    }
}
