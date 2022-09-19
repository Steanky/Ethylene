package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.util.ExceptionHandler;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryTreeConfigSource implements ConfigSource {
    private static final LinkOption[] EMPTY_LINK_OPTION_ARRAY = new LinkOption[0];
    private static final FileVisitOption[] EMPTY_FILE_VISIT_OPTION_ARRAY = new FileVisitOption[0];

    private final Path rootPath;
    private final CodecResolver codecResolver;
    private final PathNameInspector pathNameInspector;
    private final ConfigCodec preferredCodec;
    private final String preferredExtensionName;
    private final String preferredExtension;
    private final Executor executor;
    private final boolean supportSymlinks;
    private final FileVisitOption[] fileVisitOptions;

    public DirectoryTreeConfigSource(@NotNull Path rootPath, @NotNull CodecResolver codecResolver,
        @NotNull PathNameInspector pathNameInspector, @NotNull ConfigCodec preferredCodec, @Nullable Executor executor,
        boolean supportSymlinks) {
        this.rootPath = Objects.requireNonNull(rootPath);
        this.codecResolver = Objects.requireNonNull(codecResolver);
        this.pathNameInspector = Objects.requireNonNull(pathNameInspector);
        this.preferredCodec = Objects.requireNonNull(preferredCodec);

        this.preferredExtensionName = preferredCodec.getPreferredExtension();
        this.preferredExtension =
            preferredExtensionName.isEmpty() ? preferredExtensionName : "." + preferredExtensionName;
        this.executor = executor;
        this.supportSymlinks = supportSymlinks;
        this.fileVisitOptions =
            supportSymlinks ? new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS} : EMPTY_FILE_VISIT_OPTION_ARRAY;
    }

    private static Object getKey(Path path, boolean followSymlinks) {
        try {
            //try to use file keys if possible (supported by the system & accessible by us)
            //readAttributes follows symlinks (so a symlink to a file and the file itself will have the same key)
            LinkOption[] options =
                followSymlinks ? EMPTY_LINK_OPTION_ARRAY : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
            Object fileKey = Files.readAttributes(path, BasicFileAttributes.class, options).fileKey();
            if (fileKey != null) {
                return fileKey;
            }
        } catch (IOException ignored) {
            //if we can't read the attributes, we probably can't follow it if it's a symlink, so just return
            return path.toAbsolutePath().normalize().toString();
        }

        if (!followSymlinks) {
            //symlinks disabled
            return path.toAbsolutePath().normalize().toString();
        }

        //if we can't use the file key because system support is lacking, make a best-effort attempt to create a key
        //from the path string, following symbolic links as necessary, with cycle detection, taking care to normalize
        //the path for consistent behavior
        path = path.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(path)) {
            //low initial capacity, we shouldn't expect too many nested symlinks
            Set<Path> visited = new HashSet<>(2);
            visited.add(path);

            do {
                try {
                    path = Files.readSymbolicLink(path).toAbsolutePath().normalize();
                } catch (IOException e) {
                    return path.toString();
                }

                if (!visited.add(path)) {
                    //cycle detected
                    return path.toString();
                }
            } while (Files.isSymbolicLink(path));
        }

        return path.toString();
    }

    @Override
    public @NotNull CompletableFuture<ConfigElement> read() {
        return FutureUtils.completeCallable(() -> {
            if (!Files.exists(rootPath)) {
                return ConfigPrimitive.NULL;
            }

            ExceptionHandler<IOException> exceptionHandler = new ExceptionHandler<>(IOException.class);
            ConfigElement element = Graph.process(rootPath, directoryEntry -> {
                    //gets all files that are directories, not the current path, or a file with an extension we can
                    //understand
                    List<Path> pathList = exceptionHandler.get(() -> {
                        try (Stream<Path> paths = Files.walk(directoryEntry, 1, fileVisitOptions)) {
                            return paths.filter(path -> filterPath(directoryEntry, path)).toList();
                        }
                    }, List::of);

                    ConfigNode node = new LinkedConfigNode(pathList.size());
                    return Graph.node(new Iterator<>() {
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
                    }, Graph.output(node,
                        (Graph.Accumulator<String, ConfigElement>) (s, configElement, circular) -> node.put(s,
                            configElement)));
                }, Files::isDirectory, entry -> {
                    String extension = pathNameInspector.getExtension(entry);
                    if (codecResolver.hasCodec(extension)) {
                        return exceptionHandler.get(() -> Configuration.read(entry, codecResolver.resolve(extension)),
                            () -> ConfigPrimitive.NULL);
                    }

                    return ConfigPrimitive.NULL;
                }, entry -> getKey(entry, supportSymlinks), HashMap::new, ArrayDeque::new,
                Graph.Options.TRACK_ALL_REFERENCES);

            exceptionHandler.throwIfPresent();
            return element;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        return FutureUtils.completeCallable(() -> {
            if (!Files.exists(rootPath)) {
                //create the root if it doesn't exist
                Files.createDirectories(rootPath);
            }

            if (!Files.isDirectory(rootPath) || !element.isNode()) {
                //if root is not a directory, write the entire element to it
                //if element is not a node, do the same
                String extension = pathNameInspector.getExtension(rootPath);
                Configuration.write(rootPath, element,
                    codecResolver.hasCodec(extension) ? codecResolver.resolve(extension) : preferredCodec);
                return null;
            }

            ExceptionHandler<IOException> exceptionHandler = new ExceptionHandler<>(IOException.class);
            Graph.process(new OutputInfo(rootPath, element, true), containerEntry -> {
                exceptionHandler.run(() -> Files.createDirectories(containerEntry.path));
                Path normalizedPath = containerEntry.path.normalize();

                //get paths currently in the folder
                //if there's an exception, existingPaths will be empty
                Set<Path> existingPaths = exceptionHandler.get(() -> {
                    try (Stream<Path> paths = Files.walk(normalizedPath, 1, fileVisitOptions)) {
                        //like when reading, include all directories, and ignore files whose extensions we don't
                        //recognize
                        return paths.filter(path -> filterPath(normalizedPath, path)).collect(Collectors.toSet());
                    }
                }, Set::of);

                ConfigNode node = containerEntry.element.asNode();

                //list of paths
                //key (first in the entry) is always null because it's unused
                List<Entry<Object, OutputInfo>> paths = new ArrayList<>(node.size());
                for (ConfigEntry entry : node.entryCollection()) {
                    String elementName = entry.getFirst();
                    ConfigElement entryElement = entry.getSecond();
                    Path targetPath = normalizedPath.resolve(elementName);

                    boolean exists = existingPaths.contains(targetPath);

                    if (exists) {
                        //path already exists, is either an extensionless file or directory
                        //if it's a directory, we'll iterate into it later
                        //if it's a file, we'll write to it as a scalar
                        paths.add(
                            Entry.of(null, new OutputInfo(targetPath, entryElement, Files.isDirectory(targetPath))));
                    } else {
                        //path doesn't exist, but that could be because of an extension
                        //filter the existing paths to those whose name matches
                        List<Path> targets =
                            existingPaths.stream().filter(path -> pathNameInspector.getName(path).equals(elementName))
                                .toList();

                        if (targets.isEmpty()) {
                            //no file found at all - we'll write a new one using our preferred extension
                            Path filePath = normalizedPath.resolve(elementName + preferredExtension);
                            paths.add(Entry.of(null, new OutputInfo(filePath, entryElement, false)));
                        } else {
                            //we have at least one matching (and existing) file
                            //if there are multiple files, they must differ only by extension
                            //if one of these extensions is the preferred extension, use that
                            Path preferred = null;
                            for (Path path : targets) {
                                String extension = pathNameInspector.getExtension(path);
                                if (preferredExtensionName.equals(extension)) {
                                    preferred = path;
                                    break;
                                }
                            }

                            if (preferred == null) {
                                //no preferred extension, just use the first file on the list
                                preferred = targets.get(0);
                            }

                            paths.add(Entry.of(null, new OutputInfo(preferred, entryElement, false)));
                        }
                    }
                }

                //use another iterator - this will differ from the results passed to the accumulator only when
                //there is a circular reference (in which case the OutputInfo passed to the accumulator comes
                //from some other node)
                Iterator<Entry<Object, OutputInfo>> actualIterator = paths.iterator();
                return Graph.node(paths.iterator(), Graph.output(containerEntry, (o, outputInfo, circular) -> {
                    //actually write the files
                    //need to do this here, instead of in, say, the scalar mapper, so we can create
                    //symlinks when indicated to do so by the circular flag
                    OutputInfo actualInfo = actualIterator.next().getSecond();

                    if (circular && supportSymlinks) {
                        //make a symlink to the target file/directory instead of making a copy
                        //strip the extension if the link target is a directory
                        Path link = outputInfo.isDirectory ?
                            actualInfo.path.getParent().resolve(pathNameInspector.getName(actualInfo.path)) :
                            actualInfo.path;

                        exceptionHandler.run(() -> Files.createSymbolicLink(link, outputInfo.path));
                    } else if (!outputInfo.isDirectory) {
                        //if outputInfo is a directory, no need to bother writing anything
                        //(it will be created when its appropriate node is initialized)
                        String extension = pathNameInspector.getExtension(actualInfo.path);

                        exceptionHandler.run(() -> Configuration.write(actualInfo.path, actualInfo.element,
                            codecResolver.hasCodec(extension) ? codecResolver.resolve(extension) : preferredCodec));
                    }
                }));
            }, potentialContainer -> {
                if (!potentialContainer.element.isNode()) {
                    return false;
                }

                return potentialContainer.isDirectory;
            }, Function.identity(), entry -> entry.element, Graph.Options.TRACK_ALL_REFERENCES);

            exceptionHandler.throwIfPresent();
            return null;
        }, executor);
    }

    private boolean filterPath(Path rootPath, Path childPath) {
        return !childPath.equals(rootPath) &&
            (Files.isDirectory(childPath) || codecResolver.hasCodec(pathNameInspector.getExtension(childPath)));
    }

    private record OutputInfo(Path path, ConfigElement element, boolean isDirectory) {
    }
}
