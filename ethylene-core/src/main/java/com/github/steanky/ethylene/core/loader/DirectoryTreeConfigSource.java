package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.GraphTransformer;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.util.ExceptionHolder;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class DirectoryTreeConfigSource implements ConfigSource {
    private final Path rootPath;
    private final CodecResolver codecResolver;
    private final PathNameInspector pathNameInspector;
    private final ConfigCodec defaultCodec;
    private final String preferredExtension;
    private final Executor executor;

    public DirectoryTreeConfigSource(@NotNull Path rootPath, @NotNull CodecResolver codecResolver,
            @NotNull PathNameInspector pathNameInspector, @NotNull ConfigCodec defaultCodec,
            @Nullable Executor executor) {
        this.rootPath = Objects.requireNonNull(rootPath);
        this.codecResolver = Objects.requireNonNull(codecResolver);
        this.pathNameInspector = Objects.requireNonNull(pathNameInspector);
        this.defaultCodec = Objects.requireNonNull(defaultCodec);
        this.preferredExtension = defaultCodec.getPreferredExtension().isEmpty() ? "" :
                "." + defaultCodec.getPreferredExtension();
        this.executor = executor;
    }

    public DirectoryTreeConfigSource(@NotNull Path rootPath, @NotNull CodecResolver codecResolver,
            @NotNull PathNameInspector pathNameInspector, @NotNull ConfigCodec defaultCodec) {
        this(rootPath, codecResolver, pathNameInspector, defaultCodec, null);
    }

    private static Object getKey(Path path) {
        try {
            //try to use file keys if possible (supported by the system & accessible by us)
            Object fileKey = Files.readAttributes(path, BasicFileAttributes.class).fileKey();
            if (fileKey != null) {
                return fileKey;
            }
        } catch (IOException ignored) {
            //if we got an exception, we probably can't read symlinks either, so just return
            return path.normalize().toString();
        }

        //if we can't use the file key because system support is lacking, and no error occurred, make a best-effort
        //attempt to handle them ourselves based on normalized path name
        Path root = path;
        if (Files.isSymbolicLink(root)) {
            //for keeping track of the paths we visit
            Set<String> visited = Collections.newSetFromMap(new HashMap<>(2));
            visited.add(root.normalize().toString());

            do {
                try {
                    Path old = root;
                    root = Files.readSymbolicLink(root);
                    String targetIdentifier = root.normalize().toString();
                    if (visited.contains(targetIdentifier)) {
                        //cycle detected
                        return old.normalize().toString();
                    }

                    visited.add(targetIdentifier);
                } catch (IOException e) {
                    return root.normalize().toString();
                }
            } while (Files.isSymbolicLink(root));
        }

        return root.normalize().toString();
    }

    @Override
    public @NotNull CompletableFuture<ConfigElement> read() {
        return FutureUtils.completeCallable(() -> {
            ExceptionHolder<IOException> exceptionHolder = new ExceptionHolder<>();

            ConfigElement element = GraphTransformer.process(rootPath, directoryEntry -> {
                List<Path> pathList = exceptionHolder.supply(() -> {
                    try (Stream<Path> paths = Files.walk(directoryEntry, 0, FileVisitOption.FOLLOW_LINKS)) {
                        return paths.filter(path -> !path.equals(directoryEntry) && (Files.isDirectory(path) ||
                                codecResolver.hasCodec(pathNameInspector.getExtension(path)))).toList();
                    }
                }, List::of);

                ConfigNode node = new LinkedConfigNode(pathList.size());
                return new GraphTransformer.Node<>(new Iterator<>() {
                    private final Iterator<Path> pathIterator = pathList.listIterator();

                    @Override
                    public boolean hasNext() {
                        return pathIterator.hasNext();
                    }

                    @Override
                    public Entry<String, Path> next() {
                        Path path = pathIterator.next();
                        return Entry.of(pathNameInspector.getName(path), path);
                    }
                }, new GraphTransformer.Output<>(node,
                        (GraphTransformer.Accumulator<String, ConfigElement>) (s, configElement, circular) -> node.put(s,
                                configElement)));
            }, Files::isDirectory, entry -> {
                String extension = pathNameInspector.getExtension(entry);
                if (codecResolver.hasCodec(extension)) {
                    return exceptionHolder.supply(() -> Configuration.read(entry, codecResolver.resolve(extension)),
                            () -> ConfigPrimitive.NULL);
                }

                return ConfigPrimitive.NULL;
            }, DirectoryTreeConfigSource::getKey, new HashMap<>(), new ArrayDeque<>());

            exceptionHolder.throwIfPresent();
            return element;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        return FutureUtils.completeCallable(() -> {
            if (!element.isNode()) {
                writeElementWithPreferredExtension(element, rootPath);
                return null;
            }

            ExceptionHolder<IOException> exceptionHolder = new ExceptionHolder<>();
            GraphTransformer.process(new OutputInfo(rootPath, element), containerEntry -> {
                        exceptionHolder.call(() -> Files.createDirectories(containerEntry.path));
                        Path normalizedPath = containerEntry.path.normalize();

                        Set<Path> existingPaths = exceptionHolder.supply(() -> {
                            Set<Path> newPaths;
                            try (Stream<Path> paths = Files.walk(normalizedPath, 0, FileVisitOption.FOLLOW_LINKS)) {
                                newPaths = new HashSet<>();
                                for (Path path : (Iterable<? extends Path>) paths::iterator) {
                                    if (path.equals(normalizedPath)) {
                                        //Files.walk includes the root entry
                                        continue;
                                    }

                                    newPaths.add(path);
                                }
                            }

                            return newPaths;
                        }, Set::of);

                        ConfigNode node = containerEntry.element.asNode();
                        List<Entry<Object, OutputInfo>> paths = new ArrayList<>(node.size());

                        for (ConfigEntry entry : node.entryCollection()) {
                            ConfigElement configElement = entry.getSecond();
                            String elementName = Objects.requireNonNull(entry.getFirst());
                            Path expectedPath = normalizedPath.resolve(elementName);
                            paths.add(Entry.of(null, new OutputInfo(existingPaths.contains(expectedPath) ? expectedPath
                                    : normalizedPath.resolve(elementName + preferredExtension), configElement)));
                        }

                        //this node has no output (not necessary as we're creating a tree in the filesystem instead of
                        //doing it in-memory
                        return new GraphTransformer.Node<>(paths.iterator(), new GraphTransformer.Output<>(containerEntry,
                                (o, info, circular) -> {
                                    exceptionHolder.call(() -> writeElement(info.element, info.path));
                                }));
                    }, potentialContainer -> {
                        if (!potentialContainer.element.isNode()) {
                            return false;
                        }

                        ConfigNode node = potentialContainer.element.asNode();
                        for (ConfigEntry entry : node.entryCollection()) {
                            if (!entry.getValue().isNode()) {
                                return false;
                            }
                        }

                        return true;
                    },
                    scalar -> {
                        //actually write the data to a specific file
                        //the path is assumed to already have an extension
                        //scalar.element will always be a ConfigNode, this is checked before processing and in the node
                        //function
                        //exceptionHolder.call(() -> writeElement(scalar.element, scalar.path));
                        return scalar;
                    }, entry -> getKey(entry.path), new HashMap<>(), new ArrayDeque<>());

            exceptionHolder.throwIfPresent();
            return null;
        }, executor);
    }

    private void writeElementWithPreferredExtension(ConfigElement element, Path parent) throws IOException {
        String filename = parent.getFileName().toString();
        Path filePath = parent.resolve(filename + preferredExtension);
        writeElement(element, filePath);
    }

    private void writeElement(ConfigElement element, Path path) throws IOException {
        String extension = pathNameInspector.getExtension(path);

        ConfigCodec codec;
        if (codecResolver.hasCodec(extension)) {
            codec = codecResolver.resolve(extension);
        } else {
            codec = defaultCodec;
        }

        Files.createDirectories(path.getParent());
        Configuration.write(path, element, codec);
    }

    private record OutputInfo(Path path, ConfigElement element) {}
}
