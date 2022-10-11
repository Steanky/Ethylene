package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.signature.MatchingSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link ConfigProcessor} which uses the object mapper API in order to convert most types to and from
 * {@link ConfigElement}s.
 *
 * @param <T> the type of object to serialize/deserialize
 */
public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private static final int GRAPH_OPTIONS =
        Graph.Options.DEPTH_FIRST | Graph.Options.TRACK_REFERENCES | Graph.Options.LAZY_ACCUMULATION;

    private final Token<T> token;
    private final SignatureMatcher.Source signatureMatcherSource;
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;
    private final ScalarSource scalarSource;

    /**
     * Creates a new instance of this class.
     *
     * @param token                  the type of object this processor will be able to process
     * @param signatureMatcherSource the {@link SignatureMatcher.Source} used to create signature matchers
     * @param typeHinter             the {@link TypeHinter} used to determine information about types
     * @param typeResolver           the {@link TypeResolver} used to resolve types into concrete implementations
     * @param scalarSource           the {@link ScalarSource} used to produce scalar types from {@link ConfigElement}s,
     *                               and vice versa
     */
    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull SignatureMatcher.Source signatureMatcherSource,
        @NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver, @NotNull ScalarSource scalarSource) {
        this.token = Objects.requireNonNull(token);
        this.signatureMatcherSource = Objects.requireNonNull(signatureMatcherSource);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.scalarSource = Objects.requireNonNull(scalarSource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        try {
            Token<?> rootType = typeResolver.resolveType(token, element);
            SignatureMatcher rootFactory = signatureMatcherSource.matcherFor(rootType);

            return (T) Graph.process(new ClassEntry(rootType, element, rootFactory), nodeEntry -> {
                    ConfigElement nodeElement = nodeEntry.element;
                    MatchingSignature matchingSignature =
                        nodeEntry.signatureMatcher.signatureForElement(nodeEntry.type, nodeElement);

                    Signature<Object> signature = (Signature<Object>) matchingSignature.signature();
                    int signatureSize = matchingSignature.size();

                    Iterator<ConfigElement> elementIterator = matchingSignature.elements().iterator();
                    Iterator<Map.Entry<String, Token<?>>> typeEntryIterator = signature.argumentTypes().iterator();

                    //if this signature supports circular refs, buildingObject should be non-null
                    Object buildingObject =
                        signature.hasBuildingObject() ? signature.initBuildingObject(nodeElement) : null;
                    nodeEntry.reference.setValue(buildingObject);

                    //arguments which are needed to create this object
                    Object[] args = new Object[signatureSize];

                    return Graph.node(new Iterator<>() {
                        private int i = 0;

                        @Override
                        public boolean hasNext() {
                            return i < signatureSize;
                        }

                        @Override
                        public Map.Entry<Object, ClassEntry> next() {
                            if (i++ == signatureSize) {
                                throw new NoSuchElementException();
                            }

                            ConfigElement nextElement = elementIterator.next();
                            Token<?> nextType = typeResolver.resolveType(typeEntryIterator.next().getValue(),
                                nextElement);
                            SignatureMatcher nextMatcher = signatureMatcherSource.matcherFor(nextType);

                            return Entry.of(null, new ClassEntry(nextType, nextElement, nextMatcher));
                        }
                    }, Graph.output(nodeEntry.reference, new Graph.Accumulator<>() {
                        private int i = 0;

                        @Override
                        public void accept(Object key, Mutable<Object> value, boolean visited) {
                            if (visited && !signature.hasBuildingObject()) {
                                throw new MapperException("Signatures which do not supply building objects may " +
                                    "not be used to construct circular references");
                            }

                            args[i++] = value.getValue();

                            if (i == args.length) {
                                nodeEntry.reference.setValue(signature.buildObject(buildingObject, args));
                            }
                        }
                    }));
                }, this::elementToObjectContainerPredicate,
                scalar -> new MutableObject<>(scalarSource.makeObject(scalar.element, scalar.type)),
                entry -> entry.element, GRAPH_OPTIONS).getValue();
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull ConfigElement elementFromData(T data) throws ConfigProcessException {
        try {
            Token<?> rootType = typeResolver.resolveType(token, null);
            SignatureMatcher rootMatcher = signatureMatcherSource.matcherFor(rootType);
            ElementEntry rootEntry = new ElementEntry(rootType, data, rootMatcher);

            return Graph.process(rootEntry, nodeEntry -> {
                    Object nodeObject = nodeEntry.object;
                    MatchingSignature typeSignature =
                        nodeEntry.signatureMatcher.signatureForObject(nodeEntry.type, nodeObject);
                    Signature<?> signature = typeSignature.signature();
                    int size = typeSignature.size();

                    ConfigContainer target = signature.initContainer(size);
                    nodeEntry.element = target;

                    Iterator<Signature.TypedObject> typedObjectIterator = typeSignature.objects().iterator();

                    return Graph.node(new Iterator<>() {
                        private int i = 0;

                        @Override
                        public boolean hasNext() {
                            return i < size;
                        }

                        @Override
                        public Map.Entry<String, ElementEntry> next() {
                            if (i++ == size) {
                                throw new NoSuchElementException();
                            }

                            Signature.TypedObject typedObject = typedObjectIterator.next();
                            Token<?> objectType = typeResolver.resolveType(typedObject.type(), null);
                            SignatureMatcher thisMatcher = signatureMatcherSource.matcherFor(objectType);

                            return Entry.of(typedObject.name(),
                                new ElementEntry(objectType, typedObject.value(), thisMatcher));
                        }
                    }, Graph.output(nodeEntry.element, (String key, ConfigElement value, boolean circular) -> {
                        if (target.isList()) {
                            target.asList().add(value);
                        } else {
                            target.asNode().put(key, value);
                        }
                    }));
                }, this::objectToElementContainerPredicate, scalar -> scalarSource.makeElement(scalar.object,
                    scalar.type),
                entry -> entry.object, GRAPH_OPTIONS);
        } catch (Exception e) {
            throw new ConfigProcessException(e);
        }
    }

    private boolean elementToObjectContainerPredicate(ClassEntry entry) {
        return typeHinter.getHint(entry.type) != ElementType.SCALAR && entry.element.isContainer();
    }

    private boolean objectToElementContainerPredicate(ElementEntry entry) {
        return typeHinter.getHint(entry.type) != ElementType.SCALAR &&
            (entry.object != null && typeHinter.getHint(Token.ofType(entry.object.getClass())) != ElementType.SCALAR);
    }

    private static class ElementEntry {
        private final Token<?> type;
        private final Object object;
        private final SignatureMatcher signatureMatcher;

        //ElementEntry isn't a record, so we can set this field
        private ConfigElement element;

        private ElementEntry(Token<?> type, Object object, SignatureMatcher signatureMatcher) {
            this.type = type;
            this.object = object;
            this.signatureMatcher = signatureMatcher;
            this.element = null;
        }
    }

    private record ClassEntry(Token<?> type, ConfigElement element, SignatureMatcher signatureMatcher,
                              Mutable<Object> reference) {
        private ClassEntry(Token<?> type, ConfigElement element, SignatureMatcher signatureMatcher) {
            this(type, element, signatureMatcher, new MutableObject<>(null));
        }
    }
}