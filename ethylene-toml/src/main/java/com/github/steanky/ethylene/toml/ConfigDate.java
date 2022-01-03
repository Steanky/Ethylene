package com.github.steanky.ethylene.toml;

import com.github.steanky.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Objects;

public class ConfigDate implements ConfigElement {
    private Date date;

    public ConfigDate(@NotNull Date date) {
        this.date = Objects.requireNonNull(date);
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public @NotNull String asString() {
        return date.toString();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public @NotNull Object asObject() {
        return date;
    }

    public void setDate(@NotNull Date date) {
        this.date = Objects.requireNonNull(date);
    }

    public @NotNull Date getDate() {
        return date;
    }
}
