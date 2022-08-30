package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.*;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * <p>This class contains functionality common to many {@link ConfigCodec} implementations. Many of its methods are
 * designed to deeply iterate hierarchical data structures.</p>
 *
 * <p>In addition to the two abstract methods, {@link AbstractConfigCodec#readObject(InputStream)} and
 * {@link AbstractConfigCodec#writeObject(Object, OutputStream)}, almost every method is designed to be readily
 * subclassed by implementations. In particular, specialized codecs may enable processing of custom objects that are
 * not natively supported by this class.</p>
 */
public abstract class AbstractConfigCodec implements ConfigCodec {
    /**
     * Empty constructor, for use by subclasses.
     */
    protected AbstractConfigCodec() {}

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {
        Objects.requireNonNull(element);
        Objects.requireNonNull(output);

        try(output) {
            writeObject(GraphTransformer.process(element, this::makeEncodeNode, this::isContainer,
                    this::serializeElement), output);
        }
    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        Objects.requireNonNull(input);

        try (input) {
            return GraphTransformer.process(readObject(input), this::makeDecodeNode, this::isContainer,
                    this::deserializeObject);
        }
    }

    protected @NotNull GraphTransformer.Node<ConfigElement, Object, String> makeEncodeNode(@NotNull ConfigElement target) {
        if(target.isNode()) {
            ConfigNode elementNode = target.asNode();
            return new GraphTransformer.Node<>(target, elementNode.entryCollection().iterator(),
                    makeEncodeMap(elementNode.size()));
        }
        else if(target.isList()) {
            ConfigList elementList = target.asList();
            return new GraphTransformer.Node<>(target, elementList.entryCollection().iterator(),
                    makeEncodeCollection(elementList.size()));
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    protected @NotNull GraphTransformer.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        if(target instanceof Map<?, ?> map) {
            return new GraphTransformer.Node<>(target, new Iterator<>() {
                private final Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    Map.Entry<?, ?> next = iterator.next();
                    return Entry.of(next.getKey().toString(), next.getValue());
                }
            }, makeDecodeMap(map.size()));
        }
        else if(target instanceof List<?> list) {
            return new GraphTransformer.Node<>(target, new Iterator<>() {
                private final Iterator<?> backing = list.iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    return Entry.of(null, backing.next());
                }
            }, makeDecodeCollection(list.size()));
        }
        else if(target.getClass().isArray()) {
            Object[] array = (Object[])target;

            return new GraphTransformer.Node<>(target, new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < array.length;
                }

                @Override
                public Entry<String, Object> next() {
                    return Entry.of(null, array[i++]);
                }
            }, makeDecodeCollection(array.length));
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    protected @NotNull GraphTransformer.Output<Object, String> makeEncodeMap(int size) {
        Map<String, Object> map = new LinkedHashMap<>(size);
        return new GraphTransformer.Output<>(map, map::put);
    }

    protected @NotNull GraphTransformer.Output<Object, String> makeEncodeCollection(int size) {
        Collection<Object> collection = new ArrayList<>(size);
        return new GraphTransformer.Output<>(collection, (k, v) -> collection.add(v));
    }

    protected @NotNull GraphTransformer.Output<ConfigElement, String> makeDecodeMap(int size) {
        ConfigNode node = new LinkedConfigNode(size);
        return new GraphTransformer.Output<>(node, node::put);
    }

    protected @NotNull GraphTransformer.Output<ConfigElement, String> makeDecodeCollection(int size) {
        ConfigList list = new ArrayConfigList(size);
        return new GraphTransformer.Output<>(list, (k, v) -> list.add(v));
    }

    /**
     * Returns true if this object is non-null and a container (subclass of {@link Collection}, {@link Map}, or an
     * array type).
     * @param input the input object
     * @return true if input is a container and non-null, false if it is not a container or is null
     */
    @Contract("null -> false")
    protected boolean isContainer(@Nullable Object input) {
        return input != null && (input instanceof Collection<?> || input instanceof Map<?, ?> || input.getClass()
                .isArray());
    }

    /**
     * Serializes a {@link ConfigElement} object (converts it into an object which should be interpreted by a particular
     * format serializer). This function only applies to <i>scalars</i> (i.e. objects that are not containers; such as
     * Number or String).
     * @param element the ConfigElement to convert
     * @return the serialized object
     * @throws IllegalStateException if element cannot be converted using {@link ConfigElement#asScalar()}
     */
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        return element.asScalar();
    }

    /**
     * Deserializes a given object, as it is typically produced by a particular format deserializer.
     * @param object the object to deserialize
     * @return the resulting ConfigElement
     * @throws IllegalArgumentException if object is not a valid type for {@link ConfigPrimitive}
     */
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        return new ConfigPrimitive(object);
    }

    /**
     * Reads an object from the given {@link InputStream}, which should contain configuration data in a particular
     * format.
     * @param input the InputStream to read from
     * @return an object
     * @throws IOException if an IO error occurs
     */
    protected abstract @NotNull Object readObject(@NotNull InputStream input) throws IOException;

    /**
     * Writes an object to the given {@link OutputStream}, which will then contain configuration data in some particular
     * format.
     * @param object the object to write
     * @param output the OutputStream to write to
     * @throws IOException if an IO error occurs
     */
    protected abstract void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException;
}