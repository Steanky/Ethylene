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
import static com.github.steanky.ethylene.butylene.ButyleneParseException.builder;

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
        Tokenizer t = new Tokenizer(new ButyleneReader(reader));

        int listDepth = 0;
        int mapDepth = 0;

        Map<String, ConfigElement> anchorMap = null;
        Deque<DeferredResolve> resolves = null;

        Tokenizer.Token token = t.next();
        String rootAnchor = null;

        // special case for root node or value with anchor
        if (token == ANCHOR) {
            int line = t.tokenLine;
            int column = t.tokenColumn;

            Tokenizer.Token next = t.next();
            if (next == EOF) builder()
                .reason(E_EOF_IN_ANCHOR_NAME)
                .line(line)
                .column(column)
                .raise();

            if (next != UNQUOTED_TEXT) builder()
                .reason(E_INVALID_REFERENCE_NAME)
                .from(t, token)
                .raise();

            rootAnchor = t.buffer.toString();

            switch (token = t.next()) {
                case LIST_START, MAP_START, QUOTED_TEXT, UNQUOTED_TEXT -> { }
                default -> builder()
                    .reason(E_INVALID_TOKEN_POSITION)
                    .from(t, token)
                    .raise();
            }
        }

        boolean topLevelMap = token != LIST_START;
        boolean eofClosesTopLevelMap = rootAnchor == null && token != LIST_START && token != MAP_START;
        boolean maybeTopLevelScalar = false;

        if (token == LIST_START || token == MAP_START) {
            if (token == LIST_START) listDepth ++;
            else mapDepth++;

            token = t.next();
        }
        else if (token == UNQUOTED_TEXT || token == QUOTED_TEXT) maybeTopLevelScalar = true;

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
                if (context.foundAny && token == VALUE_SEPARATOR) token = t.next();
                context.foundAny = true;

                String anchorName = null;
                String key = null;

                if (contextNode != null && token != OVERRIDE) {
                    int keyColumn;
                    // the key part of a key: value pair, or EOF, or a closing curly bracket
                    switch (token) {
                        case QUOTED_TEXT -> keyColumn = t.tokenColumn - 1;
                        case UNQUOTED_TEXT -> keyColumn = t.tokenColumn;
                        case MAP_END -> {
                            if (--mapDepth < 0) builder()
                                .reason(E_MISSING_OPENING_BRACE)
                                .from(t, token)
                                .raise();

                            contextStack.removeLast().maybeMinimize();
                            break entryLoop;
                        }
                        case EOF -> {
                            if (eofClosesTopLevelMap && context.container == topLevel) {
                                contextStack.removeLast().maybeMinimize();
                                break entryLoop;
                            }
                            else throw builder()
                                .reason(E_MISSING_CLOSING_BRACE)
                                .from(t, token)
                                .build();
                        }
                        default -> throw builder()
                            .reason(E_INVALID_TOKEN_POSITION)
                            .from(t, token)
                            .build();
                    }

                    key = t.buffer.toString();

                    Tokenizer.Token next = t.next();
                    if (maybeTopLevelScalar && next == EOF) {
                        // handle top-level primitives
                        if (token == QUOTED_TEXT) return ConfigPrimitive.of(key);
                        else return parseUnquotedText(t, key);
                    }
                    else if (next != VALUE_ASSIGN) builder()
                        .reason(E_INVALID_TOKEN_POSITION)
                        .token(token, key)
                        .line(t.tokenLine)
                        .column(keyColumn)
                        .raise();

                    token = t.next();

                    if (token == OVERRIDE) builder()
                        .reason(E_INVALID_TOKEN_POSITION)
                        .from(t, token)
                        .raise();

                    maybeTopLevelScalar = false;
                }

                int anchorLine = -1;
                int anchorColumn = -1;

                // anchors can appear before any value
                if (token == ANCHOR) {
                    anchorLine = t.tokenLine;
                    anchorColumn = t.tokenColumn;

                    Tokenizer.Token next = t.next();
                    if (next == EOF) builder()
                        .reason(E_EOF_IN_ANCHOR_NAME)
                        .line(anchorLine)
                        .column(anchorColumn)
                        .raise();

                    if (next != UNQUOTED_TEXT) builder()
                        .reason(E_INVALID_REFERENCE_NAME)
                        .from(t, next)
                        .raise();

                    anchorName = t.buffer.toString();

                    if (anchorMap != null && anchorMap.containsKey(anchorName)) builder()
                        .reason(E_DUPLICATE_ANCHOR_NAME)
                        .token("&" + anchorName)
                        .line(t.tokenLine)
                        .column(t.tokenColumn - 1)
                        .raise();

                    token = t.next();
                }

                // value part
                switch (token) {
                    // quoted text here is always a string
                    case QUOTED_TEXT -> {
                        String bufferValue = t.buffer.toString();
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
                        ConfigPrimitive primitive = parseUnquotedText(t, t.buffer);

                        if (contextNode != null) contextNode.put(key, primitive);
                        else contextList.add(primitive);

                        if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, primitive);
                        }
                    }

                    case REFERENCE, OVERRIDE -> {
                        int tokenLine = t.tokenLine;
                        int tokenColumn = t.tokenColumn;

                        if (t.next() != UNQUOTED_TEXT) builder()
                            .reason(E_INVALID_TOKEN_POSITION)
                            .token(token, t.buffer)
                            .locationFrom(t)
                            .raise();

                        boolean isReference = token == REFERENCE;

                        if (anchorName != null) builder()
                            .reason(E_ANCHOR_BEFORE_REFERENCE)
                            .token("&" + anchorName)
                            .line(anchorLine)
                            .column(anchorColumn)
                            .raise();

                        String name = t.buffer.toString();
                        ConfigElement referenced = anchorMap == null ? null : anchorMap.get(name);
                        if (isReference && referenced != null) {
                            // no need to defer, we already have the anchor
                            if (contextNode != null) contextNode.put(key, referenced);
                            else contextList.add(referenced);
                        } else {
                            if (!isReference && referenced == context.container) builder()
                                .reason(E_SELF_REFERENTIAL_OVERRIDE)
                                .token(">" + name)
                                .line(tokenLine)
                                .column(tokenColumn)
                                .raise();

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
                        if (contextNode != null) builder()
                            .reason(E_WRONG_CLOSING_BRACE)
                            .from(t, token)
                            .raise();

                        if (--listDepth < 0) builder()
                            .reason(E_MISSING_OPENING_BRACE)
                            .from(t, token)
                            .raise();

                        contextStack.removeLast().maybeMinimize();
                        break entryLoop;
                    }

                    case MAP_END -> throw builder()
                        .reason(E_WRONG_CLOSING_BRACE)
                        .from(t, token)
                        .build();

                    default -> throw builder()
                        .reason(E_INVALID_TOKEN_POSITION)
                        .from(t, token)
                        .build();
                }

                token = t.next();
            }

            token = t.next();
        } while (!contextStack.isEmpty());

        if (token != EOF) builder()
            .reason(E_EOF_EXPECTED)
            .from(t, token)
            .column(t.tokenColumn - 1)
            .raise();

        if (resolves == null) return topLevel;

        if (anchorMap == null) builder()
            .reason(E_MISSING_ANCHOR)
            .from(resolves.getFirst())
            .raise();

        for (DeferredResolve deferred : resolves) {
            ConfigElement referenced = anchorMap.get(deferred.name);
            if (referenced == null) builder()
                .reason(E_MISSING_ANCHOR)
                .from(deferred)
                .raise();

            ContainerContext context = deferred.context;
            ConfigContainer container = context.container;

            if (deferred.isReference) {
                if (container.isNode()) container.asNode().put(deferred.key, referenced);
                else container.asList().set(deferred.idx, referenced);
                continue;
            }

            if ((referenced.isNode() && !container.isNode()) || (referenced.isList() && !container.isList())) builder()
                .reason(E_INVALID_REFERENCED_TYPE)
                .from(deferred)
                .raise();

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