package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class HjsonCodec extends AbstractConfigCodec {
    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return JsonValue.readHjson(new BufferedReader(new InputStreamReader(input)));
        }
        catch (ParseException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        ((JsonObject) object).writeTo(new BufferedWriter(new OutputStreamWriter(output)));
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

        throw new IllegalArgumentException("Invalid element type: " + element.getClass().getName());
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

        throw new IllegalArgumentException("Invalid JsonValue type: " + object.getClass().getName());
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
}