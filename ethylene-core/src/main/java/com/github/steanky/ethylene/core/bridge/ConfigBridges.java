package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.FileConfigNode;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Contains static utility methods relating to {@link ConfigBridge}. Many of these can be used to conveniently read data
 * from or write to various sources.
 */
public final class ConfigBridges {
    private ConfigBridges() {
        throw new AssertionError("Why would you try to do this?");
    }

    private static ConfigBridge fromStreamsInternal(Callable<InputStream> inputCallable,
                                                    Callable<OutputStream> outputCallable,
                                                    ConfigCodec codec) {
        return new ConfigBridge() {
            @Override
            public @NotNull CompletableFuture<ConfigElement> read() {
                return FutureUtils.callableToCompletedFuture(() -> codec.decode(inputCallable.call()));
            }

            @Override
            public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
                return FutureUtils.callableToCompletedFuture(() -> {
                    codec.encode(element, outputCallable.call());
                    return null;
                });
            }
        };
    }

    private static ConfigElement readInternal(InputStream inputStream,
                                              ConfigCodec codec) throws IOException {
        try {
            return fromStreamsInternal(() -> inputStream, OutputStream::nullOutputStream, codec).read().get();
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

    private static void writeInternal(OutputStream outputStream,
                                      ConfigElement element,
                                      ConfigCodec codec) {
        fromStreamsInternal(InputStream::nullInputStream, () -> outputStream, codec).write(element);
    }

    /**
     * <p>Produces a ConfigBridge implementation which will use input/output streams generated from the provided
     * {@link Callable}s, using the provided {@link ConfigCodec} to read/write to these streams. Each Callable will be
     * invoked once per read/write attempt, and the returned stream will be closed after the operation completes.</p>
     *
     * <p>If either Callable throws an {@link IOException} when it is called (by an invocation to
     * {@link ConfigBridge#read()} or {@link ConfigBridge#write(ConfigElement)} on the returned ConfigBridge), it will
     * be rethrown when {@link Future#get()} is called. If the exception thrown is any other type, a new IOException
     * instance will be created with the actual exception set as the cause, then thrown.</p>
     *
     * <p>The produced ConfigBridge instance is synchronous.</p>
     * @param inputCallable the callable which produces {@link InputStream} instances for reading
     * @param outputCallable the callable which produces {@link OutputStream} instances for writing
     * @param codec the codec used to encode/decode from the streams
     * @return a ConfigBridge implementation which reads/writes from the given input/output streams
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigBridge fromStreams(@NotNull Callable<InputStream> inputCallable,
                                                    @NotNull Callable<OutputStream> outputCallable,
                                                    @NotNull ConfigCodec codec) {
        Objects.requireNonNull(inputCallable);
        Objects.requireNonNull(outputCallable);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(inputCallable, outputCallable, codec);
    }

    /**
     * <p>Produces a ConfigBridge implementation capable of reading and writing to the given file path.</p>
     *
     * <p>If the file is invalid or cannot be read from, {@link IOException}s will be thrown when attempts are made to
     * read objects from the ConfigBridge.</p>
     *
     * <p>This method uses {@link ConfigBridges#fromStreams(Callable, Callable, ConfigCodec)} to produce its
     * ConfigBridge implementation.</p>
     * @param path a path pointing to the file read from and written to
     * @param codec the codec used to read/write from this file
     * @return a ConfigBridge implementation which can read/write {@link FileConfigNode} objects from and to the given
     * file
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigBridge fromPath(@NotNull Path path, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(() -> Files.newInputStream(path), () -> Files.newOutputStream(path), codec);
    }

    /**
     * Utility method to read a {@link ConfigElement} from an {@link InputStream}, using the given {@link ConfigCodec}
     * for decoding. This method uses {@link ConfigBridges#fromStreams(Callable, Callable, ConfigCodec)} to produce a
     * ConfigBridge implementation that is immediately read from.
     * @param inputStream the InputStream to read from
     * @param codec the ConfigCodec which will be used to decode the input data
     * @return a {@link ConfigNode} object representing the decoded configuration data
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     * @throws NullPointerException if any arguments are null
     */
    public static @NotNull ConfigElement read(@NotNull InputStream inputStream,
                                              @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);

        return readInternal(inputStream, codec);
    }

    /**
     * Writes a {@link ConfigElement} to an {@link OutputStream}, using the given {@link ConfigCodec}.
     * @param outputStream the OutputStream to write to
     * @param codec the codec to use to encode the data
     * @param element the element that will be written
     * @throws IOException if an IO error occurs when writing to the stream
     * @throws NullPointerException if any of the arguments are null
     */
    public static void write(@NotNull OutputStream outputStream,
                             @NotNull ConfigCodec codec,
                             @NotNull ConfigElement element)
            throws IOException {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(element);

        writeInternal(outputStream, element, codec);
    }

    /**
     * Same as {@link ConfigBridges#read(InputStream, ConfigCodec)}, but uses a {@link ByteArrayInputStream}
     * constructed from the provided string, assuming a UTF-8 encoding.
     * @param input the string to read from
     * @param codec the {@link ConfigCodec} which will be used to decode the input string
     * @return a {@link ConfigNode} object representing the decoded configuration data
     * @throws IOException if the string does not contain valid data for the codec
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigElement read(@NotNull String input, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);

        return readInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec);
    }

    /**
     * Writes the given {@link ConfigNode} to a string, using the provided {@link ConfigCodec}. Note that this should
     * never throw an exception under normal circumstances, as the {@link OutputStream} being written to is a simple
     * in-memory {@link ByteArrayOutputStream}. However, an {@link IOException} <i>can still be thrown</i> if there is
     * a problem with the provided codec or node (perhaps the codec is incapable of parsing one or more of the objects
     * present in the node?)
     * @param node the ConfigNode to write
     * @param codec the ConfigCodec used to decode node
     * @return a string containing the encoded data present in node
     * @throws IOException if an IO error occurs
     */
    public static @NotNull String write(@NotNull ConfigNode node, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(node);
        Objects.requireNonNull(codec);

        OutputStream outputStream = new ByteArrayOutputStream();
        writeInternal(outputStream, node, codec);
        return outputStream.toString();
    }

    /**
     * Same as {@link ConfigBridges#read(InputStream, ConfigCodec)}, but uses a {@link InputStream} constructed from
     * the provided {@link Path}.
     * @param path the path pointing to the file to read from
     * @param codec the codec to use to decode the file
     * @return a {@link FileConfigNode} representing the file's configuration data
     * @throws IOException if the config data contained in the file is invalid or if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigElement read(@NotNull Path path, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return readInternal(Files.newInputStream(path), codec);
    }

    /**
     * Writes a {@link FileConfigNode} to the file. The node's codec will be used to encode the data. The node must not
     * represent a directory.
     * @param path a {@link Path} pointing to the file to write to
     * @param node the node to write
     * @throws IOException if an IO error occurred
     * @throws IllegalArgumentException if node represents a directory
     */
    public static void write(@NotNull Path path, @NotNull FileConfigNode node) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(node);

        writeInternal(Files.newOutputStream(path), node, node.getCodec());
    }

    /**
     * Writes a {@link FileConfigNode} to the filesystem. The provided codec will be used to encode the node's data.
     * @param path a path pointing to the file to write to
     * @param node the node to write
     * @param codec the codec to use
     * @throws IOException if an IO error occurred
     */
    public static void write(@NotNull Path path, @NotNull ConfigNode node, @NotNull ConfigCodec codec)
            throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(node);
        Objects.requireNonNull(codec);

        writeInternal(Files.newOutputStream(path), node, codec);
    }
}
