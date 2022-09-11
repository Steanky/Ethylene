package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.GraphTransformer;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.collection.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class DirectoryTreeConfigSource implements ConfigSource {
    private final Path path;
    private final CodecResolver codecResolver;
    private final ExtensionExtractor extensionExtractor;

    private record OutputEntry(Path path, ConfigElement element) {}

    public DirectoryTreeConfigSource(@NotNull Path path, @NotNull CodecResolver codecResolver,
            @NotNull ExtensionExtractor extensionExtractor) {
        this.path = Objects.requireNonNull(path);
        this.codecResolver = Objects.requireNonNull(codecResolver);
        this.extensionExtractor = Objects.requireNonNull(extensionExtractor);
    }

    @Override
    public @NotNull CompletableFuture<ConfigElement> read() {
        return CompletableFuture.completedFuture(GraphTransformer.process(path, directoryEntry -> {
            List<Path> pathList;
            try(Stream<Path> paths = Files.walk(directoryEntry, 0, FileVisitOption.FOLLOW_LINKS)) {
                pathList = paths.filter(path -> Files.isDirectory(path) || (codecResolver.hasCodec(extensionExtractor
                        .getExtension(path)))).toList();
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
                    (GraphTransformer.Accumulator<String, ConfigElement>) (s, configElement, circular) -> node
                            .put(s, configElement)));
        }, Files::isDirectory, entry -> {
            String extension = extensionExtractor.getExtension(entry);
            if (codecResolver.hasCodec(extension)) {
                try {
                    return Configuration.read(entry, codecResolver.resolve(extension));
                } catch (IOException ignored) {}
            }

            return ConfigPrimitive.NULL;
        }, DirectoryTreeConfigSource::getKey));
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        return CompletableFuture.completedFuture(null);
    }

    private static Object getKey(Path path) {
        try {
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
}
