package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.signature.MatchingSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.SignatureParameter;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.toolkit.collection.Iterators;
import com.github.steanky.toolkit.collection.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private final Token<T> token;
    private final SignatureMatcher.Source signatureMatcherSource;
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;
    private final ScalarSource scalarSource;
    private final boolean writeDefaults;

    /**
     * Creates a new instance of this class.
     *
     * @param token                  the type of object this processor will be able to process
     * @param signatureMatcherSource the {@link SignatureMatcher.Source} used to create signature matchers
     * @param typeHinter             the {@link TypeHinter} used to determine information about types
     * @param typeResolver           the {@link TypeResolver} used to resolve types into concrete implementations
     * @param scalarSource           the {@link ScalarSource} used to produce scalar types from {@link ConfigElement}s,
     *                               and vice versa
     * @param writeDefaults          whether this mapper should write configuration data when it is equal to a supplied
     *                               default value
     */
    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull SignatureMatcher.Source signatureMatcherSource,
        @NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver, @NotNull ScalarSource scalarSource,
        boolean writeDefaults) {
        this.token = Objects.requireNonNull(token);
        this.signatureMatcherSource = Objects.requireNonNull(signatureMatcherSource);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.scalarSource = Objects.requireNonNull(scalarSource);
        this.writeDefaults = writeDefaults;
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

                    //if this signature supports circular refs, buildingObject should be non-null
                    Object buildingObject =
                        signature.hasBuildingObject() ? signature.initBuildingObject(nodeElement) : null;

                    //immediately create parameterless objects as they wouldn't be constructed otherwise
                    if (signatureSize == 0) {
                        nodeEntry.reference.set(signature.buildObject(buildingObject, EMPTY_OBJECT_ARRAY));
                        return Graph.node(Iterators.iterator(),
                            Graph.output(nodeEntry.reference, Graph.emptyAccumulator()));
                    }

                    nodeEntry.reference.set(buildingObject);

                    Iterator<ConfigElement> elementIterator = matchingSignature.elements().iterator();
                    Iterator<Map.Entry<String, SignatureParameter>> typeEntryIterator = signature.argumentTypes().iterator();

                    //used to resolve correct generic parameters based on the actual type arguments
                    Map<String, Token<?>> typeVariableOverrides = signature.genericMappings();
                    Map<TypeVariable<?>, Type> map;

                    if (!typeVariableOverrides.isEmpty()) {
                        map = nodeEntry.type.supertypeVariables(signature.returnType()).resolve();
                    } else {
                        map = null;
                    }

                    //arguments which are needed to create this object
                    Object[] args = new Object[signatureSize];

                    return Graph.node(new Iterator<>() {
                        private int i;
                        private final Graph.InputEntry<String, ClassEntry, Wrapper<Object>> inputEntry = Graph.nullEntry();

                        @Override
                        public boolean hasNext() {
                            return i < signatureSize;
                        }

                        @Override
                        public Graph.InputEntry<String, ClassEntry, Wrapper<Object>> next() {
                            if (i++ == signatureSize) {
                                throw new NoSuchElementException();
                            }

                            ConfigElement nextElement = elementIterator.next();
                            Map.Entry<String, SignatureParameter> entry = typeEntryIterator.next();

                            Token<?> actualType = null;

                            if (map != null && entry.getKey() != null) {
                                Token<?> typeVariableToken = typeVariableOverrides.get(entry.getKey());
                                if (typeVariableToken != null) {
                                    Type actual = map.get((TypeVariable<?>) typeVariableToken.get());
                                    if (actual != null) {
                                        actualType = Token.ofType(actual);
                                    }
                                }
                            }

                            if (actualType == null) {
                                actualType = entry.getValue().type();
                            }

                            Token<?> nextType = typeResolver.resolveType(actualType, nextElement);
                            SignatureMatcher nextMatcher = signatureMatcherSource.matcherFor(nextType);

                            inputEntry.setValue(new ClassEntry(nextType, nextElement, nextMatcher));
                            return inputEntry;
                        }
                    }, Graph.output(nodeEntry.reference, new Graph.Accumulator<>() {
                        private int i;

                        @Override
                        public void accept(Object key, Wrapper<Object> value, boolean circular) {
                            if (circular && !signature.hasBuildingObject()) {
                                throw new MapperException("Signatures which do not supply building objects may " +
                                    "not be used to construct circular references");
                            }

                            args[i++] = value.get();

                            if (i == args.length) {
                                nodeEntry.reference.set(signature.buildObject(buildingObject, args));
                            }
                        }
                    }));
                }, this::elementToObjectContainerPredicate,
                scalar -> Wrapper.of(scalarSource.makeObject(scalar.element, scalar.type)),
                entry -> entry.element, GRAPH_OPTIONS).get();
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

                    Iterator<Signature.TypedObject> outputIterator;
                    if (!writeDefaults) {
                        outputIterator = typeSignature.objects().iterator();
                    } else {
                        outputIterator = null;
                    }

                    Map<String, Token<?>> typeVariableOverrides = signature.genericMappings();
                    Map<TypeVariable<?>, Type> map;
                    if (!typeVariableOverrides.isEmpty()) {
                        map = nodeEntry.type.supertypeVariables(signature.returnType()).resolve();
                    } else {
                        map = null;
                    }

                    return Graph.node(new Iterator<>() {
                        private int i;
                        private final Graph.InputEntry<String, ElementEntry, ConfigElement> inputEntry = Graph.nullEntry();

                        @Override
                        public boolean hasNext() {
                            return i < size;
                        }

                        @Override
                        public Graph.InputEntry<String, ElementEntry, ConfigElement> next() {
                            if (i++ == size) {
                                throw new NoSuchElementException();
                            }

                            Signature.TypedObject typedObject = typedObjectIterator.next();

                            Token<?> type = null;
                            if (map != null && typedObject.name() != null) {
                                Token<?> token = typeVariableOverrides.get(typedObject.name());
                                if (token != null) {
                                    Type actualType = token.get();
                                    Type nullableType = map.get((TypeVariable<?>) actualType);
                                    if (nullableType != null) {
                                        type = Token.ofType(nullableType);
                                    }
                                }
                            }

                            if (type == null) {
                                type = typedObject.type();
                            }

                            Token<?> objectType = typeResolver.resolveType(type, null);
                            SignatureMatcher thisMatcher = signatureMatcherSource.matcherFor(objectType);

                            inputEntry.setKey(typedObject.name());
                            inputEntry.setValue(new ElementEntry(objectType, typedObject.value(), thisMatcher));

                            return inputEntry;
                        }
                    }, Graph.output(nodeEntry.element,
                        (Graph.Accumulator<String, ConfigElement>) (key, element, visited) -> {
                            if (!writeDefaults) {
                                Signature.TypedObject object = outputIterator.next();
                                ConfigElement defaultValue = object.defaultValue();

                                if (element.equals(defaultValue)) {
                                    return;
                                }
                            }

                            if (target.isList()) {
                                target.asList().add(element);
                            } else {
                                target.asNode().put(key, element);
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
        //if the signature matcher for this entry is null: we found a scalar
        return entry.element.isContainer() && entry.signatureMatcher != null;
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
                              Wrapper<Object> reference) {
        private ClassEntry(Token<?> type, ConfigElement element, SignatureMatcher signatureMatcher) {
            this(type, element, signatureMatcher, Wrapper.ofNull());
        }
    }
}