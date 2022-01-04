package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.FileConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

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

    /**
     * Utility method that produces a read-only ConfigBridge instance that reads {@link ConfigNode} instances using
     * {@link InputStream} objects produced by the given supplier. Each InputStream will be closed after it is read
     * from.
     * @param inputStreamCallable the supplier which generates InputStream objects to read from
     * @param codec the ConfigCodec to use
     * @param nodeSupplier the supplier used to construct nodes
     * @throws NullPointerException if inputStream or codec are null
     * @return a read-only ConfigBridge
     */
    static <T extends ConfigNode> @NotNull ConfigBridge<T> fromInput(@NotNull Callable<InputStream> inputStreamCallable,
                                                                     @NotNull ConfigCodec codec,
                                                                     @NotNull Supplier<T> nodeSupplier) {
        Objects.requireNonNull(inputStreamCallable);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(nodeSupplier);

        return new ConfigBridge<>() {
            @Override
            public @NotNull Future<T> read() throws IOException {
                InputStream inputStream;
                try {
                    inputStream = inputStreamCallable.call();
                }
                catch (Exception exception) {
                    if(exception instanceof IOException ioException) {
                        throw ioException;
                    }
                    else {
                        throw new IOException(exception);
                    }
                }

                return CompletableFuture.completedFuture(codec.decodeNode(inputStream, nodeSupplier));
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

    /**
     * Utility method to read a {@link ConfigNode} from an InputStream, using the given {@link ConfigCodec} for
     * decoding.
     * @param inputStream the InputStream to read from
     * @param codec the ConfigCodec which will be used to decode the input data
     * @return a ConfigNode object representing the decoded configuration data
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     * @throws NullPointerException if any parameters are null
     */
    static @NotNull ConfigNode read(@NotNull InputStream inputStream, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(inputStream);

        try {
            return fromInput(() -> inputStream, codec, LinkedConfigNode::new).read().get();
        }
        catch (ExecutionException | InterruptedException exception) {
            Throwable cause = exception.getCause();
            if(cause instanceof IOException ioException) {
                throw ioException;
            }
            else {
                throw new IOException(cause);
            }
        }
    }

    /**
     * Same as {@link ConfigBridge#read(InputStream, ConfigCodec)}, but reads directly from a string rather than an
     * InputStream.
     * @param input the string to read from
     * @param codec the ConfigCodec which will be used to decode the input string
     * @return a ConfigNode object representing the decoded configuration data
     * @throws IOException if the string does not contain valid data for the codec
     */
    static @NotNull ConfigNode read(@NotNull String input, @NotNull ConfigCodec codec) throws IOException {
        return read(new ByteArrayInputStream(Objects.requireNonNull(input).getBytes(StandardCharsets.UTF_8)), codec);
    }

    /**
     * Reads a {@link FileConfigNode} from the given file, using the provided {@link ConfigCodec}.
     * @param file the file to read from
     * @param codec the codec to use to decode the file
     * @return a FileConfigNode representing the file's configuration data
     * @throws IOException if the config data contained in the file is invalid or if an IO error occurred
     * @throws NullPointerException if file or codec are null
     */
    static @NotNull FileConfigNode read(@NotNull File file, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(file);

        ConfigBridge<FileConfigNode> bridge = fromInput(() -> new FileInputStream(file), codec, FileConfigNode::new);

        try {
            return bridge.read().get();
        }
        catch (ExecutionException | InterruptedException exception) {
            if(exception.getCause() instanceof IOException ioException) {
                //if the cause is an IOException, rethrow here
                throw ioException;
            }
            else {
                //...if we're not an IOException, wrap the exception in one
                //this is not necessary for properly written bridges and codecs
                throw new IOException(exception);
            }
        }
    }
}