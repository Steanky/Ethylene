package com.github.steanky.ethylene.codec.json;

import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * Provides support for the JSON format.
 */
public class JsonCodec extends AbstractConfigCodec {
    /**
     * The default {@link Gson} instance used to read and write data.
     */
    public static final Gson DEFAULT_GSON = new Gson();

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final Gson gson;

    /**
     * Creates a new JsonCodec using the provided {@link Gson} to read and write data.
     * @param gson the Gson instance to use
     * @throws NullPointerException if gson is null
     */
    public JsonCodec(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson);
    }

    /**
     * Creates a new JsonCodec using the default {@link Gson} ({@link JsonCodec#DEFAULT_GSON}) to read and write data.
     */
    public JsonCodec() {
        this.gson = DEFAULT_GSON;
    }

    @Override
    protected @NotNull Map<String, Object> readMap(@NotNull InputStream input) throws IOException {
        try(InputStreamReader reader = new InputStreamReader(input)) {
            return gson.fromJson(reader, MAP_TYPE);
        }
        catch (JsonIOException | JsonSyntaxException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected void writeMap(@NotNull Map<String, Object> mappings, @NotNull OutputStream output) throws IOException {
        try(OutputStreamWriter writer = new OutputStreamWriter(output)) {
            gson.toJson(mappings, writer);
        }
        catch (JsonIOException exception) {
            throw new IOException(exception);
        }
    }
}
