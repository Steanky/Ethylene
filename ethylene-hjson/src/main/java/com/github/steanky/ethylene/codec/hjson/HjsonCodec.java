package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import org.hjson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

/**
 * Provides support for HJSON.
 */
public class HjsonCodec extends AbstractConfigCodec {
    private static final List<String> EXTENSIONS = List.of("hjson");

    private final HjsonOptions readOptions;
    private final HjsonOptions writeOptions;

    /**
     * Creates a new HjsonCodec instance using the given {@link HjsonOptions} for reading and writing.
     * @param readOptions the options used when reading
     * @param writeOptions the options used when writing
     */
    public HjsonCodec(@NotNull HjsonOptions readOptions, @NotNull HjsonOptions writeOptions) {
        this.readOptions = Objects.requireNonNull(readOptions);
        this.writeOptions = Objects.requireNonNull(writeOptions);
    }

    /**
     * Creates a new HjsonCodec using default values.
     */
    public HjsonCodec() {
        this(new HjsonOptions(), new HjsonOptions());
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return JsonValue.readHjson(new InputStreamReader(input), readOptions);
        }
        catch (ParseException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        Writer outputWriter = new OutputStreamWriter(output);
        BufferedWriter bufferedWriter = new BufferedWriter(outputWriter);
        ((JsonObject) object).writeTo(bufferedWriter, writeOptions);

        //ensure everything gets written to the OutputStream before it is closed
        bufferedWriter.flush();
        outputWriter.flush();
    }

    @Override
    protected @NotNull <TOut> Output<TOut> makeEncodeMap() {
        JsonObject object = new JsonObject();
        return new Output<>(object, (k, v) -> object.add(k, (JsonValue) v));
    }

    @Override
    protected @NotNull <TOut> Output<TOut> makeEncodeCollection() {
        JsonArray array = new JsonArray();
        return new Output<>(array, (k, v) -> array.add((JsonValue) v));
    }

    @Override
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        if(element instanceof ConfigPrimitive primitive) {
            if(primitive.isNull()) {
                return JsonValue.NULL;
            }
            else if(primitive.isBoolean()) {
                return JsonValue.valueOf(primitive.asBoolean());
            }
            else if(primitive.isNumber()) {
                return JsonValue.valueOf(primitive.asNumber().doubleValue());
            }
            else if(primitive.isString()) {
                return JsonValue.valueOf(primitive.asString());
            }
        }

        throw new IllegalArgumentException("Invalid element " + element.getClass().getName());
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        if(object == null) {
            return new ConfigPrimitive(null);
        }

        if(object instanceof JsonValue value) {
            if(value.isNull()) {
                return new ConfigPrimitive(null);
            }
            else if(value.isBoolean()) {
                return new ConfigPrimitive(value.asBoolean());
            }
            else if(value.isNumber()) {
                return new ConfigPrimitive(value.asDouble());
            }
            else if(value.isString()) {
                return new ConfigPrimitive(value.asString());
            }
        }

        throw new IllegalArgumentException("Invalid JsonValue type " + object.getClass().getName());
    }

    @Override
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof JsonArray || input instanceof JsonObject;
    }

    @Override
    protected @NotNull <TOut> Node<TOut> makeNode(@NotNull Object inputContainer,
                                                  @NotNull Supplier<Output<TOut>> mapSupplier,
                                                  @NotNull Supplier<Output<TOut>> collectionSupplier) {
        if(inputContainer instanceof JsonObject object) {
            return new Node<>(object, StreamSupport.stream(object.spliterator(), false)
                    .map(member -> new Entry<>(member.getName(), member.getValue())).iterator(), mapSupplier.get());
        }
        else if(inputContainer instanceof JsonArray array) {
            return new Node<>(array, StreamSupport.stream(array.spliterator(), false)
                    .map(element -> new Entry<>((String)null, element)).iterator(), collectionSupplier.get());
        }
        else {
            return super.makeNode(inputContainer, mapSupplier, collectionSupplier);
        }
    }

    @Override
    public @Unmodifiable @NotNull List<String> getPreferredExtensions() {
        return EXTENSIONS;
    }
}