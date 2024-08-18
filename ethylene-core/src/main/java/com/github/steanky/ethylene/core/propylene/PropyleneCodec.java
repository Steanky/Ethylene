package com.github.steanky.ethylene.core.propylene;

import com.github.steanky.ethylene.core.AbstractConfigCodec;
import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.EnumSet;
import java.util.Set;

/**
 * A basic {@link ConfigCodec} implementation for Propylene. The singleton instance can be obtained by referencing
 * {@link PropyleneCodec#INSTANCE}.
 * <p>
 * This class does not extend from {@link AbstractConfigCodec}, as Propylene directly deserializes to
 * {@link ConfigElement} instances rather than needing an extra conversion step.
 *
 * @see ConfigElement#of(String)
 */
public class PropyleneCodec implements ConfigCodec {
    /**
     * The singleton instance for {@link PropyleneCodec}.
     */
    public static final PropyleneCodec INSTANCE = new PropyleneCodec();

    private static final String PREFERRED_EXTENSION = "propylene";
    private static final Set<String> PREFERRED_EXTENSIONS = Set.of(PREFERRED_EXTENSION);

    private PropyleneCodec() {

    }

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(output))) {
            ConfigElements.toString(element, writer);
        }
    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(input))) {
            return Parser.fromReader(reader, this::makeNode, this::makeList);
        }
    }

    @Override
    public @Unmodifiable @NotNull Set<String> getPreferredExtensions() {
        return PREFERRED_EXTENSIONS;
    }

    @Override
    public @NotNull String getPreferredExtension() {
        return PREFERRED_EXTENSION;
    }

    @Override
    public @NotNull String getName() {
        return "Propylene";
    }

    @Override
    public @NotNull Set<ElementType> supportedTopLevelTypes() {
        return EnumSet.allOf(ElementType.class);
    }

    /**
     * Creates a node, given a size hint. Can be overridden by subclasses to customize the {@link ConfigNode}
     * implementation used. The default is {@link LinkedConfigNode}.
     *
     * @param sizeHint the size hint
     * @return a new empty, mutable node
     */
    public @NotNull ConfigNode makeNode(int sizeHint) {
        return new LinkedConfigNode(sizeHint);
    }

    /**
     * Creates a list, given a size hint. Can be overridden by subclasses to customize the {@link ConfigList}
     * implementation used. The default is {@link ArrayConfigList}.
     *
     * @param sizeHint the size hint
     * @return a new empty, mutable list
     */
    public @NotNull ConfigList makeList(int sizeHint) {
        return new ArrayConfigList(sizeHint);
    }
}
