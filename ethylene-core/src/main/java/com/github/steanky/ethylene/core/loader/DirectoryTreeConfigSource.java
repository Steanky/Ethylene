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
    private final ExtensionExtractor extensionExtractor;
    private final ConfigCodec defaultCodec;
    private final String preferredExtension;
    private final Executor executor;

    public DirectoryTreeConfigSource(@NotNull Path rootPath, @NotNull CodecResolver codecResolver,
            @NotNull ExtensionExtractor extensionExtractor, @NotNull ConfigCodec defaultCodec,
            @Nullable Executor executor) {
        this.rootPath = Objects.requireNonNull(rootPath);
        this.codecResolver = Objects.requireNonNull(codecResolver);
        this.extensionExtractor = Objects.requireNonNull(extensionExtractor);
        this.defaultCodec = Objects.requireNonNull(defaultCodec);
        this.preferredExtension = defaultCodec.getPreferredExtension().isEmpty() ? "" :
                "." + defaultCodec.getPreferredExtension();
        this.executor = Objects.requireNonNull(executor);
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
                                codecResolver.hasCodec(extensionExtractor.getExtension(path)))).toList();
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
                        return Entry.of(extensionExtractor.getName(path), path);
                    }
                }, new GraphTransformer.Output<>(node,
                        (GraphTransformer.Accumulator<String, ConfigElement>) (s, configElement, circular) -> node.put(s,
                                configElement)));
            }, Files::isDirectory, entry -> {
                String extension = extensionExtractor.getExtension(entry);
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
            ExceptionHolder<IOException> exceptionHolder = new ExceptionHolder<>();
            GraphTransformer.process(new OutputEntry(rootPath, element), containerEntry -> {
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
                        List<PathInfo> paths = new ArrayList<>(node.size());
                        for (ConfigEntry entry : node.entryCollection()) {
                            String name = Objects.requireNonNull(entry.getFirst());
                            ConfigElement configElement = entry.getSecond();

                            Path dirPath = normalizedPath.resolve(name);
                            if (!existingPaths.contains(dirPath)) {
                                paths.add(new PathInfo(normalizedPath.resolve(name + preferredExtension), configElement));
                                continue;
                            }

                            paths.add(new PathInfo(dirPath, configElement));
                        }

                        return new GraphTransformer.Node<>(new Iterator<>() {
                            private final Iterator<PathInfo> pathInfoIterator = paths.iterator();

                            @Override
                            public boolean hasNext() {
                                return pathInfoIterator.hasNext();
                            }

                            @Override
                            public Entry<Object, OutputEntry> next() {
                                PathInfo pathInfo = pathInfoIterator.next();
                                return Entry.of(null, new OutputEntry(pathInfo.path, pathInfo.element));
                            }
                        });
                    }, potentialContainer -> Files.isDirectory(potentialContainer.path) && potentialContainer.element
                            .isNode(),
                    scalar -> {
                        exceptionHolder.call(() -> {
                            ConfigCodec codec;
                            if (Files.exists(scalar.path) && !Files.isDirectory(scalar.path)) {
                                String extension = extensionExtractor.getExtension(scalar.path);
                                if (codecResolver.hasCodec(extension)) {
                                    codec = codecResolver.resolve(extension);
                                } else {
                                    codec = defaultCodec;
                                }
                            } else {
                                codec = defaultCodec;
                            }


                            Configuration.write(scalar.path, scalar.element, codec);
                        });

                        return null;
                    }, entry -> getKey(entry.path), new HashMap<>(), new ArrayDeque<>());

            exceptionHolder.throwIfPresent();
            return null;
        }, executor);
    }

    private record OutputEntry(Path path, ConfigElement element) {}

    private record PathInfo(Path path, ConfigElement element) {
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }

            if (obj instanceof PathInfo pathInfo) {
                return path.equals(pathInfo.path);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }
}
