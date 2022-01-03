package com.github.steanky.ethylene.bridge;

import com.github.steanky.ethylene.codec.ConfigCodec;
import com.github.steanky.ethylene.collection.ConfigNode;
import com.github.steanky.ethylene.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Reads and writes ConfigNode objects to and from an implementation-defined source. This source may be the network,
 * file IO, memory, a database, etc. Synchronicity is also implementation-specific: {@link Future} is used to provide
 * a common interface between synchronous and non-synchronous usage.
 */
public interface ConfigBridge<T extends ConfigNode> {
    /**
     * Loads a {@link ConfigNode} object from this loader's source. Asynchronous implementations may choose to load
     * ConfigNode objects on another thread; in which case this method should return immediately.
     * @return A {@link Future} object which will contain a ConfigNode object when it has finished loading, and can be
     * used to query or await the completion of the read task
     * @throws IOException if an IO error occurs
     */
    @NotNull Future<T> read() throws IOException;

    /**
     * Writes a {@link ConfigNode} object to this loader's source. This operation may occur asynchronously in some
     * implementations, in which case this method should return immediately. Write operations may not be supported on
     * all implementations.
     * @param node The node to write to the source
     * @return A {@link Future} object, which may be used to query or await the completion of the write task
     * @throws IOException if an IO error occurs
     * @throws IllegalStateException if the bridge does not support writing at this current time
     */
    @NotNull Future<Void> write(@NotNull T node) throws IOException;

    /**
     * Used to query if this ConfigBridge supports writes as well as reads. If this method returns true, any
     * implementation <i>must</i> immediately throw an IllegalStateException when {@link ConfigBridge#write(ConfigNode)}
     * is invoked.
     * @return true if this object is read-only (does not support writing); false otherwise
     */
    boolean readOnly();

    /**
     * Creates a read-only copy of this ConfigBridge implementation. The returned object will have the same behavior
     * as the invoking object, but will not support writing, and its {@link ConfigBridge#readOnly()} method will always
     * return true.
     * @return A read-only ConfigBridge
     */
    default @NotNull ConfigBridge<T> toReadOnly() {
        return new ConfigBridge<>() {
            @Override
            public @NotNull Future<T> read() throws IOException {
                return ConfigBridge.this.read();
            }

            @Override
            public @NotNull Future<Void> write(@NotNull T node) {
                throw new IllegalStateException();
            }

            @Override
            public boolean readOnly() {
                return true;
            }
        };
    }

    static @NotNull ConfigBridge<ConfigNode> fromInput(@NotNull InputStream inputStream, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);

        return new ConfigBridge<>() {
            @Override
            public @NotNull Future<ConfigNode> read() throws IOException {
                return CompletableFuture.completedFuture(codec.decodeNode(inputStream, LinkedConfigNode::new));
            }

            @Override
            public @NotNull Future<Void> write(@NotNull ConfigNode node) {
                throw new IllegalStateException();
            }

            @Override
            public boolean readOnly() {
                return true;
            }
        };
    }

    static @NotNull ConfigNode read(@NotNull InputStream inputStream, @NotNull ConfigCodec codec) throws IOException {
        try {
            return fromInput(inputStream, codec).read().get();
        }
        catch (ExecutionException | InterruptedException exception) {
            throw new IOException(exception);
        }
    }

    static @NotNull ConfigNode read(@NotNull String inputString, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(inputString);

        return read(new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8)), codec);
    }
}