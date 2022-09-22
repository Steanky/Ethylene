package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.AbstractConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.collection.Entry;
import org.hjson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Provides support for HJSON.
 */
public class HjsonCodec extends AbstractConfigCodec {
    private static final String NAME = "HJSON";
    private static final String PREFERRED_EXTENSION = "hjson";
    private static final Set<String> EXTENSIONS = Set.of(PREFERRED_EXTENSION);
    private static final int ENCODE_OPTIONS = Graph.Options.TRACK_REFERENCES;
    private static final int DECODE_OPTIONS = Graph.Options.NONE;
    private final HjsonOptions readOptions;
    private final HjsonOptions writeOptions;

    /**
     * Creates a new HjsonCodec using default values.
     */
    public HjsonCodec() {
        this(new HjsonOptions(), new HjsonOptions());
    }

    /**
     * Creates a new HjsonCodec instance using the given {@link HjsonOptions} for reading and writing.
     *
     * @param readOptions  the options used when reading
     * @param writeOptions the options used when writing
     */
    public HjsonCodec(@NotNull HjsonOptions readOptions, @NotNull HjsonOptions writeOptions) {
        super(ENCODE_OPTIONS, DECODE_OPTIONS);
        this.readOptions = Objects.requireNonNull(readOptions);
        this.writeOptions = Objects.requireNonNull(writeOptions);
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return JsonValue.readHjson(new InputStreamReader(input), readOptions);
        } catch (ParseException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected @NotNull Graph.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        if (target instanceof JsonObject object) {
            return Graph.node(new Iterator<>() {
                private final Iterator<JsonObject.Member> iterator = object.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    JsonObject.Member next = iterator.next();
                    return Entry.of(next.getName(), next.getValue());
                }
            }, makeDecodeMap(object.size()));
        } else if (target instanceof JsonArray array) {
            return Graph.node(new Iterator<>() {
                private final Iterator<JsonValue> backing = array.iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    return Entry.of(null, backing.next());
                }
            }, makeDecodeCollection(array.size()));
        }

        return super.makeDecodeNode(target);
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        if (object instanceof JsonValue value) {
            if (value.isNull()) {
                return ConfigPrimitive.of(null);
            } else if (value.isBoolean()) {
                return ConfigPrimitive.of(value.asBoolean());
            } else if (value.isNumber()) {
                return ConfigPrimitive.of(value.asDouble());
            } else if (value.isString()) {
                return ConfigPrimitive.of(value.asString());
            }
        }

        return super.deserializeObject(object);
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
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof JsonArray || input instanceof JsonObject;
    }

    @Override
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        if (element instanceof ConfigPrimitive primitive) {
            if (primitive.isNull()) {
                return JsonValue.NULL;
            } else if (primitive.isBoolean()) {
                return JsonValue.valueOf(primitive.asBoolean());
            } else if (primitive.isNumber()) {
                return JsonValue.valueOf(primitive.asNumber().doubleValue());
            } else if (primitive.isString()) {
                return JsonValue.valueOf(primitive.asString());
            }
        }

        return super.serializeElement(element);
    }

    @Override
    protected @NotNull Graph.Output<Object, String> makeEncodeMap(int size) {
        JsonObject object = new JsonObject();
        return Graph.output(object, (k, v, b) -> object.add(k, (JsonValue) v));
    }

    @Override
    protected @NotNull Graph.Output<Object, String> makeEncodeCollection(int size) {
        JsonArray array = new JsonArray();
        return Graph.output(array, (k, v, b) -> array.add((JsonValue) v));
    }

    @Override
    public @Unmodifiable @NotNull Set<String> getPreferredExtensions() {
        return EXTENSIONS;
    }

    @Override
    public @NotNull String getPreferredExtension() {
        return PREFERRED_EXTENSION;
    }

    @Override
    public @NotNull String getName() {
        return NAME;
    }
}