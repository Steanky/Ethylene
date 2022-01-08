package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Creates a new FileConfigNode instance. If the provided {@link ConfigCodec} is null, this FileConfigNode will be
     * treated as a directory and mappings will be validated accordingly. Directories may only contain other
     * FileConfigNode instances as values, whereas non-directories <i>can't</i> contain <b>any</b> FileConfigNode
     * instances.
     * @param mappings the mappings used to create this instance
     * @param codec the codec to use
     * @throws IllegalArgumentException if codec is null and mappings contains something that isn't a FileConfigNode;
     * otherwise, if codec is non-null and mappings contains a FileConfigNode
     * @throws NullPointerException if mappings is null
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings, @Nullable ConfigCodec codec) {
        super(constructMap(mappings, LinkedHashMap::new, element -> isElementValid(element, codec == null)));

        this.isDirectory = codec == null;
        this.codec = codec;
    }

    /**
     * Creates a new FileConfigNode using the specified codec. If codec is null, the FileConfigNode will be a directory.
     * If codec is non-null, it will be a non-directory.
     * @param codec the codec to use, or null if this FileConfigNode should be a directory
     */
    public FileConfigNode(@Nullable ConfigCodec codec) {
        super(new LinkedHashMap<>());
        this.isDirectory = codec == null;
        this.codec = codec;
    }

    /**
     * Creates a new FileConfigNode representing a directory, from the given mappings.
     * @param mappings the mappings to use
     * @throws IllegalArgumentException if mappings contains any object not an instance of FileConfigNode
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        this(mappings, null);
    }

    /**
     * Creates a new FileConfigNode object representing a directory.
     */
    public FileConfigNode() {
        this((ConfigCodec) null);
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
            throw new IllegalArgumentException("Invalid element type for this FileConfigNode");
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
