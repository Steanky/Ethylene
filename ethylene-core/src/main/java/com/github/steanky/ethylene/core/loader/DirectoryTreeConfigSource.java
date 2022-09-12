package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.GraphTransformer;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class DirectoryTreeConfigSource implements ConfigSource {
    private final Path rootPath;
    private final CodecResolver codecResolver;
    private final ExtensionExtractor extensionExtractor;
    private final ConfigCodec defaultCodec;

    public DirectoryTreeConfigSource(@NotNull Path rootPath, @NotNull CodecResolver codecResolver,
            @NotNull ExtensionExtractor extensionExtractor, @NotNull ConfigCodec defaultCodec) {
        this.rootPath = Objects.requireNonNull(rootPath);
        this.codecResolver = Objects.requireNonNull(codecResolver);
        this.extensionExtractor = Objects.requireNonNull(extensionExtractor);
        this.defaultCodec = Objects.requireNonNull(defaultCodec);
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
        return CompletableFuture.completedFuture(GraphTransformer.process(rootPath, directoryEntry -> {
            List<Path> pathList;
            try (Stream<Path> paths = Files.walk(directoryEntry, 0, FileVisitOption.FOLLOW_LINKS)) {
                pathList = paths.filter(path -> Files.isDirectory(path) ||
                        (codecResolver.hasCodec(extensionExtractor.getExtension(path)))).toList();
            } catch (IOException e) {
                pathList = List.of();
            }

            ConfigNode node = new LinkedConfigNode(pathList.size());
            List<Path> finalPathList = pathList;
            return new GraphTransformer.Node<>(new Iterator<>() {
                private final Iterator<Path> pathIterator = finalPathList.listIterator();

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
                try {
                    return Configuration.read(entry, codecResolver.resolve(extension));
                } catch (IOException ignored) {
                }
            }

            return ConfigPrimitive.NULL;
        }, DirectoryTreeConfigSource::getKey, new HashMap<>(), new ArrayDeque<>()));
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        GraphTransformer.process(new OutputEntry(rootPath, element), containerEntry -> {
                    try {
                        Files.createDirectories(containerEntry.path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    ConfigNode node = containerEntry.element.asNode();
                    List<PathInfo> matchingPaths;
                    try (Stream<Path> paths = Files.walk(containerEntry.path, 0, FileVisitOption.FOLLOW_LINKS)) {
                        matchingPaths = new ArrayList<>();
                        for (Path path : (Iterable<? extends Path>) paths::iterator) {
                            if (!Files.isDirectory(path)) {
                                matchingPaths.add(new PathInfo(path, false));
                            } else if (node.containsKey(path.getFileName().toString())) {
                                matchingPaths.add(new PathInfo(path, false));
                            }
                        }
                    } catch (IOException e) {
                        matchingPaths = List.of();
                    }

                    List<PathInfo> finalMatchingPaths = matchingPaths;
                    return new GraphTransformer.Node<>(new Iterator<>() {
                        private final Iterator<PathInfo> pathInfoIterator = finalMatchingPaths.listIterator();

                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public Entry<Object, OutputEntry> next() {
                            return null;
                        }
                    }, new GraphTransformer.Output<>(containerEntry, GraphTransformer.emptyAccumulator()));
                }, potentialContainer -> Files.isDirectory(potentialContainer.path) && potentialContainer.element.isNode(),
                scalar -> {
                    try {
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
                    } catch (IOException ignored) {
                    }

                    return scalar;
                }, entry -> getKey(entry.path), new HashMap<>(), new ArrayDeque<>());

        return CompletableFuture.completedFuture(null);
    }

    private record OutputEntry(Path path, ConfigElement element) {}

    private record PathInfo(Path path, boolean isDirectory) {}
}
