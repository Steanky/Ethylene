package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * <p>This class contains functionality common to {@link ConfigCodec} implementations.</p>
 *
 * <p>In addition to the two abstract methods, {@link AbstractConfigCodec#readObject(InputStream)} and
 * {@link AbstractConfigCodec#writeObject(Object, OutputStream)}, almost every method is designed to be readily
 * subclassed by implementations. In particular, specialized codecs may enable processing of custom objects that are not
 * natively supported by this class.</p>
 *
 * <p>Implementations of this class should guarantee thread safety: multiple threads must be able to safely call methods
 * of this class concurrently, without risk of data corruption or deadlock. Since multithreaded usage is common,
 * implementations are strongly encouraged to ensure that they are well optimized in this case.</p>
 *
 * <p>Many of the methods make use of functionality exposed by the static utility class {@link Graph}.</p>
 */
public abstract class AbstractConfigCodec implements ConfigCodec {
    private final int graphEncodeOptions;
    private final int graphDecodeOptions;

    /**
     * The shared {@link Graph.InputEntry}, used to reduce object allocation during graph traversal. The value is
     * thread-local to ensure consistent behavior in multithreaded environments, and must be cleared after every call
     * to {@link ConfigCodec#encode(ConfigElement, OutputStream)} or {@link ConfigCodec#decode(InputStream)}. Subclasses
     * implementing their own {@link AbstractConfigCodec#makeDecodeNode(Object)} or similar should consider using this
     * field instead of creating their own ad hoc InputEntry instances.
     */
    protected static final ThreadLocal<Graph.InputEntry<String, Object, ConfigElement>> INPUT_ENTRY =
        ThreadLocal.withInitial(Graph::nullEntry);

    /**
     * Constructor, for use by implementing subclasses.
     *
     * @param graphEncodeOptions the {@link Graph.Options} bit flags used during encoding
     * @param graphDecodeOptions the bit flags using during decoding
     */
    protected AbstractConfigCodec(int graphEncodeOptions, int graphDecodeOptions) {
        this.graphEncodeOptions = graphEncodeOptions;
        this.graphDecodeOptions = graphDecodeOptions;
    }

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {
        Graph.InputEntry<String, Object, ConfigElement> localEntry = INPUT_ENTRY.get();

        try (output) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(output);

            ElementType type = element.type();
            if (!supportedTopLevelTypes().contains(type)) {
                throw new IOException(
                    "Top-level elements of type '" + type + "' not supported by '" + getName() + "' codec");
            }

            writeObject(Graph.process(element, this::makeEncodeNode, this::isContainer, this::serializeElement,
                graphEncodeOptions), output);
        }
        finally {
            // always clear the entry after use: old data would never be accessed, but wouldn't get GC'd as long as the
            // thread lives, which may be problematic for large configuration
            localEntry.clear();
        }
    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        Graph.InputEntry<String, Object, ConfigElement> localEntry = INPUT_ENTRY.get();

        try (input) {
            Objects.requireNonNull(input);

            return Graph.process(readObject(input), this::makeDecodeNode, this::isContainer, this::deserializeObject,
                graphDecodeOptions);
        }
        finally {
            localEntry.clear();
        }
    }

    /**
     * Reads an object from the given {@link InputStream}, which should contain configuration data in a particular
     * format.
     *
     * <p>This method may or may not close the input stream. Callers should therefore defensively close the stream, to
     * ensure consistent behavior across different serialization formats.</p>
     *
     * @param input the InputStream to read from
     * @return an object
     * @throws IOException if an IO error occurs
     */
    protected abstract @NotNull Object readObject(@NotNull InputStream input) throws IOException;

    /**
     * Creates a {@link Graph.Node} object from a given target object; used during decoding. Must be overridden by
     * codecs whose container objects (maps, lists) are not subclasses of {@link Map}, {@link Collection}, or are
     * arrays.
     *
     * @param target the target object
     * @return a new graph node
     */
    protected @NotNull Graph.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        Graph.InputEntry<String, Object, ConfigElement> localEntry = INPUT_ENTRY.get();

        if (target instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return Graph.emptyNode(makeDecodeMap(0));
            }

            return Graph.node(new Iterator<>() {
                private final Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Graph.InputEntry<String, Object, ConfigElement> next() {
                    Map.Entry<?, ?> next = iterator.next();
                    localEntry.setKey(next.getKey().toString());
                    localEntry.setValue(next.getValue());

                    return localEntry;
                }
            }, makeDecodeMap(map.size()));
        } else if (target instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return Graph.emptyNode(makeDecodeCollection(0));
            }

            return Graph.node(new Iterator<>() {
                private final Iterator<?> backing = collection.iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Graph.InputEntry<String, Object, ConfigElement> next() {
                    localEntry.setValue(backing.next());
                    return localEntry;
                }
            }, makeDecodeCollection(collection.size()));
        } else if (target.getClass().isArray()) {
            Object[] array = (Object[]) target;
            if (array.length == 0) {
                return Graph.emptyNode(makeDecodeCollection(0));
            }

            return Graph.node(new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < array.length;
                }

                @Override
                public Graph.InputEntry<String, Object, ConfigElement> next() {
                    localEntry.setValue(array[i++]);
                    return localEntry;
                }
            }, makeDecodeCollection(array.length));
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    /**
     * Deserializes a given object, as it is typically produced by a particular format deserializer.
     *
     * @param object the object to deserialize
     * @return the resulting ConfigElement
     * @throws IllegalArgumentException if object is not a valid type for {@link ConfigPrimitive}
     */
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        return ConfigPrimitive.of(object);
    }

    /**
     * Creates a {@link Graph.Output} corresponding to a map. May be overridden by subclasses wishing to further
     * customize output {@link ConfigNode} generation during decoding.
     *
     * @param size the size of the map; this is a hint for more efficient construction and may be ignored
     * @return a new output object linked to a ConfigNode
     */
    protected @NotNull Graph.Output<ConfigElement, String> makeDecodeMap(int size) {
        ConfigNode node = new LinkedConfigNode(size);
        return Graph.output(node, (k, v, b) -> node.put(k, v));
    }

    /**
     * Creates a {@link Graph.Output} corresponding to a list. May be overriden by subclasses withing to further
     * customize {@link ConfigList} generation during decoding.
     *
     * @param size the size of the map; this is a hint for more efficient construction and may be ignored
     * @return a new output object linked to a ConfigList
     */
    protected @NotNull Graph.Output<ConfigElement, String> makeDecodeCollection(int size) {
        ConfigList list = new ArrayConfigList(size);
        return Graph.output(list, (k, v, b) -> list.add(v));
    }

    /**
     * Writes an object to the given {@link OutputStream}, which will then contain configuration data in some particular
     * format.
     * <p>
     * This method may or may not close the OutputStream. Callers should therefore defensively close the stream
     * themselves, to ensure consistent behavior across different formats.
     *
     * @param object the object to write
     * @param output the OutputStream to write to
     * @throws IOException if an IO error occurs
     */
    protected abstract void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException;

    /**
     * Creates a {@link Graph.Node} object from a given target element; used during encoding. May be overridden to
     * enable support for additional container-like implementations of {@link ConfigElement}, if necessary.
     *
     * @param target the target object
     * @return a new graph node
     */
    protected @NotNull Graph.Node<ConfigElement, Object, String> makeEncodeNode(@NotNull ConfigElement target) {
        if (target.isNode()) {
            ConfigNode elementNode = target.asNode();
            return Graph.node(Graph.iterator(elementNode.entryCollection().iterator()), makeEncodeMap(elementNode.size()));
        } else if (target.isList()) {
            ConfigList elementList = target.asList();
            return Graph.node(Graph.iterator(elementList.entryCollection().iterator()), makeEncodeCollection(elementList.size()));
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    /**
     * Returns true if this object is non-null and a container (subclass of {@link Collection}, {@link Map}, or an array
     * type).
     *
     * @param input the input object
     * @return true if input is a container and non-null, false if it is not a container or is null
     */
    @Contract("null -> false")
    protected boolean isContainer(@Nullable Object input) {
        return input != null &&
            (input instanceof Collection<?> || input instanceof Map<?, ?> || input.getClass().isArray());
    }

    /**
     * Serializes a {@link ConfigElement} object (converts it into an object which should be interpreted by a particular
     * format serializer). This function only applies to <i>scalars</i> (i.e. objects that are not containers; such as
     * Number or String).
     *
     * @param element the ConfigElement to convert
     * @return the serialized object
     * @throws IllegalStateException if element cannot be converted using {@link ConfigElement#asScalar()}
     */
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        return element.asScalar();
    }

    /**
     * Creates a {@link Graph.Output} bound to a {@link LinkedHashMap}, used during encoding. Subclasses may override
     * this to customize the objects produced during encoding.
     *
     * @param size the size hint; may be ignored
     * @return a new graph output bound to a map
     */
    protected @NotNull Graph.Output<Object, String> makeEncodeMap(int size) {
        Map<String, Object> map = new LinkedHashMap<>(size);
        return Graph.output(map, (k, v, b) -> map.put(k, v));
    }

    /**
     * Creates a {@link Graph.Output} bound to an {@link ArrayList}, used during encoding. Subclasses may override this
     * to customize the objects produced during encoding.
     *
     * @param size the size hint; may be ignored
     * @return a new graph output bound to a collection
     */
    protected @NotNull Graph.Output<Object, String> makeEncodeCollection(int size) {
        Collection<Object> collection = new ArrayList<>(size);
        return Graph.output(collection, (k, v, b) -> collection.add(v));
    }
}