package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import org.hjson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Objects;

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
    protected @NotNull GraphTransformer.Output<Object, String> makeEncodeMap(int size) {
        JsonObject object = new JsonObject();
        return new GraphTransformer.Output<>(object, (k, v) -> object.add(k, (JsonValue) v));
    }

    @Override
    protected @NotNull GraphTransformer.Output<Object, String> makeEncodeCollection(int size) {
        JsonArray array = new JsonArray();
        return new GraphTransformer.Output<>(array, (k, v) -> array.add((JsonValue) v));
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

        return super.serializeElement(element);
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
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

        return super.deserializeObject(object);
    }

    @Override
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof JsonArray || input instanceof JsonObject;
    }

    @Override
    protected @NotNull GraphTransformer.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        if(target instanceof JsonObject object) {
            return new GraphTransformer.Node<>(target, new Iterator<>() {
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
        }
        else if(target instanceof JsonArray array) {
            return new GraphTransformer.Node<>(target, new Iterator<>() {
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
    public @Unmodifiable @NotNull List<String> getPreferredExtensions() {
        return EXTENSIONS;
    }
}