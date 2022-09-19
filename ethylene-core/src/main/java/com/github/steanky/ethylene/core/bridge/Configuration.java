package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Contains many static utility methods to simplify the process of reading and writing configuration data from a variety
 * of sources, either synchronously or asynchronously. Asynchronous methods generally support user-supplied
 * {@link Executor}s, but in the event one is not given, an overload exists which uses {@link ForkJoinPool#commonPool()}
 * as a default.<p>
 * <p>
 * All methods, unless otherwise noted, will throw a {@link NullPointerException} if any of their arguments are null.
 */
public final class Configuration {
    private Configuration() {
        throw new AssertionError("Why would you try to do this?");
    }

    /**
     * <p>Produces a ConfigSource implementation which will use input/output streams generated from the provided
     * {@link Callable}s, using the provided {@link ConfigCodec} to read/write to these streams. Each Callable will be
     * invoked once per read/write attempt, and the returned stream will be closed after the operation completes.</p>
     *
     * <p>If either Callable throws an {@link IOException} when it is called (by an invocation to
     * {@link ConfigSource#read()} or {@link ConfigSource#write(ConfigElement)} on the returned ConfigSource), it will
     * be rethrown when {@link Future#get()} is called. If the exception thrown is any other type, a new IOException
     * instance will be created with the actual exception set as the cause, then thrown.</p>
     *
     * <p>The produced ConfigSource instance is synchronous.</p>
     *
     * @param inputCallable  the callable which produces {@link InputStream} instances for reading
     * @param outputCallable the callable which produces {@link OutputStream} instances for writing
     * @param codec          the codec used to encode/decode from the streams
     * @return a ConfigSource implementation which reads/writes from the given input/output streams
     */
    public static @NotNull ConfigSource sourceFromStreams(@NotNull Callable<? extends InputStream> inputCallable,
        @NotNull Callable<? extends OutputStream> outputCallable, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(inputCallable);
        Objects.requireNonNull(outputCallable);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(inputCallable, outputCallable, codec, null);
    }

    //runs synchronously if executor is null, uses the given executor to run asynchronously if non-null
    private static ConfigSource fromStreamsInternal(Callable<? extends InputStream> inputCallable,
        Callable<? extends OutputStream> outputCallable, ConfigCodec codec, Executor executor) {
        return new ConfigSource() {
            @Override
            public @NotNull CompletableFuture<ConfigElement> read() {
                return FutureUtils.completeCallable(() -> codec.decode(inputCallable.call()), executor);
            }

            @Override
            public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
                return FutureUtils.completeCallable(() -> {
                    codec.encode(element, outputCallable.call());
                    return null;
                }, executor);
            }
        };
    }

    public static @NotNull ConfigSource asyncSourceFromStreams(@NotNull Callable<? extends InputStream> inputCallable,
        @NotNull Callable<? extends OutputStream> outputCallable, @NotNull ConfigCodec codec,
        @NotNull Executor executor) {
        Objects.requireNonNull(inputCallable);
        Objects.requireNonNull(outputCallable);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return fromStreamsInternal(inputCallable, outputCallable, codec, executor);
    }

    public static @NotNull ConfigSource asyncSourceFromStreams(@NotNull Callable<? extends InputStream> inputCallable,
        @NotNull Callable<? extends OutputStream> outputCallable, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(inputCallable);
        Objects.requireNonNull(outputCallable);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(inputCallable, outputCallable, codec, ForkJoinPool.commonPool());
    }

    /**
     * Produces a ConfigSource implementation capable of reading and writing to the given file path.
     *
     * @param path  a path pointing to the file read from and written to
     * @param codec the codec used to read/write from this file
     * @return a ConfigSource implementation which can read/write {@link ConfigElement} objects from and to the given
     * file
     */
    public static @NotNull ConfigSource sourceFromPath(@NotNull Path path, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(() -> Files.newInputStream(path), () -> Files.newOutputStream(path), codec, null);
    }

    public static @NotNull ConfigSource asyncSourceFromPath(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull Executor executor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return fromStreamsInternal(() -> Files.newInputStream(path), () -> Files.newOutputStream(path), codec,
            executor);
    }

    public static @NotNull ConfigSource asyncSourceFromPath(@NotNull Path path, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return fromStreamsInternal(() -> Files.newInputStream(path), () -> Files.newOutputStream(path), codec,
            ForkJoinPool.commonPool());
    }

    /**
     * Reads a {@link ConfigElement} from an {@link InputStream}, using the given {@link ConfigCodec} for decoding.
     *
     * @param inputStream the InputStream to read from
     * @param codec       the ConfigCodec which will be used to decode the input data
     * @return a {@link ConfigNode} object representing the decoded configuration data
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     */
    public static @NotNull ConfigElement read(@NotNull InputStream inputStream, @NotNull ConfigCodec codec)
        throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);

        return readInternal(inputStream, codec);
    }

    private static ConfigElement readInternal(InputStream inputStream, ConfigCodec codec) throws IOException {
        return codec.decode(inputStream);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull InputStream inputStream,
        @NotNull ConfigCodec codec, @NotNull Executor executor) {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return readAsyncInternal(inputStream, codec, executor);
    }

    private static CompletableFuture<ConfigElement> readAsyncInternal(InputStream inputStream, ConfigCodec codec,
        Executor executor) {
        return FutureUtils.completeCallableAsync(() -> codec.decode(inputStream), executor);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull InputStream inputStream,
        @NotNull ConfigCodec codec) {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);

        return readAsyncInternal(inputStream, codec, ForkJoinPool.commonPool());
    }

    /**
     * Works similarly to {@link Configuration#read(InputStream, ConfigCodec)}, but uses the given
     * {@link ConfigProcessor} to process the resulting {@link ConfigElement}.
     *
     * @param inputStream the InputStream to read from
     * @param codec       the ConfigCodec which will be used to decode the input data
     * @param processor   the processor used to convert a ConfigElement into arbitrary data
     * @param <TData>     the type of data to read
     * @return an object representing the data from the input stream
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     */
    public static <TData> TData read(@NotNull InputStream inputStream, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return processor.dataFromElement(readInternal(inputStream, codec));
    }

    public static <TData> @NotNull CompletableFuture<TData> readAsync(@NotNull InputStream inputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigProcessor<? extends TData> processor, @NotNull Executor executor) {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        return readAsyncInternal(inputStream, codec, processor, executor);
    }

    private static <TData> CompletableFuture<TData> readAsyncInternal(InputStream inputStream, ConfigCodec codec,
        ConfigProcessor<? extends TData> processor, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> processor.dataFromElement(codec.decode(inputStream)), executor);
    }

    public static <TData> @NotNull CompletableFuture<TData> readAsync(@NotNull InputStream inputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigProcessor<? extends TData> processor) {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return readAsyncInternal(inputStream, codec, processor, ForkJoinPool.commonPool());
    }

    /**
     * Writes a {@link ConfigElement} to an {@link OutputStream}, using the given {@link ConfigCodec}.
     *
     * @param outputStream the OutputStream to write to
     * @param codec        the codec to use to encode the data
     * @param element      the element that will be written
     * @throws IOException          if an IO error occurs when writing to the stream
     * @throws NullPointerException if any of the arguments are null
     */
    public static void write(@NotNull OutputStream outputStream, @NotNull ConfigCodec codec,
        @NotNull ConfigElement element) throws IOException {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(element);

        writeInternal(outputStream, element, codec);
    }

    private static void writeInternal(OutputStream outputStream, ConfigElement element, ConfigCodec codec)
        throws IOException {
        codec.encode(element, outputStream);
    }

    public static @NotNull CompletableFuture<Void> writeAsync(@NotNull OutputStream outputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigElement element, @NotNull Executor executor) {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(element);
        Objects.requireNonNull(executor);

        return writeAsyncInternal(outputStream, element, codec, executor);
    }

    private static CompletableFuture<Void> writeAsyncInternal(OutputStream outputStream, ConfigElement element,
        ConfigCodec codec, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> {
            codec.encode(element, outputStream);
            return null;
        }, executor);
    }

    public static @NotNull CompletableFuture<Void> writeAsync(@NotNull OutputStream outputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigElement element) {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(element);

        return writeAsyncInternal(outputStream, element, codec, ForkJoinPool.commonPool());
    }

    /**
     * Same as {@link Configuration#write(OutputStream, ConfigCodec, ConfigElement)}, but uses the given
     * {@link ConfigProcessor} to process the given {@link ConfigElement}.
     *
     * @param outputStream the OutputStream to write to
     * @param codec        the codec to use to encode the data
     * @param processor    the processor used to convert a ConfigElement into arbitrary data
     * @param data         the data object to write
     * @param <TData>      the type of data to write
     * @throws IOException if an IO error occurs when writing to the stream
     */
    public static <TData> void write(@NotNull OutputStream outputStream, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data) throws IOException {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);

        writeInternal(outputStream, processor.elementFromData(data), codec);
    }

    public static <TData> @NotNull CompletableFuture<Void> writeAsync(@NotNull OutputStream outputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigProcessor<? super TData> processor, TData data,
        @NotNull Executor executor) {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        return writeAsyncInternal(outputStream, data, codec, processor, executor);
    }

    private static <TData> CompletableFuture<Void> writeAsyncInternal(OutputStream outputStream, TData data,
        ConfigCodec codec, ConfigProcessor<? super TData> processor, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> {
            codec.encode(processor.elementFromData(data), outputStream);
            return null;
        }, executor);
    }

    public static <TData> @NotNull CompletableFuture<Void> writeAsync(@NotNull OutputStream outputStream,
        @NotNull ConfigCodec codec, @NotNull ConfigProcessor<? super TData> processor, TData data) {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return writeAsyncInternal(outputStream, data, codec, processor, ForkJoinPool.commonPool());
    }

    /**
     * Same as {@link Configuration#read(InputStream, ConfigCodec)}, but uses a {@link ByteArrayInputStream} constructed
     * from the provided string, assuming a UTF-8 encoding.
     *
     * @param input the string to read from
     * @param codec the {@link ConfigCodec} which will be used to decode the input string
     * @return a {@link ConfigElement} object representing the decoded configuration data
     * @throws IOException          if the string does not contain valid data for the codec
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigElement read(@NotNull String input, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);

        return readInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull String input, @NotNull ConfigCodec codec,
        @NotNull Executor executor) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return readAsyncInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec, executor);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull String input,
        @NotNull ConfigCodec codec) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);

        return readAsyncInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec,
            ForkJoinPool.commonPool());
    }

    /**
     * Works similarly to {@link Configuration#read(String, ConfigCodec)}, but uses the given {@link ConfigProcessor} to
     * process the resulting {@link  ConfigElement}.
     *
     * @param input     the string to read from
     * @param codec     the ConfigCodec which will be used to decode the input data
     * @param processor the processor used to convert a ConfigElement into arbitrary data
     * @param <TData>   the type of data to read
     * @return an object representing the data from the input stream
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     */
    public static <TData> TData read(@NotNull String input, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return processor.dataFromElement(
            readInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec));
    }

    public static <TData> CompletableFuture<TData> readAsync(@NotNull String input, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor, @NotNull Executor executor) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        return readAsyncInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec, processor,
            executor);
    }

    public static <TData> CompletableFuture<TData> readAsync(@NotNull String input, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return readAsyncInternal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), codec, processor,
            ForkJoinPool.commonPool());
    }

    /**
     * Writes the given {@link ConfigElement} to a string, using the provided {@link ConfigCodec}. Note that this should
     * never throw an exception under normal circumstances, as the {@link OutputStream} being written to is a simple
     * in-memory {@link ByteArrayOutputStream}. However, an {@link IOException} <i>can still be thrown</i> if there is a
     * problem with the provided codec or node (perhaps the codec is incapable of parsing one or more of the objects
     * present in the node?)
     *
     * @param element the ConfigElement to write
     * @param codec   the ConfigCodec used to decode node
     * @return a string containing the encoded data present in node
     * @throws IOException if an IO error occurs
     */
    public static @NotNull String write(@NotNull ConfigElement element, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);

        OutputStream outputStream = new ByteArrayOutputStream();
        writeInternal(outputStream, element, codec);
        return outputStream.toString();
    }

    public static @NotNull CompletableFuture<String> writeAsync(@NotNull ConfigElement element,
        @NotNull ConfigCodec codec, @NotNull Executor executor) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        OutputStream outputStream = new ByteArrayOutputStream();
        return writeAsyncInternal(outputStream, element, codec, executor).thenApply(ignored -> outputStream.toString());
    }

    public static @NotNull CompletableFuture<String> writeAsync(@NotNull ConfigElement element,
        @NotNull ConfigCodec codec) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);

        OutputStream outputStream = new ByteArrayOutputStream();
        return writeAsyncInternal(outputStream, element, codec, ForkJoinPool.commonPool()).thenApply(
            ignored -> outputStream.toString());
    }

    /**
     * Same as {@link Configuration#write(ConfigElement, ConfigCodec)}, but uses the given {@link ConfigProcessor} to
     * process the given {@link ConfigElement}.
     *
     * @param codec     the codec to use to encode the data
     * @param processor the processor used to convert a ConfigElement into arbitrary data
     * @param data      the data object to write
     * @param <TData>   the type of data to write
     * @return a string containing the encoded data present in node
     * @throws IOException if an IO error occurs when writing to the stream
     */
    public static <TData> @NotNull String write(@NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data) throws IOException {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        OutputStream outputStream = new ByteArrayOutputStream();
        writeInternal(outputStream, processor.elementFromData(data), codec);
        return outputStream.toString();
    }

    public static <TData> @NotNull CompletableFuture<String> writeAsync(@NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data, @NotNull Executor executor) {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        OutputStream outputStream = new ByteArrayOutputStream();
        return writeAsyncInternal(outputStream, data, codec, processor, executor).thenApply(
            ignored -> outputStream.toString());
    }

    public static <TData> @NotNull CompletableFuture<String> writeAsync(@NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data) {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        OutputStream outputStream = new ByteArrayOutputStream();
        return writeAsyncInternal(outputStream, data, codec, processor, ForkJoinPool.commonPool()).thenApply(
            ignored -> outputStream.toString());
    }

    /**
     * Same as {@link Configuration#read(InputStream, ConfigCodec)}, but uses a {@link InputStream} constructed from the
     * provided {@link Path}.
     *
     * @param path  the path pointing to the file to read from
     * @param codec the codec to use to decode the file
     * @return a {@link ConfigElement} representing the file's configuration data
     * @throws IOException          if the config data contained in the file is invalid or if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    public static @NotNull ConfigElement read(@NotNull Path path, @NotNull ConfigCodec codec) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return readInternal(Files.newInputStream(path), codec);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull Executor executor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return readAsyncInternal(() -> Files.newInputStream(path), codec, executor);
    }

    private static CompletableFuture<ConfigElement> readAsyncInternal(Callable<? extends InputStream> inputStream,
        ConfigCodec codec, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> codec.decode(inputStream.call()), executor);
    }

    public static @NotNull CompletableFuture<ConfigElement> readAsync(@NotNull Path path, @NotNull ConfigCodec codec) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);

        return readAsyncInternal(() -> Files.newInputStream(path), codec, ForkJoinPool.commonPool());
    }

    /**
     * Works similarly to {@link Configuration#read(Path, ConfigCodec)}, but uses the given {@link ConfigProcessor} to
     * process the resulting {@link ConfigElement}.
     *
     * @param path      the file path to read from
     * @param codec     the ConfigCodec which will be used to decode the input data
     * @param processor the processor used to convert a ConfigElement into arbitrary data
     * @param <TData>   the type of data to read
     * @return an object representing the data from the input stream
     * @throws IOException if an IO error occurs or the InputStream does not contain valid data for the codec
     */
    public static <TData> TData read(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return processor.dataFromElement(readInternal(Files.newInputStream(path), codec));
    }

    public static <TData> CompletableFuture<TData> readAsync(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor, @NotNull Executor executor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        return readAsyncInternal(() -> Files.newInputStream(path), codec, processor, executor);
    }

    private static <TData> CompletableFuture<TData> readAsyncInternal(Callable<? extends InputStream> inputStream,
        ConfigCodec codec, ConfigProcessor<? extends TData> processor, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> processor.dataFromElement(codec.decode(inputStream.call())),
            executor);
    }

    public static <TData> CompletableFuture<TData> readAsync(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? extends TData> processor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return readAsyncInternal(() -> Files.newInputStream(path), codec, processor, ForkJoinPool.commonPool());
    }

    /**
     * Writes a {@link ConfigNode} to the filesystem. The provided codec will be used to encode the node's data.
     *
     * @param path    a path pointing to the file to write to
     * @param element the element to write
     * @param codec   the codec to use
     * @throws IOException if an IO error occurred
     */
    public static void write(@NotNull Path path, @NotNull ConfigElement element, @NotNull ConfigCodec codec)
        throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);

        writeInternal(Files.newOutputStream(path), element, codec);
    }

    public static @NotNull CompletableFuture<Void> writeAsync(@NotNull Path path, @NotNull ConfigElement element,
        @NotNull ConfigCodec codec, @NotNull Executor executor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(executor);

        return writeAsyncInternal(() -> Files.newOutputStream(path), element, codec, executor);
    }

    private static CompletableFuture<Void> writeAsyncInternal(Callable<? extends OutputStream> outputStream,
        ConfigElement element, ConfigCodec codec, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> {
            codec.encode(element, outputStream.call());
            return null;
        }, executor);
    }

    public static @NotNull CompletableFuture<Void> writeAsync(@NotNull Path path, @NotNull ConfigElement element,
        @NotNull ConfigCodec codec) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(element);
        Objects.requireNonNull(codec);

        return writeAsyncInternal(() -> Files.newOutputStream(path), element, codec, ForkJoinPool.commonPool());
    }

    /**
     * Same as {@link Configuration#write(Path, ConfigElement, ConfigCodec)}, but uses the given {@link ConfigProcessor}
     * to process the given {@link ConfigElement}.
     *
     * @param path      the path to write to
     * @param codec     the codec to use to encode the data
     * @param processor the processor used to convert a ConfigElement into arbitrary data
     * @param data      the data object to write
     * @param <TData>   the type of data to write
     * @throws IOException if an IO error occurs when writing to the stream
     */
    public static <TData> void write(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        writeInternal(Files.newOutputStream(path), processor.elementFromData(data), codec);
    }

    public static <TData> @NotNull CompletableFuture<Void> writeAsync(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data, @NotNull Executor executor) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);
        Objects.requireNonNull(executor);

        return writeAsyncInternal(() -> Files.newOutputStream(path), data, codec, processor, executor);
    }

    private static <TData> CompletableFuture<Void> writeAsyncInternal(Callable<? extends OutputStream> outputStream,
        TData data, ConfigCodec codec, ConfigProcessor<? super TData> processor, Executor executor) {
        return FutureUtils.completeCallableAsync(() -> {
            codec.encode(processor.elementFromData(data), outputStream.call());
            return null;
        }, executor);
    }

    public static <TData> @NotNull CompletableFuture<Void> writeAsync(@NotNull Path path, @NotNull ConfigCodec codec,
        @NotNull ConfigProcessor<? super TData> processor, TData data) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(processor);

        return writeAsyncInternal(() -> Files.newOutputStream(path), data, codec, processor, ForkJoinPool.commonPool());
    }
}
