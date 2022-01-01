package com.steank.ethylene.bridge;

import com.steank.ethylene.ConfigElement;
import com.steank.ethylene.PathUtils;
import com.steank.ethylene.codec.CodecRegistry;
import com.steank.ethylene.codec.ConfigCodec;
import com.steank.ethylene.collection.ConfigNode;
import com.steank.ethylene.collection.FileConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * <p>A class containing commonly used functionality for <i>filesystem</i> bridges. Filesystem bridges, as the name
 * implies, are {@link ConfigBridge} implementations which read {@link ConfigNode} objects from the filesystem. In
 * general, they are capable of reading entire directory trees into a single {@link FileConfigNode} object which may
 * be interpreted accordingly. Conversely, they may also <i>write</i> to one or more files according to the structure
 * of a given FileConfigNode.</p>
 *
 * <p>The behavior of this class can be modified easily by overriding one or more of its non-abstract methods, which
 * might involve:</p>
 *
 * <ul>
 *     <li>determining what {@link ConfigCodec} to use for a particular file</li>
 *     <li>choosing which files are valid to read data from</li>
 *     <li>mapping {@link File} objects to their associated string keys</li>
 * </ul>
 *
 * <p>Implementations of this class include {@link AsyncFilesystemBridge} and {@link SyncFilesystemBridge}, which
 * (respectively) read and write asynchronously and synchronously.</p>
 */
public abstract class AbstractFilesystemBridge implements ConfigBridge<FileConfigNode> {
    private final Path root;

    protected record InputNode(@NotNull File file, @NotNull ConfigNode children) {}

    protected record OutputNode(@NotNull FileConfigNode node, @NotNull Path path) {}

    public AbstractFilesystemBridge(@NotNull Path root) {
        this.root = Objects.requireNonNull(root);
    }

    @Override
    public @NotNull Future<FileConfigNode> read() throws IOException {
        try {
            return callRead(() -> {
                File rootFile = root.toFile();

                if(!validateFile(rootFile)) {
                    //if the filter excludes rootFile, we can't load anything so just throw an exception
                    throw new IllegalArgumentException("root file is not valid");
                }
                else if(!rootFile.isDirectory()) {
                    //root isn't a directory, so read only the root and nothing else
                    return readFile(rootFile);
                }
                else {
                    //root is a directory, so we need to iterate the directory tree
                    FileConfigNode rootConfigNode = new FileConfigNode();

                    Deque<InputNode> stack = new ArrayDeque<>();
                    stack.push(new InputNode(rootFile, rootConfigNode));

                    //handles recursive file structures by only processing each directory once
                    Set<File> visited = new HashSet<>();
                    visited.add(rootFile);

                    while(!stack.isEmpty()) {
                        InputNode currentNode = stack.pop();

                        File[] subFiles = currentNode.file.listFiles(this::validateFile);
                        if(subFiles != null) { //subFiles should never be null, currentNode.file must be a directory
                            for(File subFile : subFiles) {
                                if(subFile.isDirectory()) {
                                    if(visited.add(subFile)) {
                                        //use directory node here as well
                                        ConfigNode childNode = new FileConfigNode();
                                        stack.push(new InputNode(subFile, childNode));
                                        currentNode.children.put(getKeyFor(subFile), childNode);
                                    }
                                }
                                else {
                                    currentNode.children.put(getKeyFor(subFile), readFile(subFile));
                                }
                            }
                        }
                    }

                    return rootConfigNode;
                }
            });
        }
        catch (Exception exception) {
            //rethrow any exceptions as IOException
            throw new IOException(exception);
        }
    }

    @Override
    public @NotNull Future<Void> write(@NotNull FileConfigNode node) throws IOException {
        Objects.requireNonNull(node);

        try {
            return callWrite(() -> {
                File rootFile = root.toFile();

                if(!node.isDirectory()) {
                    //assume rootFile is a non-directory since node is not a directory either
                    //an exception will be thrown here if this is not the case, indicating user error
                    writeFile(rootFile, node);
                }
                else {
                    Deque<OutputNode> stack = new ArrayDeque<>();
                    stack.push(new OutputNode(node, root));

                    Set<Object> visited = new HashSet<>();
                    visited.add(node);

                    while(!stack.isEmpty()) {
                        OutputNode currentNode = stack.pop();

                        for(Map.Entry<String, ConfigElement> childEntry : currentNode.node.entrySet()) {
                            //cast should always succeed: we only push FileConfigNode instances that are DIRECTORIES
                            //onto the stack, and directories are guaranteed to only contain other FileConfigNode
                            //instances as per the additional restrictions placed on put() for that class
                            FileConfigNode childNode = (FileConfigNode)childEntry.getValue().asConfigNode();

                            if(childNode.isDirectory() && visited.add(childNode)) {
                                //node is a directory we haven't visited yet
                                stack.push(new OutputNode(childNode, root.resolve(childEntry.getKey())));
                            }
                            else {
                                //not a directory, so write to the filesystem
                                writeFile(currentNode.path.toFile(), currentNode.node);
                            }
                        }
                    }
                }

                return null;
            });
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public boolean readOnly() {
        return false;
    }

    /**
     * Reads a {@link FileConfigNode} object from a file, which must be non-null
     * @param file the file to read from
     * @return a FileConfigNode containing configuration data from the file
     * @throws IOException if an IO error occurs
     * @throws IllegalArgumentException if file represents a directory, or no codec can be found for the file
     * @throws NullPointerException if file is null
     */
    protected @NotNull FileConfigNode readFile(@NotNull File file) throws IOException {
        Objects.requireNonNull(file);

        //directories are handled specially, not by this function
        if(file.isDirectory()) {
            throw new IllegalArgumentException("cannot read a directory");
        }

        ConfigCodec codec = getCodecFor(file);
        return codec.decodeNode(new FileInputStream(file), true, () -> new FileConfigNode(codec));
    }

    protected void writeFile(@NotNull File file, @NotNull FileConfigNode node) throws IOException {
        if(!node.isDirectory()) {
            node.getCodec().encodeNode(node, new FileOutputStream(file), true, LinkedHashMap::new);
        }
        else {
            throw new IllegalArgumentException("cannot write a FileConfigNode representing a directory");
        }
    }

    protected boolean validateFile(@NotNull File file) {
        if(file.isDirectory()) {
            return true;
        }
        else {
            return CodecRegistry.INSTANCE.hasCodec(PathUtils.getFileExtension(file.toPath()));
        }
    }

    protected @NotNull ConfigCodec getCodecFor(@NotNull File file) {
        if(!file.isDirectory()) {
            //use the file extension to determine what codec to use
            ConfigCodec codec = CodecRegistry.INSTANCE.getCodec(PathUtils.getFileExtension(file.toPath()));
            if(codec != null) {
                return codec;
            }

            throw new IllegalArgumentException("unable to find a codec for file " + file + "; it may use an " +
                    "unsupported format");
        }

        //no codec for directories
        throw new IllegalArgumentException("cannot retrieve a codec for a directory");
    }

    protected @NotNull String getKeyFor(@NotNull File file) {
        return PathUtils.getFileNameWithoutExtension(file.toPath());
    }

    protected abstract Future<FileConfigNode> callRead(@NotNull Callable<FileConfigNode> callable) throws Exception;

    protected abstract Future<Void> callWrite(@NotNull Callable<Void> callable) throws Exception;
}