package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.CallableUtils;
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
     * implementations, in which case this method should return immediately.
     * @param node The node to write to the source
     * @return A {@link Future} object, which may be used to query or await the completion of the write task
     * @throws IOException if an IO error occurs
     */
    @NotNull Future<Void> write(@NotNull T node) throws IOException;

    /**
     * <p>Produces a ConfigBridge implementation which will use input/output streams generated from the provided
     * {@link Callable}s, using the provided {@link ConfigCodec} to read/write to these streams. Each callable will be
     * invoked once per read/write attempt, and the returned stream will be closed after the operation completes.</p>
     *
     * <p>If either callable throws an {@link IOException} when it is called (by an invocation to
     * {@link ConfigBridge#read()} or {@link ConfigBridge#write(ConfigNode)} on the returned ConfigBridge), it will be
     * rethrown when {@link Future#get()} is called. If the exception thrown is any other type, a new IOException
     * instance will be created with the actual exception set as the cause, then thrown.</p>
     *
     * <p>The produced ConfigBridge instance is synchronous.</p>
     * @param inputCallable the callable which produces {@link InputStream} instances for reading
     * @param outputCallable the callable which produces {@link OutputStream} instances for writing
     * @param codec the codec used to encode/decode from the streams
     * @param nodeSupplier a supplier used to construct the ConfigNode instances used by the returned ConfigBridge
     * @param <T> the type of {@link ConfigNode} which is returned from the bridge
     * @return a ConfigBridge implementation which reads/writes from the given input/output streams
     */
    static <T extends ConfigNode> @NotNull ConfigBridge<T> fromStreams(@NotNull Callable<InputStream> inputCallable,
                                                                       @NotNull Callable<OutputStream> outputCallable,
                                                                       @NotNull ConfigCodec codec,
                                                                       @NotNull Supplier<T> nodeSupplier) {
        Objects.requireNonNull(inputCallable);
        Objects.requireNonNull(outputCallable);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(nodeSupplier);

        return new ConfigBridge<>() {
            @Override
            public @NotNull Future<T> read() throws IOException {
                InputStream inputStream = CallableUtils.wrapException(inputCallable, IOException.class,
                        IOException::new);

                return CompletableFuture.completedFuture(codec.decodeNode(inputStream, nodeSupplier));
            }

            @Override
            public @NotNull Future<Void> write(@NotNull ConfigNode node) throws IOException {
                OutputStream outputStream = CallableUtils.wrapException(outputCallable, IOException.class,
                        IOException::new);

                codec.encodeNode(node, outputStream);
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    /**
     * <p>Produces a ConfigBridge implementation capable of reading and writing to the given file, using
     * {@link FileInputStream} and {@link FileOutputStream} objects.</p>
     *
     * <p>If the file is invalid or cannot be read from, {@link IOException}s will be thrown when attempts are made
     * to read objects from the ConfigBridge.</p>
     *
     * <p>This method uses {@link ConfigBridge#fromStreams(Callable, Callable, ConfigCodec, Supplier)} to produce its
     * ConfigBridge implementation.</p>
     * @param file the file read from and written to
     * @param codec the codec used to read/write from this file
     * @return a ConfigBridge implementation which can read/write FileConfigNode objects from and to the given file
     */
    static @NotNull ConfigBridge<FileConfigNode> fromFile(@NotNull File file, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(file);

        return fromStreams(() -> new FileInputStream(file), () -> new FileOutputStream(file), codec,
                () -> new FileConfigNode(codec));
    }

    /**
     * Utility method to read a {@link ConfigNode} from an InputStream, using the given {@link ConfigCodec} for
     * decoding. This method uses {@link ConfigBridge#fromStreams(Callable, Callable, ConfigCodec, Supplier)} to produce
     * a ConfigBridge implementation that is immediately read from.
     * @param inputStream the InputStream to read from
     * @param codec the ConfigCodec which will be used to decode the input data
     * @param nodeSupplier the supplier used to generate the returned ConfigNode object
     * @param <T> the returned type of ConfigNode
     * @return a ConfigNode object representing the decoded configuration data
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     * @throws NullPointerException if any parameters are null
     */
    static <T extends ConfigNode> @NotNull T read(@NotNull InputStream inputStream, @NotNull ConfigCodec codec,
                                                  @NotNull Supplier<T> nodeSupplier) throws IOException {
        Objects.requireNonNull(inputStream);

        try {
            return fromStreams(() -> inputStream, OutputStream::nullOutputStream, codec, nodeSupplier).read().get();
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
     * Same as {@link ConfigBridge#read(InputStream, ConfigCodec, Supplier)}, but uses a {@link ByteArrayInputStream}
     * constructed from the provided string, assuming a UTF-8 encoded string.
     * @param input the string to read from
     * @param codec the ConfigCodec which will be used to decode the input string
     * @return a ConfigNode object representing the decoded configuration data
     * @throws IOException if the string does not contain valid data for the codec
     * @throws NullPointerException if any parameters are null
     */
    static @NotNull ConfigNode read(@NotNull String input, @NotNull ConfigCodec codec) throws IOException {
        return read(new ByteArrayInputStream(Objects.requireNonNull(input).getBytes(StandardCharsets.UTF_8)), codec,
                LinkedConfigNode::new);
    }

    /**
     * Same as {@link ConfigBridge#read(InputStream, ConfigCodec, Supplier)}, but uses a {@link FileInputStream}
     * constructed from the provided file.
     * @param file the file to read from
     * @param codec the codec to use to decode the file
     * @return a FileConfigNode representing the file's configuration data
     * @throws IOException if the config data contained in the file is invalid or if an IO error occurred
     * @throws NullPointerException if any parameters are null
     */
    static @NotNull FileConfigNode read(@NotNull File file, @NotNull ConfigCodec codec) throws IOException {
        return read(new FileInputStream(Objects.requireNonNull(file)), codec, () -> new FileConfigNode(codec));
    }
}