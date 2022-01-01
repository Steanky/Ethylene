package com.steank.ethylene.collection;

import com.steank.ethylene.ConfigElement;
import com.steank.ethylene.bridge.ConfigBridge;
import com.steank.ethylene.codec.ConfigCodec;
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
     */
    public FileConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        this(constructMap(mappings, HashMap::new), true, null);
    }

    /**
     * Constructs a new FileConfigNode with the provided codec. It should be interpreted as a file.
     * @param codec the codec used to encode file data
     */
    public FileConfigNode(@NotNull ConfigCodec codec) {
        this(new LinkedHashMap<>(), false, Objects.requireNonNull(codec));
    }

    @Override
    public ConfigElement put(@NotNull String key, @NotNull ConfigElement value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        if(isDirectory) {
            if(!(value instanceof FileConfigNode)) {
                throw new IllegalArgumentException("directories may only contain FileConfigNode instances");
            }
        }
        else if(value instanceof FileConfigNode) {
            throw new IllegalArgumentException("non-directories cannot contain other FileConfigNode instances");
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
     * @return The {@link ConfigCodec} used to encode and decode this node
     * @throws IllegalStateException if this FileConfigNode represents a directory
     */
    public @NotNull ConfigCodec getCodec() {
        if(isDirectory) {
            throw new IllegalStateException("this FileConfigNode represents a directory and therefore has no codec");
        }
        else {
            return codec;
        }
    }
}
