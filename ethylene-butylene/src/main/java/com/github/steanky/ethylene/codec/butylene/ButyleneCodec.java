package com.github.steanky.ethylene.codec.butylene;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;

public class ButyleneCodec implements ConfigCodec {
    private static final String EXTENSION = "butylene";
    private static final Set<String> EXTENSIONS = Set.of(EXTENSION);

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {

    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        return null;
    }

    @Override
    public @Unmodifiable @NotNull Set<String> getPreferredExtensions() {
        return EXTENSIONS;
    }

    @Override
    public @NotNull String getPreferredExtension() {
        return EXTENSION;
    }

    @Override
    public @NotNull String getName() {
        return "Butylene";
    }

    @Override
    public @NotNull Set<ElementType> supportedTopLevelTypes() {
        return EnumSet.allOf(ElementType.class);
    }

    public static class Builder {

    }
}
