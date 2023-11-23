package com.github.steanky.ethylene.core;

import com.github.steanky.toolkit.collection.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This utility class can be used to aid the process of designing generalized topology-preserving object graph
 * transformations. It is used by many of Ethylene's components, whenever it is necessary to deeply traverse graph-like
 * structures of any kind, or create a new topologically-equivalent graph comprised of different objects from a given
 * input graph.
 * <p>
 * Explanations for the terms used throughout this class's documentation are shown below:
 * <ul>
 *     <li>Child: an object directly referenced by a parent</li>
 *     <li>Parent: an object which may have any number of references to children</li>
 *     <li>Object graph: a data structure consisting of <i>nodes</i> which may have any number of children, which may
 *     include other nodes</li>
 *     <li>Scalar: an object in an object graph which cannot have children, but may be a child of a node</li>
 *     <li>Root element: the node or scalar at the root of the object graph; this is the entrypoint of said graph</li>
 *     <li>Topology: the shape of the object graph; the specific connections of its parent-child relationships</li>
 * </ul>
 */
public final class Graph {
    //shared empty accumulator which is just a no-op
    private static final Accumulator<?, ?> EMPTY_ACCUMULATOR = (Accumulator<Object, Object>) (o, o2, circular) -> {
    };
    //shared empty Output with null data and the empty accumulator
    private static final Output<?, ?> EMPTY_OUTPUT = new Output<>(null, EMPTY_ACCUMULATOR);
    //shared empty Node with an empty iterator and the empty output
    private static final Node<?, ?, ?> EMPTY_NODE = new Node<>(Iterators.iterator(), emptyOutput());

    private Graph() {
        throw new UnsupportedOperationException();
    }

    /**
     * Convenience overload for
     * {@link Graph#process(Object, Function, Predicate, Function, Function, Supplier, Supplier, int)} that uses the
     * identity function for visit key mapping, the parameterless {@link IdentityHashMap} constructor for constructing
     * references, and the parameterless {@link ArrayDeque} constructor for the node stack.
     *
     * @param rootInput          the input object; this is the root of the input graph
     * @param nodeFunction       the function used to create new {@link Node}s
     * @param containerPredicate the {@link Predicate} used to determine "container" objects (from which Nodes will be
     *                           made; true values indicate the object is a container, false indicates it is a scalar
     * @param scalarMapper       the function used to map scalar input to scalar output
     * @param flags              the flags which determine traversal and translation behavior; see
     *                           {@link Graph.Options}
     * @param <TIn>              the input type
     * @param <TOut>             the output type
     * @param <TKey>             the key type
     * @return the root of the new object graph
     */
    public static <TIn, TOut, TKey> TOut process(TIn rootInput,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        int flags) {
        return process(rootInput, nodeFunction, containerPredicate, scalarMapper, Function.identity(),
            IdentityHashMap::new, ArrayDeque::new, flags);
    }

    /**
     * Processes the given input graph, starting at the root object {@code rootInput}. The exact behavior is dependent
     * on the {@code flags} parameter (see {@link Graph.Options} for details).
     *
     * @param rootInput          the input object; this is the root of the input graph
     * @param nodeFunction       the function used to create new {@link Node}s
     * @param containerPredicate the {@link Predicate} used to determine "container" objects (from which Nodes will be
     *                           made; true values indicate the object is a container, false indicates it is a scalar)
     * @param scalarMapper       the function used to map scalar input to scalar output
     * @param visitKeyMapper     the function used to map input objects to keys used to track reference identity; can be
     *                           null if references aren't being tracked
     * @param visitedSupplier    the supplier used to create the visitation map; can be null if references aren't being
     *                           tracked
     * @param stackSupplier      the supplier used to create the node stack; can be null if the input object is a
     *                           scalar, or if
     * @param flags              the flags which determine traversal and translation behavior; see
     *                           {@link Graph.Options}
     * @param <TIn>              the input type
     * @param <TOut>             the output type
     * @param <TKey>             the key type
     * @param <TVisit>           the type of the objects used to track input references
     * @return the root of the new object graph
     */
    public static <TIn, TOut, TKey, TVisit> TOut process(TIn rootInput,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        Function<? super TIn, ? extends TVisit> visitKeyMapper,
        Supplier<? extends Map<Object, TOut>> visitedSupplier,
        @NotNull Supplier<? extends Deque<Node<TIn, TOut, TKey>>> stackSupplier, int flags) {
        if (!containerPredicate.test(rootInput)) {
            //if rootInput is a scalar, just return whatever the scalar mapper produces
            //there's nothing more to do
            return scalarMapper.apply(rootInput);
        }

        Node<TIn, TOut, TKey> rootNode = nodeFunction.apply(rootInput);

        //can't iterate an empty node, so just return its data immediately
        if (isEmpty(rootNode)) {
            return rootNode.output.data;
        }

        boolean circularRefSupport = Options.hasOption(flags, Options.TRACK_REFERENCES);
        boolean trackScalarReference = Options.hasOption(flags, Options.TRACK_SCALAR_REFERENCES);
        boolean depthFirst = Options.hasOption(flags, Options.DEPTH_FIRST);
        boolean lazyAccumulation = depthFirst && Options.hasOption(flags, Options.LAZY_ACCUMULATION);

        //don't track node identity if we are not supporting circular references
        //make sure usages of this map check circularRefSupport to avoid NPE
        if (circularRefSupport) {
            rootNode.identity = visitKeyMapper.apply(rootInput);
        }

        Map<Object, TOut> scalarMap = null;
        if (trackScalarReference) {
            scalarMap = visitedSupplier.get();
        }

        Deque<Node<TIn, TOut, TKey>> stack = stackSupplier.get();
        stack.push(rootNode);
        while (!stack.isEmpty()) {
            //guaranteed to be non-empty (empty nodes are never added to the stack)
            Node<TIn, TOut, TKey> node = depthFirst ? stack.peek() : stack.pop();

            boolean hasOutput = hasOutput(node);
            boolean finished = true;
            while (node.inputIterator.hasNext()) {
                InputEntry<? extends TKey, ? extends TIn, ? extends TOut> entry = node.inputIterator.next();

                //input indicated we should fast-exit, do so with the provided value
                if (entry.control == Control.FAST_EXIT) {
                    return entry.fastExitValue;
                }

                TKey entryKey = entry.getKey();
                TIn entryInput = entry.getValue();

                //if not a container, then we have a scalar
                if (!containerPredicate.test(entryInput)) {
                    //nodes that aren't containers have no children, so we can immediately add them to the accumulator
                    if (hasOutput) {
                        TOut out;
                        boolean visited;

                        if (trackScalarReference) {
                            TVisit visit = visitKeyMapper.apply(entryInput);
                            if (scalarMap.containsKey(visit)) {
                                out = scalarMap.get(visit);
                                visited = true;
                            }
                            else {
                                scalarMap.put(visit, out = scalarMapper.apply(entryInput));
                                visited = false;
                            }
                        }
                        else {
                            out = scalarMapper.apply(entryInput);
                            visited = false;
                        }

                        node.output.accumulator.accept(entryKey, out, visited);
                    }

                    continue;
                }

                TVisit visit = null;
                if (circularRefSupport) {
                    //handle already-visited non-scalar nodes, to allow proper handling of circular references
                    visit = visitKeyMapper.apply(entryInput);

                    //check containsKey, null values are allowed in the map
                    if (node.hasParent(visit)) {
                        /*
                        already-visited references are immediately added to the accumulator. if these references are
                        nodes, their output might not have been fully constructed yet. it might not even be possible to
                        ensure that it is constructed, in the case of circular references. therefore, immediately add
                        them to the accumulator, and let it know the reference is circular
                         */
                        if (hasOutput) {
                            node.output.accumulator.accept(entryKey, node.getParent(visit), true);
                        }

                        continue;
                    }
                }

                Node<TIn, TOut, TKey> newNode = nodeFunction.apply(entryInput);
                if (circularRefSupport) {
                    newNode.parent = node;
                    newNode.identity = visit;
                }

                if (isEmpty(newNode)) {
                    if (hasOutput) {
                        //call the accumulator right away, empty nodes cannot have children
                        node.output.accumulator.accept(entryKey, newNode.output.data, false);
                    }

                    //don't bother pushing empty nodes to the stack, they cannot be explored
                    continue;
                }

                //if depth-first, newNode is the next node to be processed
                //if breadth-first, nodes further to the end of this node's iterator will be processed first
                stack.push(newNode);

                if (hasOutput) {
                    if (lazyAccumulation) {
                        //set the current node's result key and out fields
                        node.setResult(entryKey, newNode.output.data);
                    } else {
                        //no lazy accumulation, immediately add this node
                        node.output.accumulator.accept(entryKey, newNode.output.data, false);
                    }
                }

                //depthFirst also means we've only peeked the top node!
                if (depthFirst) {
                    //if depth-first, try to process the next node
                    //we are "unfinished" at this point because we have a child node (newNode)
                    finished = false;
                    break;
                }
            }

            if (finished && depthFirst) {
                //if depth-first, we only peeked the stack node
                //if we're finished, we can pop this node
                stack.pop();

                if (lazyAccumulation) {
                    //when lazily accumulating depth-first, the parent node is at the top of the stack
                    //we will never lazily accumulate when breadth-first
                    Node<TIn, TOut, TKey> old = stack.peek();
                    if (old != null && hasOutput(old) && old.hasResult()) {
                        old.output.accumulator.accept(old.result.key, old.result.out, false);
                    }
                }
            }
        }

        return rootNode.output.data;
    }

    private static boolean isEmpty(Node<?, ?, ?> node) {
        return node == EMPTY_NODE || !node.inputIterator.hasNext();
    }

    private static boolean hasOutput(Node<?, ?, ?> node) {
        return node.output != EMPTY_OUTPUT && node.output.accumulator != EMPTY_ACCUMULATOR;
    }

    /**
     * Convenience overload for
     * {@link Graph#process(Object, Function, Predicate, Function, Function, Supplier, Supplier, int)} that uses the
     * {@link IdentityHashMap} constructor as its visitation map supplier and the {@link ArrayDeque} constructor as its
     * node stack supplier.
     *
     * @param rootInput          the input object; this is the root of the input graph
     * @param nodeFunction       the function used to create new {@link Node}s
     * @param containerPredicate the {@link Predicate} used to determine "container" objects (from which Nodes will be
     *                           made; true values indicate the object is a container, false indicates it is a scalar
     * @param scalarMapper       the function used to map scalar input to scalar output
     * @param visitKeyMapper     the function used to map input objects to keys used to track reference identity; can be
     *                           null if references aren't being tracked
     * @param flags              the flags which determine traversal and translation behavior; see
     *                           {@link Graph.Options}
     * @param <TIn>              the input type
     * @param <TOut>             the output type
     * @param <TKey>             the key type
     * @param <TVisit>           the type of the objects used to track input references
     * @return the root of the new object graph
     */
    public static <TIn, TOut, TKey, TVisit> TOut process(TIn rootInput,
        @NotNull Function<? super TIn, ? extends Node<TIn, TOut, TKey>> nodeFunction,
        @NotNull Predicate<? super TIn> containerPredicate, @NotNull Function<? super TIn, ? extends TOut> scalarMapper,
        @NotNull Function<? super TIn, ? extends TVisit> visitKeyMapper, int flags) {
        return process(rootInput, nodeFunction, containerPredicate, scalarMapper, visitKeyMapper, IdentityHashMap::new,
            ArrayDeque::new, flags);
    }

    /**
     * Creates a new {@link Node} given an iterator and output object.
     *
     * @param inputIterator the input iterator
     * @param output        the output object
     * @param <TIn>         the input type
     * @param <TOut>        the output type
     * @param <TKey>        the input key
     * @return a new node
     */
    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> node(
        @NotNull Iterator<? extends InputEntry<? extends TKey, ? extends TIn, TOut>> inputIterator,
        @NotNull Output<TOut, TKey> output) {
        return new Node<>(inputIterator, output);
    }

    /**
     * Creates a new {@link Node} given an iterator and using the empty output.
     *
     * @param inputIterator the input iterator
     * @param <TIn>         the input type
     * @param <TOut>        the output type
     * @param <TKey>        the input key
     * @return a new node with an empty output
     */
    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> node(
        @NotNull Iterator<? extends InputEntry<? extends TKey, ? extends TIn, TOut>> inputIterator) {
        return new Node<>(inputIterator, emptyOutput());
    }


    /**
     * Returns the shared, empty {@link Output} instance.
     *
     * @param <TKey> the input key
     * @param <TOut> the output type
     * @return the shared empty Output
     */
    @SuppressWarnings("unchecked")
    public static <TKey, TOut> @NotNull Output<TKey, TOut> emptyOutput() {
        return (Output<TKey, TOut>) EMPTY_OUTPUT;
    }

    /**
     * Returns the shared, empty {@link Accumulator} instance.
     *
     * @param <TKey> the key type
     * @param <TOut> the output type
     * @return the shared empty Accumulator
     */
    @SuppressWarnings("unchecked")
    public static <TKey, TOut> @NotNull Accumulator<TKey, TOut> emptyAccumulator() {
        return (Accumulator<TKey, TOut>) EMPTY_ACCUMULATOR;
    }

    /**
     * Creates a new {@link Output} from some given data and an {@link Accumulator}.
     *
     * @param data        the output data
     * @param accumulator the accumulator
     * @param <TOut>      the output data type
     * @param <TKey>      the key type
     * @return a new Output object
     */
    public static <TOut, TKey> @NotNull Output<TOut, TKey> output(TOut data,
        @NotNull Accumulator<? super TKey, ? super TOut> accumulator) {
        return new Output<>(data, accumulator);
    }

    /**
     * The shared, empty {@link Node} with an empty output.
     *
     * @param <TIn>  the input type
     * @param <TOut> the output type
     * @param <TKey> the key type
     * @return an empty node with an empty output
     */
    @SuppressWarnings("unchecked")
    public static <TIn, TOut, TKey> @NotNull Node<TIn, TOut, TKey> emptyNode() {
        return (Node<TIn, TOut, TKey>) EMPTY_NODE;
    }

    /**
     * An accumulator function for a {@link Node}'s {@link Output}.
     *
     * @param <TKey> the key type
     * @param <TOut> the value type
     */
    @FunctionalInterface
    public interface Accumulator<TKey, TOut> {
        /**
         * Accepts a value, adding it to some unspecified object, usually a collection or map.
         *
         * @param key     the key component of the value
         * @param out     the value component
         * @param circular whether this input is a circular reference
         */
        void accept(TKey key, TOut out, boolean circular);
    }

    /**
     * Bit flags for setting various options.
     */
    public static final class Options {
        /**
         * All options are disabled. This corresponds to a breadth-first graph transform with no reference tracking of
         * any sort, and no lazy accumulation.
         */
        public static final int NONE = 0;

        /**
         * Enables support for reference tracking. If this is enabled, all node references will be tracked, whereas
         * scalar references <i>can</i> be tracked only if their corresponding option flag is set. Warning: when this
         * option is not present, any circular references in the input data structure will cause an infinite loop and
         * eventually an OOM. However, if there is some guarantee that such a condition is not possible, leaving this
         * setting disabled can improve performance.
         */
        public static final int TRACK_REFERENCES = 1;

        /**
         * Enables support for tracking scalar references. This option will do nothing if the REFERENCE_TRACKING flag is
         * not also set. Since scalars cannot have children, not having this option will not cause a possibility of
         * infinite loops. However, enabling it may be desirable in order to "de-duplicate" equivalent instances of
         * scalars in the output data structure, at the cost of additional memory being used to store the scalar objects
         * during graph transformation.
         */
        public static final int TRACK_SCALAR_REFERENCES = 2;

        /**
         * Equivalent to combining TRACK_REFERENCES and TRACK_SCALAR_REFERENCES.
         */
        public static final int TRACK_ALL_REFERENCES = TRACK_REFERENCES | TRACK_SCALAR_REFERENCES;

        /**
         * Causes graphs to be processed in a depth-first manner.
         */
        public static final int DEPTH_FIRST = 4;

        /**
         * Enables lazy accumulation of nodes. Child nodes will only be added to their parent accumulator when all the
         * child's child nodes have been added. This option has no effect unless depth-first processing is enabled.
         */
        public static final int LAZY_ACCUMULATION = 8;

        private static boolean hasOption(int options, int option) {
            return (options & option) != 0;
        }
    }

    private enum Control {
        CONTINUE,
        FAST_EXIT
    }

    public static <TKey, TIn, TOut> @NotNull Iterator<InputEntry<TKey, TIn, TOut>> iterator(
        @NotNull Iterator<? extends Map.Entry<? extends TKey, ? extends TIn>> iterator) {
        Objects.requireNonNull(iterator);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public InputEntry<TKey, TIn, TOut> next() {
                Map.Entry<? extends TKey, ? extends TIn> entry = iterator.next();
                return new InputEntry<>(entry.getKey(), entry.getValue(), null, Control.CONTINUE);
            }
        };
    }

    public static <TKey, TIn, TOut> @NotNull InputEntry<TKey, TIn, TOut> entry(TKey key, TIn input) {
        return new InputEntry<>(key, input, null, Control.CONTINUE);
    }

    public static <TKey, TIn, TOut> @NotNull InputEntry<TKey, TIn, TOut> fastExit(TOut result)  {
        return new InputEntry<>(null, null, result, Control.FAST_EXIT);
    }

    public static class InputEntry<TKey, TIn, TOut> implements Map.Entry<TKey, TIn> {
        private final TKey key;
        private final TIn in;
        private final TOut fastExitValue;
        private final Control control;

        private InputEntry(TKey key, TIn in, TOut fastExitValue, Control control) {
            this.key = key;
            this.in = in;
            this.fastExitValue = fastExitValue;
            this.control = control;
        }

        @Override
        public TKey getKey() {
            return key;
        }

        @Override
        public TIn getValue() {
            return in;
        }

        @Override
        public TIn setValue(TIn value) {
            throw new UnsupportedOperationException();
        }
    }

    //used internally for lazy accumulation of results
    //only relevant when doing depth-first transforms
    private static final class NodeResult<TKey, TOut> {
        private TKey key;
        private TOut out;
    }

    /**
     * Represents a specific "node" in a graph, which has an iterator of "inputs" and a single "output" object
     * corresponding to a node in the new object graph.
     *
     * @param <TIn>  the type of input object
     * @param <TOut> the type of output object
     * @param <TKey> the key type
     */
    public static final class Node<TIn, TOut, TKey> {
        private final Iterator<? extends InputEntry<? extends TKey, ? extends TIn, TOut>> inputIterator;
        private final Output<TOut, ? super TKey> output;

        private NodeResult<TKey, TOut> result;

        private Node<TIn, TOut, TKey> parent;
        private Object identity;

        private Node(@NotNull Iterator<? extends InputEntry<? extends TKey, ? extends TIn, TOut>> inputIterator,
            @NotNull Output<TOut, ? super TKey> output) {
            this.inputIterator = inputIterator;
            this.output = output;
        }

        private TOut getParent(@Nullable Object identity) {
            Node<TIn, TOut, TKey> current = this;
            while (current != null) {
                if (current.identity == identity) {
                    return current.output.data;
                }

                current = current.parent;
            }

            return null;
        }

        private boolean hasParent(@Nullable Object identity) {
            Node<TIn, TOut, TKey> current = this;
            while (current != null) {
                if (current.identity == identity) {
                    return true;
                }

                current = current.parent;
            }

            return false;
        }

        private boolean hasResult() {
            return result != null;
        }

        private void setResult(TKey key, TOut out) {
            NodeResult<TKey, TOut> result =
                Objects.requireNonNullElseGet(this.result, () -> this.result = new NodeResult<>());
            result.key = key;
            result.out = out;
        }

        /**
         * Returns the input iterator for this node.
         *
         * @return the input iterator
         */
        public @NotNull Iterator<? extends InputEntry<? extends TKey, ? extends TIn, TOut>> inputIterator() {
            return inputIterator;
        }

        /**
         * Returns the {@link Output} object corresponding to this node.
         *
         * @return the output object
         */
        public @NotNull Output<TOut, ? super TKey> output() {
            return output;
        }
    }

    /**
     * Container object corresponding to an output node on the newly-created graph.
     *
     * @param <TOut> the actual output object
     * @param <TKey> the key type
     */
    public static final class Output<TOut, TKey> {
        private final TOut data;
        private final Accumulator<? super TKey, ? super TOut> accumulator;

        private Output(TOut data, @NotNull Accumulator<? super TKey, ? super TOut> accumulator) {
            this.data = data;
            this.accumulator = accumulator;
        }

        /**
         * The actual output object.
         *
         * @return the output object
         */
        public TOut data() {
            return data;
        }

        /**
         * The accumulator, which is used to add new children to this output.
         *
         * @return this output's accumulator
         */
        public @NotNull Accumulator<? super TKey, ? super TOut> accumulator() {
            return accumulator;
        }
    }
}