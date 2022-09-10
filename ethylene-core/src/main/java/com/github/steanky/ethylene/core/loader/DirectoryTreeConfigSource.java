package com.github.steanky.ethylene.core.loader;

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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class DirectoryTreeConfigSource implements ConfigSource {
    private static final char EXTENSION_SEPARATOR = '.';

    private final Path path;
    private final CodecResolver codecResolver;

    public DirectoryTreeConfigSource(@NotNull Path path, @NotNull CodecResolver codecResolver) {
        this.path = Objects.requireNonNull(path);
        this.codecResolver = Objects.requireNonNull(codecResolver);
    }

    @Override
    public @NotNull CompletableFuture<ConfigElement> read() {
        return CompletableFuture.completedFuture(GraphTransformer.process(path, directoryEntry -> {
            List<Path> pathList;
            try(Stream<Path> paths = Files.walk(directoryEntry, 0, FileVisitOption.FOLLOW_LINKS)) {
                pathList = paths.toList();
            } catch (IOException e) {
                pathList = Collections.emptyList();
            }

            ConfigNode node = new LinkedConfigNode(pathList.size());

            List<Path> finalPathList = pathList;
            return new GraphTransformer.Node<>(directoryEntry, new Iterator<>() {
                private final Iterator<Path> pathIterator = finalPathList.listIterator();

                @Override
                public boolean hasNext() {
                    return pathIterator.hasNext();
                }

                @Override
                public Entry<String, Path> next() {
                    Path path = pathIterator.next();
                    return Entry.of(getName(path), path);
                }
            }, new GraphTransformer.Output<>(node,
                    (GraphTransformer.Accumulator<String, ConfigElement>) (s, configElement, circular) -> node
                            .put(s, configElement)));
        }, Files::isDirectory, entry -> {
            String extension = getExtension(entry);

            if (codecResolver.hasCodec(extension)) {
                try {
                    return Configuration.read(entry, codecResolver.resolve(extension));
                } catch (IOException ignored) {}
            }

            return ConfigPrimitive.nil();
        }, DirectoryTreeConfigSource::getKey));
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        return null;
    }

    private static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int separatorIndex = name.lastIndexOf(EXTENSION_SEPARATOR);
        if (separatorIndex == -1) {
            return "";
        }

        return name.substring(separatorIndex + 1);
    }

    private static String getName(Path path) {
        String name = path.getFileName().toString();

        int separatorIndex = name.lastIndexOf(EXTENSION_SEPARATOR);
        if (separatorIndex == -1 || Files.isDirectory(path)) {
            return name;
        }

        return name.substring(0, separatorIndex);
    }

    private static Object getKey(Path path) {
        try {
            Object fileKey = Files.readAttributes(path, BasicFileAttributes.class).fileKey();
            if (fileKey != null) {
                return fileKey;
            }
        } catch (IOException ignored) {}

        //if we can't use the file key, fall back on the normalized path string
        //this is not ideal as it can lead to issue with symbolic links
        return path.normalize().toString();
    }
}
