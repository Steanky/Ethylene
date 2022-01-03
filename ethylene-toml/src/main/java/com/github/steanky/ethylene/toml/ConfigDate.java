package com.github.steanky.ethylene.toml;

import com.github.steanky.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Objects;

/**
 * A ConfigElement wrapping a date, required since dates have first-class support in TOML.
 */
public class ConfigDate implements ConfigElement {
    private Date date;

    /**
     * Construct a new ConfigDate wrapping the provided {@link Date}.
     * @param date the date to wrap
     * @throws NullPointerException if date is null
     */
    public ConfigDate(@NotNull Date date) {
        this.date = Objects.requireNonNull(date);
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public @NotNull Object asObject() {
        return date;
    }

    /**
     * Sets the {@link Date} wrapped by this ConfigDate object.
     * @param date the date to set
     * @throws NullPointerException if date is null
     */
    public void setDate(@NotNull Date date) {
        this.date = Objects.requireNonNull(date);
    }

    /**
     * Gets the date wrapped by this ConfigDate object.
     * @return the wrapped date
     */
    public @NotNull Date getDate() {
        return date;
    }
}
