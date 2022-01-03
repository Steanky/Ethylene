package com.github.steanky.ethylene.collection;

import com.github.steanky.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of {@link ConfigNode} based off of {@link LinkedHashMap}. It is insertion-ordered.
 */
public class LinkedConfigNode extends AbstractConfigNode {
    /**
     * Constructs a new LinkedConfigNode backed by an empty {@link LinkedHashMap}.
     */
    public LinkedConfigNode() {
        super(new LinkedHashMap<>());
    }

    /**
     * Constructs a new LinkedConfigNode with the same entries as the provided map, backed by a {@link LinkedHashMap}.
     * @param mappings the mappings to initialize this object with
     */
    public LinkedConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        super(constructMap(mappings, LinkedHashMap::new));
    }
}
