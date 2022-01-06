package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This implementation of {@link ConfigNode} represents a directory or file. It may be used to enable {@link ConfigNode}
 * objects to contain information about what format they'd like their data to be encoded as when they are written
 * to a filesystem using a {@link ConfigBridge}. "Directory" nodes are treated specially: they supply no codec and may
 * only contain other FileConfigNode instances as direct children. Non-directory nodes represent individual
 * non-directory files, and <i>must</i> supply a codec.
 */
public class FileConfigNode extends AbstractConfigNode {
    private final boolean isDirectory;
    private final ConfigCodec codec;

    /**
     * <p>Constructs a new FileConfigNode from the provided mappings, which may or may not represent a directory
     * depending on what is passed to isDirectory.</p>
     *
     * <p>If isDirectory is true, the input map may <b>only</b> contain FileConfigNode instances, and it <b>must</b>
     * pass in null to the codec parameter. If isDirectory is false, the input map must <b>not</b> contain any
     * FileConfigNode instances and it <b>must</b> provide a non-null {@link ConfigCodec}.</p>
     * @param mappings the mappings to create this FileConfigNode from
     * @param isDirectory whether this node should represent a directory
     * @param codec the codec to use, which must be null if isDirectory is true and non-null if it's false
     * @throws NullPointerException if mappings is null
     * @throws IllegalArgumentException if mappings contains any illegal values, as detailed above, or if codec is
     * expected to be non-null when it isn't and vice versa
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings, boolean isDirectory, ConfigCodec codec) {
        super(constructMap(mappings, LinkedHashMap::new, element -> isElementValid(element, isDirectory)));
        validateDirectoryCodecState(isDirectory, codec);

        this.isDirectory = isDirectory;
        this.codec = codec;
    }

    /**
     * Constructs a new, empty FileConfigNode. This constructor operates similarly to
     * {@link FileConfigNode#FileConfigNode(Map, boolean, ConfigCodec)} in that it has the same semantics regarding
     * correct values for isDirectory and codec.
     * @param isDirectory whether this node should represent a directory
     * @param codec the codec to use, which must be null if isDirectory is true and non-null if it's false
     * @throws IllegalArgumentException if codec is null when it shouldn't be, or non-null when it shouldn't be
     */
    public FileConfigNode(boolean isDirectory, ConfigCodec codec) {
        super(new LinkedHashMap<>());
        validateDirectoryCodecState(isDirectory, codec);

        this.isDirectory = isDirectory;
        this.codec = codec;
    }

    /**
     * Constructs a new FileConfigNode from the provided mappings. The node will represent a directory.
     * @param mappings the mappings to create this FileConfigNode from
     * @throws NullPointerException if mappings is null
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        this(mappings, true, null);
    }

    /**
     * Constructs a new FileConfigNode from the provided codec. The node will not represent a directory.
     * @param codec the codec to use
     * @throws NullPointerException if codec is null
     */
    public FileConfigNode(@NotNull ConfigCodec codec) {
        this(false, Objects.requireNonNull(codec));
    }

    /**
     * Constructs a new FileConfigNode that represents a directory.
     */
    public FileConfigNode() {
        this(true, null);
    }

    private static void validateDirectoryCodecState(boolean isDirectory, ConfigCodec codec) {
        if(isDirectory && codec != null) {
            throw new IllegalArgumentException("Directories may not specify a codec");
        }
        else if(!isDirectory && codec == null)  {
            throw new IllegalArgumentException("Non-directories must specify a codec");
        }
    }

    private static boolean isElementValid(@NotNull ConfigElement element, boolean isDirectory) {
        if(isDirectory) {
            return element instanceof FileConfigNode;
        }
        else {
            return !(element instanceof FileConfigNode);
        }
    }

    /**
     * Adds a key-value pair to this FileConfigNode. This method places additional constraints on the value type:
     *
     * <ul>
     *     <li>If this FileConfigNode instance is a directory, and the value is not an instance of
     *     FileConfigNode, an {@link IllegalArgumentException} will be thrown and the map will remain
     *     unchanged.</li>
     *     <li>Otherwise, if this FileConfigNode is NOT a directory, and the provided value is an instance of
     *     an {@link IllegalArgumentException} will be thrown and the map will remain unchanged.</li>
     * </ul>
     *
     * Otherwise, this method behaves exactly as {@link ConfigNode#put(Object, Object)}.
     * @param key the key to be associated with value
     * @param value the value to store
     * @return the previously-present {@link ConfigElement}
     * @throws NullPointerException if any of the arguments are null
     * @throws IllegalArgumentException if the value is not of a valid type for this FileConfigNode
     */
    @Override
    public ConfigElement put(@NotNull String key, @NotNull ConfigElement value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        if(!isElementValid(value, isDirectory)) {
            throw new IllegalStateException("Invalid element type for this FileConfigNode");
        }

        return mappings.put(key, value);
    }

    /**
     * Determines if this FileConfigNode instance represents a directory or not.
     * @return true if this node is a directory, false otherwise
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Obtains the codec which should be used to encode this instance. If this FileConfigNode represents a directory,
     * an {@link IllegalStateException} will be thrown.
     * @return the {@link ConfigCodec} used to encode and decode this node
     * @throws IllegalStateException if this FileConfigNode represents a directory
     */
    public @NotNull ConfigCodec getCodec() {
        if(isDirectory) {
            throw new IllegalStateException("This FileConfigNode represents a directory and therefore has no codec");
        }
        else {
            return codec;
        }
    }
}
