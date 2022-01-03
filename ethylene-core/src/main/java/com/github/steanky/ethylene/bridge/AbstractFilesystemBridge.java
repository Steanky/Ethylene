package com.github.steanky.ethylene.bridge;

import com.github.steanky.ethylene.PathUtils;
import com.github.steanky.ethylene.codec.ConfigCodec;
import com.github.steanky.ethylene.ConfigElement;
import com.github.steanky.ethylene.codec.CodecRegistry;
import com.github.steanky.ethylene.collection.ConfigNode;
import com.github.steanky.ethylene.collection.FileConfigNode;
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
 * <p>AbstractFilesystemBridge implementations must at minimum specify a "root" {@link Path}, which may point to any
 * file, including directories. All file-related operations will occur relative to this path.</p>
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
 * <p>Implementations include {@link AsyncFilesystemBridge} and {@link SyncFilesystemBridge}, which respectively read
 * and write asynchronously and synchronously.</p>
 */
public abstract class AbstractFilesystemBridge implements ConfigBridge<FileConfigNode> {
    private final Path root;

    /**
     * Used to keep track of a <i>file</i> (which is a directory) and a FileConfigNode which should contain other nodes
     * corresponding to the files contained in file's directory.
     */
    protected record Node(@NotNull Path path, @NotNull FileConfigNode children) {}

    /**
     * Constructs a new AbstractFilesystemBridge using the provided {@link Path} as a root.
     * @param root the root path
     * @throws NullPointerException if root is null
     */
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
                    throw new IllegalArgumentException("Root file is not valid");
                }
                else if(!rootFile.isDirectory()) {
                    //root isn't a directory, so read only the root and nothing else
                    return readFile(rootFile);
                }
                else {
                    //root is a directory, so we need to iterate the directory tree
                    FileConfigNode rootConfigNode = new FileConfigNode();

                    Deque<Node> stack = new ArrayDeque<>();
                    stack.push(new Node(root, rootConfigNode));

                    //handles recursive file structures by only processing each directory once
                    Map<File, FileConfigNode> visited = new IdentityHashMap<>();
                    visited.put(rootFile, rootConfigNode);

                    while(!stack.isEmpty()) {
                        Node currentNode = stack.pop();

                        File[] subFiles = currentNode.path.toFile().listFiles(this::validateFile);
                        if(subFiles != null) {
                            //subFiles should never be null, currentNode.path must be a directory
                            for(File subFile : subFiles) {
                                if(subFile.isDirectory()) {
                                    if(!visited.containsKey(subFile)) {
                                        //use directory node here as well
                                        FileConfigNode childNode = new FileConfigNode();
                                        stack.push(new Node(subFile.toPath(), childNode));
                                        visited.put(subFile, childNode);

                                        currentNode.children.put(getKeyFor(subFile), childNode);
                                    }
                                    else {
                                        currentNode.children.put(getKeyFor(subFile), visited.get(subFile));
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
                    Deque<Node> stack = new ArrayDeque<>();
                    stack.push(new Node(root, node));

                    Set<Object> visited = new HashSet<>();
                    visited.add(node);

                    while(!stack.isEmpty()) {
                        Node currentNode = stack.pop();

                        for(Map.Entry<String, ConfigElement> childEntry : currentNode.children.entrySet()) {
                            //cast should always succeed: we only push FileConfigNode instances that are DIRECTORIES
                            //onto the stack, and directories are guaranteed to only contain other FileConfigNode
                            //instances as per the additional restrictions placed on put() for that class
                            FileConfigNode childNode = (FileConfigNode)childEntry.getValue().asNode();

                            if(childNode.isDirectory() && visited.add(childNode)) {
                                //node is a directory we haven't visited yet
                                stack.push(new Node(root.resolve(childEntry.getKey()), childNode));
                            }
                            else {
                                //not a directory, so write to the filesystem
                                writeFile(currentNode.path.toFile(), currentNode.children);
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
     * Reads a {@link FileConfigNode} object from a file, which must be non-null and a non-directory.
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
            throw new IllegalArgumentException("Cannot read a directory");
        }

        ConfigCodec codec = getCodecFor(file);
        return codec.decodeNode(new FileInputStream(file), () -> new FileConfigNode(codec));
    }

    /**
     * Writes a {@link FileConfigNode} object to a file. The node and file must be non-null and not representative of a
     * directory.
     * @param file the file to write to
     * @param node the node to write
     * @throws IOException if an IO error occurs
     * @throws IllegalArgumentException if file represents a directory
     * @throws NullPointerException if file or node are null
     */
    protected void writeFile(@NotNull File file, @NotNull FileConfigNode node) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(node);

        if(!node.isDirectory()) {
            node.getCodec().encodeNode(node, new FileOutputStream(file));
        }
        else {
            throw new IllegalArgumentException("Cannot write a FileConfigNode representing a directory");
        }
    }

    /**
     * <p>Validates a given file to determine if it may be read from, or contains other files. This function is not used
     * for writing, because it is often the case that a file being written will not actually exist beforehand.</p>
     *
     * <p>This function will attempt to look up a {@link ConfigCodec} using the {@link CodecRegistry} instance, with
     * the file's extension used as a name.</p>
     * @param file the file to read from
     * @return true if the file is a directory, or if the file extension can be used to locate a registered codec; false
     * otherwise
     */
    protected boolean validateFile(@NotNull File file) {
        if(file.isDirectory()) {
            return true;
        }
        else {
            return CodecRegistry.INSTANCE.hasCodec(PathUtils.getFileExtension(file.toPath()).toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Attempts to retrieve a {@link ConfigCodec} for the given file, based off of its extension and using the
     * {@link CodecRegistry} instance. If a suitable codec cannot be found for any reason, an exception will be thrown.
     * @param file the file to retrieve a codec for
     * @return the codec used to decode the file
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if a codec cannot be found for the file, or the file represents a directory
     */
    protected @NotNull ConfigCodec getCodecFor(@NotNull File file) {
        Objects.requireNonNull(file);

        if(!file.isDirectory()) {
            //use the file extension to determine what codec to use
            ConfigCodec codec = CodecRegistry.INSTANCE.getCodec(PathUtils.getFileExtension(file.toPath())
                    .toLowerCase(Locale.ROOT));
            if(codec != null) {
                return codec;
            }

            throw new IllegalArgumentException("Unable to find a codec for file " + file + "; it may use an " +
                    "unsupported format");
        }

        //no codec for directories
        throw new IllegalArgumentException("Cannot retrieve a codec for a directory");
    }

    /**
     * Returns the <i>key</i> used to associate the file with a FileConfigNode. By default, this simply uses the name
     * of the file, minus the extension. For example, the name <b>file.txt</b> will be referenced by the key
     * <b>file</b>.
     * @param file the file to retrieve the key for
     * @return the key associated with the file
     */
    protected @NotNull String getKeyFor(@NotNull File file) {
        return PathUtils.getFileNameWithoutExtension(file.toPath());
    }

    /**
     * Performs a read operation by running the specified {@link Callable}. This call may occur on a different thread.
     * @param callable the callable to invoke
     * @return A {@link Future} object representing the result of the read operation
     * @throws IOException if an IOException occurred when the callable was run
     */
    protected abstract Future<FileConfigNode> callRead(@NotNull Callable<FileConfigNode> callable) throws IOException;

    /**
     * Performs a write operation by executing the specified {@link Callable}. This call may occur on a different
     * thread.
     * @param callable the callable to invoke
     * @return A {@link Future} object representing the result of the write operation
     * @throws IOException if an IOException occurred when the callable was run
     */
    protected abstract Future<Void> callWrite(@NotNull Callable<Void> callable) throws IOException;
}