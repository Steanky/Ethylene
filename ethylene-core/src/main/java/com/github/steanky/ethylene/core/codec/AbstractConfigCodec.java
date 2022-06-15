package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.*;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
     * Represents an <i>output</i> object, which we convert to from an <i>input</i> object (when serializing or
     * deserializing). This record holds a reference to the raw object itself (which may be a map, collection, or some
     * other arbitrary class) as well as a {@link BiConsumer} used to add new elements to the output. If output is a map
     * (or any type that requires a key), the first parameter of the BiConsumer should be used. If it is a collection,
     * array, or some other type that has no concept of a "key", the first parameter may be ignored (it should be null).
     * @param <TOut> the output type
     */
    public record Output<TOut>(@NotNull TOut output,
                               @NotNull BiConsumer<String, TOut> consumer) {}

    /**
     * Empty builder, for use by subclasses.
     */
    protected AbstractConfigCodec() {
    }

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {
        Objects.requireNonNull(element);
        Objects.requireNonNull(output);

        try(output) {
            Object object = GraphTransformer.processRoot(element, new ArrayDeque<>(), new IdentityHashMap<>(),
                    this::makeEncodeNode, e -> !isContainer(e), scalar -> Entry.of(null, serializeElement(scalar)));

            writeObject(object, output);
        }
    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        Objects.requireNonNull(input);

        try (input) {
            return GraphTransformer.processRoot(readObject(input), new ArrayDeque<>(),
                    new IdentityHashMap<>(), this::makeDecodeNode, e -> !isContainer(e), scalar -> Entry.of(null,
                            deserializeObject(scalar)));
        }
    }

    protected @NotNull GraphTransformer.Node<ConfigElement, Object, String> makeEncodeNode(@NotNull ConfigElement target) {
        if(target.isNode()) {
            ConfigNode elementNode = target.asNode();
            Output<Object> outputMap = makeEncodeMap(elementNode.size());

            return new GraphTransformer.Node<>(target, outputMap, elementNode.entryCollection(), outputMap
                    .consumer);
        }
        else if(target.isList()) {
            ConfigList elementList = target.asList();
            Output<Object> outputCollection = makeEncodeCollection(elementList.size());

            return new GraphTransformer.Node<>(target, outputCollection, elementList.entryCollection(),
                    outputCollection.consumer);
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    protected @NotNull GraphTransformer.Node<Object, ConfigElement, String> makeDecodeNode(Object target) {
        if(target instanceof Map<?, ?> map) {
            Output<ConfigElement> output = makeDecodeMap(map.size());

            return new GraphTransformer.Node<>(target, output.output, () -> new Iterator<>() {
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
            }, output.consumer);
        }
        else if(target instanceof List<?> list) {
            Output<ConfigElement> output = makeDecodeCollection(list.size());

            return new GraphTransformer.Node<>(target, output.output, () -> new Iterator<>() {
                private final Iterator<?> backing = list.iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    return Entry.of(null, backing.next());
                }
            }, output.consumer);
        }
        else if(target.getClass().isArray()) {
            Object[] array = (Object[])target;
            Output<ConfigElement> output = makeDecodeCollection(array.length);

            return new GraphTransformer.Node<>(target, output.output, () -> new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < array.length;
                }

                @Override
                public Entry<String, Object> next() {
                    return Entry.of(null, array[i++]);
                }
            }, output.consumer);
        }

        throw new IllegalArgumentException("Invalid input node type " + target.getClass().getTypeName());
    }

    protected @NotNull Output<Object> makeEncodeMap(int size) {
        Map<String, Object> map = new LinkedHashMap<>(size);
        return new Output<>(map, map::put);
    }

    protected @NotNull Output<Object> makeEncodeCollection(int size) {
        Collection<Object> collection = new ArrayList<>(size);
        return new Output<>(collection, (k, v) -> collection.add(v));
    }

    protected @NotNull Output<ConfigElement> makeDecodeMap(int size) {
        ConfigNode node = new LinkedConfigNode(size);
        return new Output<>(node, node::put);
    }

    protected @NotNull Output<ConfigElement> makeDecodeCollection(int size) {
        ConfigList list = new ArrayConfigList(size);
        return new Output<>(list, (k, v) -> list.add(v));
    }

    /**
     * Returns true if this object is non-null and a container (subclass of {@link Collection}, {@link Map}, or an
     * array type).
     * @param input the input object
     * @return true if input is a container and non-null, false if it is not a container or is null
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
     * @throws IllegalStateException if element cannot be converted using {@link ConfigElement#asObject()}
     */
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        return element.asObject();
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