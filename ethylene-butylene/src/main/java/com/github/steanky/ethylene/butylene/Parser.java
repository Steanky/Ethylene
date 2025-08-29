package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.IntFunction;

import static com.github.steanky.ethylene.butylene.Util.*;
import static com.github.steanky.ethylene.butylene.Tokenizer.Token.*;

/**
 * Utilities for parsing out Butylene configuration data.
 * <p>
 * This class is public to enable cross-package access, but is not considered part of the public API, and may change at
 * any time.
 */
@ApiStatus.Internal
public class Parser {
    private static final int INITIAL_CAPACITY = 10;

    record DeferredResolve(String name, String key, int idx, ContainerContext context, int tokenLine, int tokenColumn, boolean isReference) { }

    private static final class ContainerContext {
        private final ConfigContainer container;

        private boolean foundAny;
        private boolean minimize;
        private int offset;

        private ContainerContext(ConfigContainer container) {
            this.container = container;
            this.minimize = true;
        }

        private void maybeMinimize() {
            if (minimize) container.minimizeStorage();
        }
    }

    public static @NotNull ConfigElement fromReader(@NotNull Reader reader,
        @NotNull IntFunction<? extends ConfigNode> nodeFunction,
        @NotNull IntFunction<? extends ConfigList> listFunction) throws IOException {
        Tokenizer tokenizer = new Tokenizer(new ButyleneReader(reader));

        int listDepth = 0;
        int mapDepth = 0;

        Map<String, ConfigElement> anchorMap = null;
        Deque<DeferredResolve> resolves = null;

        Tokenizer.Token token = tokenizer.next();
        String rootAnchor = null;

        // special case for root node or value with anchor
        if (token == ANCHOR) {
            int line = tokenizer.tokenLine;
            int column = tokenizer.tokenColumn;

            Tokenizer.Token next = tokenizer.next();
            if (next == EOF) throw eofInAnchorName(line, column);
            if (next != UNQUOTED_TEXT) throw invalidAnchorOrOverrideName(next, tokenizer);

            rootAnchor = tokenizer.buffer.toString();

            switch (token = tokenizer.next()) {
                case LIST_START, MAP_START, QUOTED_TEXT, UNQUOTED_TEXT -> { }
                default -> throw invalidToken(token, tokenizer);
            }
        }

        boolean topLevelMap = token != LIST_START;
        boolean eofClosesTopLevelMap = rootAnchor == null && token != LIST_START && token != MAP_START;

        if (token == LIST_START || token == MAP_START) {
            if (token == LIST_START) listDepth ++;
            else mapDepth++;

            token = tokenizer.next();
        }
        else if ((token == UNQUOTED_TEXT || token == QUOTED_TEXT) && tokenizer.peekNext() == EOF) {
            String value = tokenizer.buffer.toString();

            // handle top-level primitives
            if (token == QUOTED_TEXT) return ConfigPrimitive.of(value);
            else return parseUnquotedText(tokenizer, value);
        }

        Deque<ContainerContext> contextStack = new ArrayDeque<>();

        ConfigContainer topLevel = topLevelMap
            ? nodeFunction.apply(INITIAL_CAPACITY)
            : listFunction.apply(INITIAL_CAPACITY);

        contextStack.addLast(new ContainerContext(topLevel));

        if (rootAnchor != null) {
            anchorMap = new HashMap<>();
            anchorMap.put(rootAnchor, topLevel);
        }

        do {
            ContainerContext context = contextStack.peekLast();

            assert context != null;

            ConfigNode contextNode = (context.container instanceof ConfigNode node) ? node : null;
            ConfigList contextList = (context.container instanceof ConfigList list) ? list : null;

            assert (contextNode == null ^ contextList == null);

            // iterate through container entries
            entryLoop:
            while (true) {
                if (context.foundAny && token == VALUE_SEPARATOR) token = tokenizer.next();
                context.foundAny = true;

                String anchorName = null;
                String key = null;

                if (contextNode != null && token != OVERRIDE) {
                    // the key part of a key: value pair, or EOF, or a closing curly bracket
                    switch (token) {
                        case QUOTED_TEXT, UNQUOTED_TEXT -> { }
                        case MAP_END -> {
                            if (--mapDepth < 0) throw invalidToken("missing opening brace", token, tokenizer);
                            contextStack.removeLast().maybeMinimize();
                            break entryLoop;
                        }
                        case EOF -> {
                            if (eofClosesTopLevelMap && context.container == topLevel) {
                                contextStack.removeLast().maybeMinimize();
                                break entryLoop;
                            }
                            else throw unclosedCurlyBraces();
                        }
                        default -> throw invalidToken(token, tokenizer);
                    }

                    key = tokenizer.buffer.toString();

                    Tokenizer.Token next = tokenizer.next();
                    if (next != VALUE_ASSIGN) throw invalidToken(next, tokenizer);

                    token = tokenizer.next();

                    if (token == OVERRIDE)
                        throw invalidToken("invalid position for override", OVERRIDE, tokenizer);
                }

                int anchorLine = -1;
                int anchorColumn = -1;

                // anchors can appear before any value
                if (token == ANCHOR) {
                    anchorLine = tokenizer.tokenLine;
                    anchorColumn = tokenizer.tokenColumn;

                    Tokenizer.Token next = tokenizer.next();
                    if (next == EOF) throw eofInAnchorName(anchorLine, anchorColumn);
                    if (next != UNQUOTED_TEXT) throw invalidAnchorOrOverrideName(next, tokenizer);

                    anchorName = tokenizer.buffer.toString();

                    if (anchorMap != null && anchorMap.containsKey(anchorName))
                        throw new ButyleneParseException("duplicate anchor name", "&" + anchorName, -1,
                            tokenizer.tokenLine, tokenizer.tokenColumn - 1);

                    token = tokenizer.next();
                }

                // value part
                switch (token) {
                    // quoted text here is always a string
                    case QUOTED_TEXT -> {
                        String bufferValue = tokenizer.buffer.toString();
                        ConfigPrimitive stringValue = ConfigPrimitive.of(bufferValue);

                        if (contextNode != null) contextNode.put(key, stringValue);
                        else contextList.add(stringValue);

                        if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, stringValue);
                        }
                    }

                    // could be a number, a boolean, or null
                    case UNQUOTED_TEXT -> {
                        ConfigPrimitive primitive = parseUnquotedText(tokenizer, tokenizer.buffer);

                        if (contextNode != null) contextNode.put(key, primitive);
                        else contextList.add(primitive);

                        if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, primitive);
                        }
                    }

                    case REFERENCE, OVERRIDE -> {
                        int tokenLine = tokenizer.tokenLine;
                        int tokenColumn = tokenizer.tokenColumn;

                        if (tokenizer.next() != UNQUOTED_TEXT) throw invalidToken(token, tokenizer);

                        boolean isReference = token == REFERENCE;

                        if (anchorName != null)
                            throw new ButyleneParseException("anchor before reference or override",
                                "&" + anchorName, -1, anchorLine, anchorColumn);

                        String name = tokenizer.buffer.toString();
                        ConfigElement referenced = anchorMap == null ? null : anchorMap.get(name);
                        if (isReference && referenced != null) {
                            // no need to defer, we already have the anchor
                            if (contextNode != null) contextNode.put(key, referenced);
                            else contextList.add(referenced);
                        } else {
                            if (!isReference && referenced == context.container)
                                throw new ButyleneParseException("override cannot reference its own container",
                                    ">" + name, -1, tokenLine, tokenColumn);

                            // references are allowed to refer to anchors that appear later in the config file
                            // so, we defer resolving until later
                            if (resolves == null) resolves = new ArrayDeque<>();

                            DeferredResolve resolve;
                            if (contextNode != null)
                                resolve = new DeferredResolve(name, key, -1, context, tokenLine, tokenColumn,
                                    isReference);
                            else {
                                resolve = new DeferredResolve(name, null, contextList.size(), context, tokenLine,
                                    tokenColumn, isReference);

                                // temporary value to occupy this index
                                if (isReference) contextList.add(ConfigPrimitive.NULL);
                            }

                            if (isReference) resolves.addFirst(resolve);
                            else {
                                resolves.addLast(resolve);
                                context.minimize = false;
                            }
                        }
                    }

                    case MAP_START, LIST_START -> {
                        ConfigContainer newContainer;
                        if (token == MAP_START) {
                            newContainer = nodeFunction.apply(INITIAL_CAPACITY);
                            mapDepth++;
                        } else {
                            newContainer = listFunction.apply(INITIAL_CAPACITY);
                            listDepth++;
                        }

                        contextStack.addLast(new ContainerContext(newContainer));

                        if (contextNode != null) contextNode.put(key, newContainer);
                        else contextList.add(newContainer);

                        if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, newContainer);
                        }

                        break entryLoop;
                    }

                    case LIST_END -> {
                        if (contextNode != null) throw wrongBraceType(token, tokenizer);
                        if (--listDepth < 0) throw invalidToken("missing opening brace", token, tokenizer);

                        contextStack.removeLast().maybeMinimize();
                        break entryLoop;
                    }

                    case MAP_END -> throw wrongBraceType(token, tokenizer);
                    default -> throw invalidToken(token, tokenizer);
                }

                token = tokenizer.next();
            }

            token = tokenizer.next();
        } while (!contextStack.isEmpty());

        if (token != EOF) throw invalidToken("expected EOF", token, tokenizer);
        if ((resolves != null) && anchorMap == null) throw missingAnchor(resolves.getFirst());
        if (resolves == null) return topLevel;

        for (DeferredResolve deferred : resolves) {
            ConfigElement referenced = anchorMap.get(deferred.name);
            if (referenced == null) throw missingAnchor(deferred);

            ContainerContext context = deferred.context;
            ConfigContainer container = context.container;

            if (deferred.isReference) {
                if (container.isNode()) container.asNode().put(deferred.key, referenced);
                else container.asList().set(deferred.idx, referenced);
                continue;
            }

            if ((referenced.isNode() && !container.isNode()) || (referenced.isList() && !container.isList()))
                throw new ButyleneParseException("type referenced by override must match its container's type",
                    ">" + deferred.name, -1, deferred.tokenLine, deferred.tokenColumn);

            // because of how items are added to the deferred list, overrides come after all references
            if (container.isNode()) {
                for (Map.Entry<String, ConfigElement> entry : referenced.asNode().entrySet()) {
                    container.asNode().putIfAbsent(entry.getKey(), entry.getValue());
                }

                container.asNode().minimizeStorage();
            }
            else {
                ConfigList referencedList = referenced.asList();
                container.asList().addAll(deferred.idx + context.offset, referencedList);
                container.asList().minimizeStorage();

                // further indices in the same scope must be offset
                context.offset += referencedList.size();
            }
        }

        return topLevel;
    }
}