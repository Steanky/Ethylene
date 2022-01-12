package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
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
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Represents a <i>tuple</i> (a pair of distinct, unrelated values that may be of different types).
     * @param <TFirst> the type of the first element
     * @param <TSecond> the type of the second element
     */
    public record Entry<TFirst, TSecond>(TFirst first, TSecond second) {}

    /**
     * Represents a specific input <i>container</i> object, and its equivalent output container. A container is a map,
     * array, or collection.
     * @param <TOut> the object held in the output container
     */
    public record Node<TOut>(@NotNull Object input,
                                @NotNull Iterator<? extends Entry<String, ?>> inputIterator,
                                @NotNull Output<TOut> output) {}

    /**
     * Represents an <i>output</i> object, which we convert to from an <i>input</i> object (when serializing or
     * deserializing). This record holds a reference to the raw object itself (which may be a map, collection, or some
     * other arbitrary class) as well as a {@link BiConsumer} used to add new elements to the output. If output is a map
     * (or any type that requires a key), the first parameter of the BiConsumer should be used. If it is a collection,
     * array, or some other type that has no concept of a "key", the first parameter may be ignored (it should be null).
     * @param <TOut> the output type
     */
    public record Output<TOut>(@NotNull Object output,
                                  @NotNull BiConsumer<String, TOut> consumer) {}

    /**
     * Empty constructor, for use by subclasses.
     */
    protected AbstractConfigCodec() {}

    @Override
    public void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException {
        Objects.requireNonNull(element);
        Objects.requireNonNull(output);

        try(output) {
            writeObject(mapInput(element, this::serializeElement, this::makeEncodeMap, this::makeEncodeCollection,
                    ConfigElement.class, Object.class), output);
        }
    }

    @Override
    public @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException {
        Objects.requireNonNull(input);

        try (input) {
            return mapInput(readObject(input), this::deserializeObject, this::makeDecodeMap, this::makeDecodeCollection,
                    Object.class, ConfigElement.class);
        }
    }

    /**
     * Produces a {@link Output} object used to hold a reference to the <i>output map</i> used during encoding.
     * @param <TOut> the output type
     * @return an Output object containing a reference to an output map
     */
    protected <TOut> @NotNull Output<TOut> makeEncodeMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        return new Output<>(map, map::put);
    }

    /**
     * Produces a {@link Output} object used to hold a reference to the <i>output collection</i> used during encoding.
     * @param <TOut> the output type
     * @return an Output object containing a reference to an output collection
     */
    protected <TOut> @NotNull Output<TOut> makeEncodeCollection() {
        Collection<Object> collection = new ArrayList<>();
        return new Output<>(collection, (k, v) -> collection.add(v));
    }

    /**
     * Produces a {@link Output} object used to hold a reference to the <i>output map</i> used during decoding.
     * @param <TOut> the output type
     * @return an Output object containing a reference to an output map
     */
    protected <TOut extends ConfigElement> @NotNull Output<TOut> makeDecodeMap() {
        ConfigNode node = new LinkedConfigNode();
        return new Output<>(node, node::put);
    }

    /**
     * Produces a {@link Output} object used to hold a reference to the <i>output collection</i> used during decoding.
     * @param <TOut> the output type
     * @return an Output object containing a reference to an output collection
     */
    protected <TOut extends ConfigElement> @NotNull Output<TOut> makeDecodeCollection() {
        ConfigList list = new ArrayConfigList();
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
        return input != null && (input instanceof Collection<?> || input instanceof Map<?, ?> ||
                input.getClass().isArray());
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
     * Constructs a {@link Node} object from an input container object, and using the given suppliers to create an
     * equivalent output {@link Map} or {@link Collection}.
     * @param inputContainer the input object, for which {@link AbstractConfigCodec#isContainer(Object)} should return
     *                       true
     * @param mapSupplier the supplier used to produce the output map
     * @param collectionSupplier the supplier used to produce the output collection
     * @param <TOut> the type of object contained in the output map or collection
     * @return a new node containing the input, output, and a {@link BiConsumer} used to add elements
     * @throws IllegalArgumentException if inputContainer is not a valid container (map, collection, or array)
     */
    protected <TOut> @NotNull Node<TOut> makeNode(@NotNull Object inputContainer,
                                                  @NotNull Supplier<Output<TOut>> mapSupplier,
                                                  @NotNull Supplier<Output<TOut>> collectionSupplier) {
        if(inputContainer instanceof Map<?, ?> inputMap) {
            return new Node<>(inputContainer, inputMap.entrySet().stream().map(entry -> new Entry<>(entry.getKey()
                    .toString(), entry.getValue())).iterator(), mapSupplier.get());
        }
        else if(inputContainer instanceof Collection<?> inputCollection) {
            return new Node<>(inputContainer, inputCollection.stream().map(entry -> new Entry<>((String)null, entry))
                    .iterator(), collectionSupplier.get());
        }
        else if(inputContainer.getClass().isArray()) {
            int length = Array.getLength(inputContainer);

            Iterator<? extends Entry<String, ?>> arrayIterator = new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < length;
                }

                @Override
                public Entry<String, ?> next() {
                    return new Entry<>(null, Array.get(inputContainer, index++));
                }
            };

            return new Node<>(inputContainer, arrayIterator, collectionSupplier.get());
        }
        else {
            throw new IllegalArgumentException("Invalid input type " + inputContainer.getClass().getName());
        }
    }

    /**
     * Processes a particular {@link Node}. The node's input container will be iterated. New Node objects will be
     * created for every container object contained in this one, if applicable. All objects will be added to the current
     * node's output. Any objects contained in the input container that are not containers will be converted to the
     * output type using the provided {@link Function} scalarMapper.
     * @param node the node to process
     * @param stack the current node stack
     * @param visited a map of the containers visited so far
     * @param scalarMapper the mapping function used to convert scalars
     * @param mapSupplier the {@link Supplier} used to construct new output maps
     * @param collectionSupplier the supplier used to construct new output collections
     * @param inClass the superclass of all input objects
     * @param outClass the superclass of all output objects
     * @param <TIn> the type of input object
     * @param <TOut> the type of output object
     */
    protected <TIn, TOut> void processNode(@NotNull Node<TOut> node,
                                           @NotNull Deque<Node<TOut>> stack,
                                           @NotNull Map<Object, TOut> visited,
                                           @NotNull Function<TIn, TOut> scalarMapper,
                                           @NotNull Supplier<Output<TOut>> mapSupplier,
                                           @NotNull Supplier<Output<TOut>> collectionSupplier,
                                           @NotNull Class<TIn> inClass,
                                           @NotNull Class<TOut> outClass) {
        while(node.inputIterator.hasNext()) {
            Entry<String, ?> pair = node.inputIterator.next();

            if(!isContainer(pair.second)) {
                node.output.consumer.accept(pair.first, scalarMapper.apply(inClass.cast(pair.second)));
            }
            else {
                if(visited.containsKey(pair.second)) {
                    node.output.consumer.accept(pair.first, visited.get(pair.second));
                }
                else {
                    Node<TOut> newNode = makeNode(pair.second, mapSupplier, collectionSupplier);
                    TOut newOut = outClass.cast(newNode.output.output);

                    stack.push(newNode);
                    visited.put(pair.first, newOut);

                    node.output.consumer.accept(pair.first, newOut);
                }
            }
        }
    }

    /**
     * Maps a provided input, deeply iterating all elements and converting them according to a provided {@link Function}
     * used to map scalars (non-containers). Container objects will be converted to {@link Map} or {@link Collection},
     * provided by the given {@link Supplier}s.
     * @param input the input object
     * @param scalarMapper the function used to map non-containers
     * @param mapSupplier the supplier used to produce output maps
     * @param collectionSupplier the supplier used to produce output collections
     * @param inClass the superclass of the input object
     * @param outClass the superclass of the output object
     * @param <TIn> the input type
     * @param <TOut> the output type
     * @return the output object, containing converted elements from the input object
     */
    protected <TIn, TOut> TOut mapInput(@NotNull Object input,
                                        @NotNull Function<TIn, TOut> scalarMapper,
                                        @NotNull Supplier<Output<TOut>> mapSupplier,
                                        @NotNull Supplier<Output<TOut>> collectionSupplier,
                                        @NotNull Class<TIn> inClass,
                                        @NotNull Class<TOut> outClass) {
        if(!isContainer(input)) {
            return scalarMapper.apply(inClass.cast(input));
        }
        else {
            Deque<Node<TOut>> stack = new ArrayDeque<>();
            Node<TOut> node = makeNode(input, mapSupplier, collectionSupplier);
            TOut rootOut = outClass.cast(node.output.output);
            stack.push(node);

            Map<Object, TOut> visited = new IdentityHashMap<>();
            visited.put(input, rootOut);

            while(!stack.isEmpty()) {
                processNode(stack.pop(), stack, visited, scalarMapper, mapSupplier, collectionSupplier, inClass, outClass);
            }

            return rootOut;
        }
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