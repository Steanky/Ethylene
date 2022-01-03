package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
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
 * This class provides functionality common to most {@link ConfigCodec} implementations. It specifies two abstract
 * methods: {@link AbstractConfigCodec#readMap(InputStream)} and
 * {@link AbstractConfigCodec#writeMap(Map, OutputStream)}. For some file formats, subclasses need only implement these
 * two methods. Other formats, especially those that provide support for different types of objects, might need to
 * override more.
 */
public abstract class AbstractConfigCodec implements ConfigCodec {
    /**
     * Used to keep track of the parent container when traversing a nested hierarchy.
     * @param <TOut> the type of object we are outputting
     */
    protected record Node<TOut>(@NotNull Object inputContainer, @NotNull BiConsumer<String, TOut> output) {}

    private final Set<String> names;

    /**
     * Constructs a new instance of AbstractConfigCodec with the provided {@link Collection} of names.
     * @param names the names used by this codec
     */
    public AbstractConfigCodec(@NotNull Collection<String> names) {
        this.names = Set.copyOf(names);
    }

    @Override
    public void encodeNode(@NotNull ConfigNode node, @NotNull OutputStream output)
            throws IOException {
        try (output) {
            writeMap(makeMap(node, LinkedHashMap::new), output);
        }
    }

    @Override
    public <TNode extends ConfigNode> @NotNull TNode decodeNode(@NotNull InputStream input,
                                                                @NotNull Supplier<TNode> nodeSupplier)
            throws IOException {
        try (input) {
            return makeNode(readMap(input), nodeSupplier);
        }
    }

    @Override
    public @NotNull Set<String> getNames() {
        return names;
    }

    /**
     * Determines if the provided object is a "container". An object is considered a container if it is non-null and a
     * subclass of {@link Map}, {@link Collection}, or is an array type.
     * @param object the object to test
     * @return true if the object is not null and subclasses Map, Collection, or is an array type; false otherwise
     */
    @Contract("null -> false")
    protected boolean isContainer(@Nullable Object object) {
        return object != null && (object instanceof Map<?, ?> || object instanceof Collection<?> ||
                object.getClass().isArray());
    }

    /**
     * <p>Processes a particular value, which is part of some container (map, array, or collection). If the value itself
     * is a container, a reference to it will be added to the {@link Deque} as a {@link Node}.</p>
     *
     * <p>This method is capable of handling recursive or self-referential data structures (for example, a map which
     * contains itself as a value). The resulting output map will have an equivalent structure.</p>
     * @param value the input value
     * @param stack the current stack, used for processing collections
     * @param visited mappings representing which containers have been visited, and their associated output objects
     * @param currentNode the current node we are processing
     * @param keyString the key string, which may be null if we're currently iterating a collection or array
     * @param mapSupplier a supplier to produce new map objects
     * @param collectionSupplier a supplier to produce new collection objects
     * @param converter a function used to convert objects to TOut
     * @param inClass the value class for the input map
     * @param outClass the value class for the output map
     * @param <TMap> the map type
     * @param <TCollection> the collection type
     * @param <TIn> the type of objects stored in the input container
     * @param <TOut> the type of objects which are stored in the output container(s)
     * @throws IllegalArgumentException if the provided object cannot be converted into TOut by the converter function
     */
    protected <TMap extends Map<String, TOut>,
            TCollection extends Collection<TOut>,
            TIn,
            TOut> void processValue(@Nullable Object value,
                                    @NotNull Deque<Node<TOut>> stack,
                                    @NotNull Map<Object, TOut> visited,
                                    @NotNull Node<TOut> currentNode,
                                    @Nullable String keyString,
                                    @NotNull Supplier<TMap> mapSupplier,
                                    @NotNull Supplier<TCollection> collectionSupplier,
                                    @NotNull Function<TIn, TOut> converter,
                                    @NotNull Class<TIn> inClass,
                                    @NotNull Class<TOut> outClass) {
        if(isContainer(value)) {
            TOut output;

            if(!visited.containsKey(value)) {
                //we found a new container element: we now need to create the correct corresponding output map or
                //collection, add a mapping from the input container to the output map/collection, push our value onto
                //the stack so we'll end up processing it later, and finally call our output consumer with our new node
                BiConsumer<String, TOut> consumer;
                Object outputContainer;

                if(value instanceof Map<?, ?>) {
                    TMap newMap = mapSupplier.get();
                    outputContainer = newMap;
                    consumer = newMap::put;
                }
                else  {
                    TCollection newCollection = collectionSupplier.get();
                    outputContainer = newCollection;
                    consumer = (k, v) -> newCollection.add(v);
                }

                output = outClass.cast(outputContainer);
                stack.push(new Node<>(value, consumer));
                visited.put(value, output);
            }
            else {
                //if visisted contains the value container as a key, it means we found a circular reference. we must
                //preserve the same structure in our output, so do that by using the associated mapping for value but
                //definitely don't push it to the stack! otherwise, any circular references would cause this method to
                //never return
                output = visited.get(value);
            }

            currentNode.output.accept(keyString, output);
        }
        else {
            //not a container object, just an ordinary one, so we can add it directly to our output
            currentNode.output.accept(keyString, converter.apply(inClass.cast(value)));
        }
    }

    /**
     * <p>Deeply processes a map, iterating all of its elements (and the elements of any "container" objects it may
     * contain, recursively). Each element will be "converted" into another object by the provided conversion function.
     * If the input object is an array or a collection, the output object will be constructed by the provided
     * collection supplier. If the input object is a map, the output object will be constructed by the provided map
     * supplier.</p>
     *
     * <p>This function is capable of handling self-referential maps. The output map will have an identical structure,
     * but with different objects.</p>
     * @param input the input map
     * @param rootMapSupplier the supplier which produces the <i>root</i> map which is returned
     * @param subMapSupplier the supplier to produce nested maps
     * @param collectionSupplier the supplier to produce collections
     * @param converter the converter used to convert each object
     * @param inClass the value class for the input map
     * @param outClass the value class for the output map
     * @param <TRootMap> the type of root map (the returned type)
     * @param <TSubMap> the type of sub map
     * @param <TCollection> the type of collection
     * @param <TIn> the input value type
     * @param <TOut> the output value type
     * @return the new map, which will contain all the elements of the input map after they have been converted to
     * appropriate types
     * @throws NullPointerException if rootMapSupplier supplies a null map
     * @throws IllegalArgumentException if any of the input values cannot be converted to TOut
     */
    protected <TRootMap extends Map<String, TOut>,
            TSubMap extends Map<String, TOut>,
            TCollection extends Collection<TOut>,
            TIn,
            TOut> @NotNull TRootMap processMap(@NotNull Map<String, TIn> input,
                                               @NotNull Supplier<TRootMap> rootMapSupplier,
                                               @NotNull Supplier<TSubMap> subMapSupplier,
                                               @NotNull Supplier<TCollection> collectionSupplier,
                                               @NotNull Function<TIn, TOut> converter,
                                               @NotNull Class<TIn> inClass,
                                               @NotNull Class<TOut> outClass) {
        TRootMap topLevel = Objects.requireNonNull(rootMapSupplier.get(), "root map cannot be null");

        Deque<Node<TOut>> stack = new ArrayDeque<>();
        stack.push(new Node<>(input, topLevel::put));

        Map<Object, TOut> visitedNodes = new IdentityHashMap<>();
        visitedNodes.put(input, outClass.cast(topLevel));

        while(!stack.isEmpty()) {
            Node<TOut> node = stack.pop();

            if(node.inputContainer instanceof Map<?, ?> inputMap) {
                //iterate input entries, process each of them
                for(Map.Entry<?, ?> entry : inputMap.entrySet()) {
                    processValue(entry.getValue(), stack, visitedNodes, node, entry.getKey().toString(), subMapSupplier,
                            collectionSupplier, converter, inClass, outClass);
                }
            }
            else if(node.inputContainer instanceof Collection<?> inputCollection) {
                //just iterate input elements and process them
                for(Object value : inputCollection) {
                    processValue(value, stack, visitedNodes, node, null, subMapSupplier, collectionSupplier,
                            converter, inClass, outClass);
                }
            }
            else {
                //inputContainer must be an array
                int length = Array.getLength(node.inputContainer);

                for(int i = 0; i < length; i++) {
                    processValue(Array.get(node.inputContainer, i), stack, visitedNodes, node, null,
                            subMapSupplier, collectionSupplier, converter, inClass, outClass);
                }
            }
        }

        return topLevel;
    }

    /**
     * Produces a {@link ConfigNode} from the provided mappings.
     * @param mappings the mappings to use to produce this ConfigNode
     * @param nodeSupplier the supplier used to construct the node which is returned
     * @param <TNode> the type of ConfigNode to return
     * @return a ConfigNode, containing the same entries as mappings, after converting them to ConfigElement instances
     * @throws NullPointerException if mappings or nodeSupplier are null
     * @throws IllegalArgumentException if mappings contains a value which cannot be converted into a ConfigElement
     */
    protected <TNode extends ConfigNode> @NotNull TNode makeNode(@NotNull Map<String, Object> mappings,
                                                                 @NotNull Supplier<TNode> nodeSupplier) {
        Objects.requireNonNull(mappings);
        Objects.requireNonNull(nodeSupplier);

        return processMap(mappings, nodeSupplier, LinkedConfigNode::new, ArrayConfigList::new, this::toElement,
                Object.class, ConfigElement.class);
    }

    /**
     * Produces a map from the provided {@link ConfigNode}. The returned map will contain the same entries as the
     * ConfigNode, with all ConfigElement instances converted to their equivalent values. ConfigNode instances will be
     * converted to {@link LinkedHashMap}, ConfigList instances to {@link ArrayList}, and {@link ConfigPrimitive}
     * instances to the value that they wrap.
     * @param node the node to convert to a map
     * @param mapSupplier the supplier which produces the <i>top level</i> map which is returned
     * @param <TMap> the type of top-level map
     * @return a map, constructed by mapSupplier, containing the same mappings as node, after deeply converting every
     * element to an equivalent value
     * @throws NullPointerException if node or mapSupplier are null
     */
    protected <TMap extends Map<String, Object>> @NotNull TMap makeMap(@NotNull ConfigNode node,
                                                                       @NotNull Supplier<TMap> mapSupplier) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(mapSupplier);

        return processMap(node, mapSupplier, LinkedHashMap::new, ArrayList::new, this::toObject, ConfigElement.class,
                Object.class);
    }

    /**
     * Converts the provided object to a ConfigElement. The input object will typically not be a container.
     * Implementations that override this method will most often also need to override
     * {@link AbstractConfigCodec#toObject(ConfigElement)}.
     * @param raw the object to convert
     * @return a ConfigElement representing the given object
     * @throws IllegalArgumentException if the provided object cannot be converted
     */
    protected @NotNull ConfigElement toElement(@Nullable Object raw) {
        return new ConfigPrimitive(raw);
    }

    /**
     * Converts the provided ConfigElement to an object. The input ConfigElement will typically not be a container.
     * Implementations that override this method will most often also need to override
     * {@link AbstractConfigCodec#toElement(Object)}.
     * @param element the element to convert
     * @return an object representing the given ConfigElement
     * @throws IllegalArgumentException if the provided object cannot be converted
     */
    protected @Nullable Object toObject(@NotNull ConfigElement element) {
        if(element.isObject()) {
            return element.asObject();
        }
        else {
            throw new IllegalArgumentException("Cannot convert ConfigElement subclass of type " +
                    element.getClass().getName());
        }
    }

    /**
     * Reads some mappings from an InputStream. This method need not ensure that the stream closes.
     * @param input an InputStream to read from
     * @return a map of strings to objects
     * @throws IOException if an IO error occurs
     */
    protected abstract @NotNull Map<String, Object> readMap(@NotNull InputStream input) throws IOException;

    /**
     * Writes some mappings to an OutputStream. This method need not ensure that the stream closes.
     * @param mappings the mappings to write
     * @param output the stream to write to
     * @throws IOException if an IO error occurs
     */
    protected abstract void writeMap(@NotNull Map<String, Object> mappings, @NotNull OutputStream output)
            throws IOException;
}