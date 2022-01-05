package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
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

    private FileConfigNode(@NotNull Map<String, ConfigElement> mappings, boolean isDirectory,
                           @Nullable ConfigCodec codec) {
        super(Objects.requireNonNull(mappings));
        this.isDirectory = isDirectory;
        this.codec = codec;
    }

    /**
     * Constructs a new {@link HashMap} based FileConfigNode which represents a directory.
     */
    public FileConfigNode() {
        this(new HashMap<>(), true, null);
    }

    /**
     * Constructs a new FileConfigNode based off of the provided mappings and representing a directory.
     * @param mappings the mappings to construct this instance from
     * @throws NullPointerException if mappings is null or contains any null keys or values
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        this(constructMap(mappings, HashMap::new), true, null);
    }

    /**
     * Constructs a new FileConfigNode with the provided codec. It should be interpreted as a <i>non-directory</i> file.
     * @param codec the codec used to encode file data
     * @throws NullPointerException if codec is null
     */
    public FileConfigNode(@NotNull ConfigCodec codec) {
        this(new LinkedHashMap<>(), false, Objects.requireNonNull(codec));
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

        if(isDirectory) {
            if(!(value instanceof FileConfigNode)) {
                throw new IllegalArgumentException("Directories may only contain FileConfigNode instances");
            }
        }
        else if(value instanceof FileConfigNode) {
            throw new IllegalArgumentException("Non-directories cannot contain other FileConfigNode instances");
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
