package com.github.steanky.ethylene.core.propylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * Parser for the Propylene configuration language. Propylene is an extremely simple format, which is meant to be used
 * to elegantly and unambiguously describe {@link ConfigElement} instances.
 * <p>
 * This class is public to enable cross-package access, but is not to be considered part of the public API. It is
 * liable to change at any time.
 */
@ApiStatus.Internal
public final class Parser {
    private static final char STRING_DELIMITER = '\'';

    private static final char LONG_POSTFIX = 'L';
    private static final char FLOAT_POSTFIX = 'F';
    private static final char SHORT_POSTFIX = 'S';
    private static final char BYTE_POSTFIX = 'B';
    private static final char ALTERNATE_BYTE_POSTFIX = 'Z';

    private static final char SPECIAL_RADIX_PREFIX = '0';
    private static final char HEXADECIMAL_INDICATOR = 'X';
    private static final char BINARY_INDICATOR = 'B';
    private static final char FLOATING_POINT_INDICATOR = '.';
    private static final char MINUS_SIGN = '-';

    private static final char LIST_START = '[';
    private static final char LIST_END = ']';
    private static final char NODE_START = '{';
    private static final char NODE_END = '}';
    private static final char VALUE_ASSIGN = '=';

    private static final char REFERENCE_PREFIX = '&';
    private static final char ESCAPE = '\\';
    private static final char LITERAL_DELIMITER = ',';
    private static final char NUMBER_SEPARATOR = '_';

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || c == MINUS_SIGN;
    }

    private static char toUpperAscii(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 32);
        }

        return c;
    }

    private static TerminalType determineTerminalType(int c) {
        return switch (c) {
            case LIST_END -> TerminalType.LIST;
            case NODE_END -> TerminalType.NODE;
            default -> TerminalType.NONE;
        };
    }

    private static boolean shouldSimpleLiteralTerminate(char c) {
        return switch (c) {
            case LITERAL_DELIMITER, NODE_END, LIST_END, NODE_START, LIST_START, VALUE_ASSIGN -> true;
            default -> false;
        };
    }

    private static boolean containerMatchesTerminalType(ContainerType containerType, TerminalType terminalType) {
        if (containerType == null) {
            return terminalType == TerminalType.NONE;
        }

        return (containerType == ContainerType.NODE && terminalType == TerminalType.NODE) ||
            (containerType == ContainerType.LIST && terminalType == TerminalType.LIST);
    }

    private enum TokenType {
        NODE,
        LIST,
        STRING,
        KEY,
        REFERENCE_TAG,
        REFERENCE,
        NUMBER_LONG,
        NUMBER_DOUBLE,
        NUMBER_INT,
        NUMBER_FLOAT,
        NUMBER_SHORT,
        NUMBER_BYTE,
        BOOLEAN_TRUE,
        BOOLEAN_FALSE,
        NULL,
        ERROR,
        EOF
    }

    private enum TokenizerState {
        SELECT,
        PARSE_NUMBER, PARSE_SIMPLE_LITERAL,
        PARSE_STRING,
        END
    }

    private enum ContainerType {
        ROOT,
        NODE,
        LIST
    }

    private enum TerminalType {
        NONE,
        LIST,
        NODE,
    }

    private sealed interface Token permits Container, Scalar {
        @NotNull TokenType type();

        ConfigElement element();
    }

    private interface NumericParseFunction {
        @NotNull ConfigElement parse(String string, int radix);
    }

    private static final class Container implements Token {
        private final ContainerType type;
        private final Container parent;

        private ConfigContainer container;
        private List<Token> children;

        private boolean defer;

        private Container(ContainerType type, Container parent) {
            this.type = type;
            this.parent = parent;
        }

        @Override
        public @NotNull TokenType type() {
            return type == ContainerType.LIST ? TokenType.LIST : TokenType.NODE;
        }

        @Override
        public ConfigElement element() {
            return container;
        }

        private List<Token> children() {
            return Objects.requireNonNullElseGet(children, () -> children = new ArrayList<>());
        }

        private boolean lastTokenIsKey() {
            if (children == null || children.isEmpty()) {
                return false;
            }

            return children.get(children.size() - 1).type() == TokenType.KEY;
        }

        private void finish() {
            if (container != null) {
                return;
            }

            if (children == null) {
                container = type == ContainerType.LIST ? new ArrayConfigList(0) : new LinkedConfigNode(0);
                return;
            }

            switch (type) {
                case LIST -> {
                    ConfigList list = new ArrayConfigList(children.size());
                    this.container = list;

                    for (Token token : children) {
                        list.add(token.element());
                    }
                }
                case NODE -> {
                    ConfigNode node = new LinkedConfigNode(children.size());
                    this.container = node;

                    for (int i = 0; i < children.size(); i += 2) {
                        Token key = children.get(i);
                        Token value = children.get(i + 1);

                        if (key.type() != TokenType.KEY) {
                            throw new IllegalStateException("Expected TokenType.KEY, got " + key.type());
                        }

                        if (value.type() == TokenType.KEY) {
                            throw new IllegalStateException("Got TokenType.KEY when any other type was expected");
                        }

                        node.put(((Scalar)key).raw, value.element());
                    }
                }
                default -> throw new IllegalStateException("Unexpected TokenType " + type + " for container");
            }
        }
    }

    private static final class Scalar implements Token {
        private final TokenType type;
        private final TerminalType terminal;
        private final String raw;

        private ConfigElement element;
        private int reference;

        private Scalar(TokenType type, TerminalType terminal, String raw) {
            this.type = type;
            this.terminal = terminal;
            this.raw = raw;
        }

        @Override
        public @NotNull TokenType type() {
            return type;
        }

        @Override
        public ConfigElement element() {
            return element;
        }
    }

    private static final Scalar MULTIPLE_TOP_LEVEL_ELEMENTS =  new Scalar(TokenType.ERROR, TerminalType.NONE, "Multiple top-level elements");
    private static final Scalar KEY_VALUE_PAIR_OUTSIDE_NODE = new Scalar(TokenType.ERROR, TerminalType.NONE, "Key-value pair outside of node");
    private static final Scalar ILLEGAL_CHARACTER = new Scalar(TokenType.ERROR, TerminalType.NONE, "Illegal character");
    private static final Scalar MISMATCHING_CLOSING_BRACKETS = new Scalar(TokenType.ERROR, TerminalType.NONE, "Mismatching closing brackets");
    private static final Scalar KEY_WITHOUT_MATCHING_VALUE = new Scalar(TokenType.ERROR, TerminalType.NONE, "Key without matching value");
    private static final Scalar UNCLOSED_BRACKETS = new Scalar(TokenType.ERROR, TerminalType.NONE, "Unclosed brackets");
    private static final Scalar UNCLOSED_STRING_LITERAL = new Scalar(TokenType.ERROR, TerminalType.NONE, "Unclosed string literal");
    private static final Scalar INVALID_LITERAL = new Scalar(TokenType.ERROR, TerminalType.NONE, "Invalid literal");
    private static final Scalar REFERENCE_TAG_AS_KEY = new Scalar(TokenType.ERROR, TerminalType.NONE, "Reference tag as key");
    private static final Scalar INVALID_REFERENCE_TAG = new Scalar(TokenType.ERROR, TerminalType.NONE, "Invalid reference tag");
    private static final Scalar DUPLICATE_REFERENCE_TAG = new Scalar(TokenType.ERROR, TerminalType.NONE, "Duplicate reference tag");
    private static final Scalar REFERENCE_AS_ROOT = new Scalar(TokenType.ERROR, TerminalType.NONE, "Reference as root");

    private static final Scalar EOF = new Scalar(TokenType.EOF, TerminalType.NONE, null);

    private static final class Tokenizer implements AutoCloseable {
        private final Reader reader;
        private final StringBuilder sharedBuffer;

        private TokenizerState state;
        private ContainerType containerType;

        private Token root;
        private Container currentContainer;

        private Scalar lastTerminalScalar;

        private boolean escape;
        private boolean isFloatingPoint;
        private boolean isNegative;

        private Map<Integer, Container> referenceMap;

        private ContainerType referenceTargetType;
        private int lastReferenceTag;

        private List<Container> deferred;

        private Tokenizer(Reader reader) {
            this.reader = reader;
            this.sharedBuffer = new StringBuilder();
            this.state = TokenizerState.SELECT;
            this.containerType = ContainerType.ROOT;
            this.referenceTargetType = ContainerType.ROOT;
            this.lastReferenceTag = -1;
        }

        private String consumeBuffer() {
            String buffer = sharedBuffer.toString();
            sharedBuffer.setLength(0);
            return buffer;
        }

        private void append(char c) {
            sharedBuffer.append(c);
        }

        private Map<Integer, Container> referenceMap() {
            return Objects.requireNonNullElseGet(referenceMap, () -> referenceMap = new HashMap<>(4));
        }

        private void acceptReferenceTag(int identifier) {
            lastReferenceTag = identifier;
        }

        private Scalar validateContainer(TerminalType type, Scalar terminalScalar) {
            if (!containerMatchesTerminalType(containerType, type)) {
                return MISMATCHING_CLOSING_BRACKETS;
            }

            if (currentContainer == null) {
                return ILLEGAL_CHARACTER;
            }

            if ((terminalScalar == null || terminalScalar.terminal != TerminalType.NONE) &&
                containerType == ContainerType.NODE) {
                if (currentContainer.lastTokenIsKey()) {
                    return KEY_WITHOUT_MATCHING_VALUE;
                }
            }

            return terminalScalar;
        }

        private void determineContainerType() {
            containerType = currentContainer == null ? ContainerType.ROOT : currentContainer.type;
        }

        private Scalar acceptNewToken(Token token) {
            if (token.type() == TokenType.REFERENCE_TAG) {
                return null;
            }

            if (root == null) {
                if (token.type() == TokenType.REFERENCE) {
                    return REFERENCE_AS_ROOT;
                }

                root = token;
            }
            else if (currentContainer != null) {
                if (containerType == ContainerType.NODE) {
                    boolean lastTokenIsKey = currentContainer.lastTokenIsKey();

                    if ((token.type() == TokenType.KEY && lastTokenIsKey) ||
                        (token.type() != TokenType.KEY && !lastTokenIsKey)) {
                        return KEY_WITHOUT_MATCHING_VALUE;
                    }
                }
                else if (token.type() == TokenType.KEY) {
                    return KEY_VALUE_PAIR_OUTSIDE_NODE;
                }

                if (token.type() == TokenType.REFERENCE) {
                    int reference = ((Scalar)token).reference;
                    Container referred;
                    if (referenceMap == null || (referred = referenceMap.get(reference)) == null) {
                        Container container = currentContainer;
                        do {
                            container.defer = true;
                            container = container.parent;
                        }
                        while (container != null);
                    }
                    else {
                        token = referred;
                    }
                }

                currentContainer.children().add(token);
            }
            else { // root non-null, current null means multiple top-level elements
                return MULTIPLE_TOP_LEVEL_ELEMENTS;
            }

            return null;
        }

        private Scalar initializeNewContainer(ContainerType type, int referenceTag) {
            Container newContainer = new Container(type, currentContainer);
            if (referenceTag > -1 && referenceMap().putIfAbsent(referenceTag, newContainer) != null) {
                return DUPLICATE_REFERENCE_TAG;
            }

            Scalar token = acceptNewToken(newContainer);

            currentContainer = newContainer;
            containerType = type;
            return token;
        }

        private List<Container> deferred() {
            return Objects.requireNonNullElseGet(deferred, () -> deferred = new ArrayList<>());
        }

        private void deferOrFinish(Container container) {
            if (container.defer) {
                deferred().add(container);
            }
            else {
                container.finish();
            }
        }

        private Scalar next() throws IOException {
            if (lastTerminalScalar != null) {
                deferOrFinish(currentContainer);
                currentContainer = currentContainer.parent;

                lastTerminalScalar = null;
                determineContainerType();
            }

            Scalar scalar;
            do {
                scalar = switch (state) {
                    case SELECT -> select();
                    case PARSE_NUMBER -> number();
                    case PARSE_SIMPLE_LITERAL -> simpleLiteral();
                    case PARSE_STRING -> string();
                    case END -> throw new IllegalStateException("Unexpected TokenizerState");
                };
            } while (scalar == null);

            if (scalar.type == TokenType.ERROR || scalar.type == TokenType.EOF) {
                state = TokenizerState.END;
                if (scalar.type == TokenType.EOF) {
                    if (containerType != ContainerType.ROOT) {
                        return UNCLOSED_BRACKETS;
                    }
                }

                return scalar;
            }

            state = TokenizerState.SELECT;

            if (scalar.terminal != TerminalType.NONE) {
                lastTerminalScalar = scalar;
            }

            Scalar error = acceptNewToken(scalar);
            if (error != null) {
                return error;
            }

            return switch (scalar.terminal) {
                case LIST -> validateContainer(TerminalType.LIST, scalar);
                case NODE -> validateContainer(TerminalType.NODE, scalar);
                case NONE -> scalar;
            };
        }

        private ConfigElement finish() throws IOException {
            if (deferred != null) {
                for (Container container : deferred) {
                    for (int i = 0; i < container.children.size(); i++) {
                        Token token = container.children.get(i);
                        if (token.type() == TokenType.REFERENCE) {
                            int identifier = ((Scalar)token).reference;

                            Container referred;
                            if (referenceMap == null || (referred = referenceMap.get(identifier)) == null) {
                                throw new IOException("Missing reference " + identifier);
                            }

                            container.children.set(i, referred);
                        }
                    }
                }

                for (Container container : deferred) {
                    container.finish();
                }
            }

            return root.element();
        }

        private Scalar select() throws IOException {
            if (lastReferenceTag > -1) {
                ContainerType type = referenceTargetType;
                int referenceTag = lastReferenceTag;

                referenceTargetType = ContainerType.ROOT;
                lastReferenceTag = -1;
                return initializeNewContainer(type, referenceTag);
            }

            int read = reader.read();
            if (read == -1) {
                return EOF;
            }

            char character = (char) read;
            if (Character.isWhitespace(character)) {
                return null;
            }

            if (isDigit(character)) {
                state = TokenizerState.PARSE_NUMBER;
                append(character);
                if (character == MINUS_SIGN) {
                    isNegative = true;
                }

                return number();
            }

            return switch (character) {
                case LITERAL_DELIMITER -> null;
                case STRING_DELIMITER -> {
                    state = TokenizerState.PARSE_STRING;
                    yield string();
                }
                case NODE_START, LIST_START -> initializeNewContainer(character == NODE_START ? ContainerType.NODE : ContainerType.LIST, -1);
                case NODE_END, LIST_END -> {
                    Scalar error = validateContainer(character == NODE_END ? TerminalType.NODE : TerminalType.LIST, null);
                    if (error != null) {
                        yield error;
                    }

                    //validateContainer will ensure current != null
                    deferOrFinish(currentContainer);
                    currentContainer = currentContainer.parent;

                    determineContainerType();
                    yield null;
                }
                default -> {
                    state = TokenizerState.PARSE_SIMPLE_LITERAL;
                    append(character);
                    yield simpleLiteral();
                }
            };
        }

        private Scalar number() throws IOException {
            int read = reader.read();

            char character;
            if (read == -1 || shouldSimpleLiteralTerminate((character = (char)read))) {
                String buffer = consumeBuffer().trim();

                int offset = isNegative ? 1 : 0;

                TokenType type;
                if (buffer.length() >= 2 + offset && buffer.charAt(offset) == '0' &&
                    toUpperAscii(buffer.charAt(1 + offset)) == HEXADECIMAL_INDICATOR) {
                    type = switch (toUpperAscii(buffer.charAt(buffer.length() - 1))) {
                        case LONG_POSTFIX -> TokenType.NUMBER_LONG;
                        case SHORT_POSTFIX -> TokenType.NUMBER_SHORT;
                        case ALTERNATE_BYTE_POSTFIX -> TokenType.NUMBER_BYTE;
                        default -> TokenType.NUMBER_INT;
                    };
                }
                else {
                    type = switch (toUpperAscii(buffer.charAt(buffer.length() - 1))) {
                        case LONG_POSTFIX -> TokenType.NUMBER_LONG;
                        case FLOAT_POSTFIX -> TokenType.NUMBER_FLOAT;
                        case SHORT_POSTFIX -> TokenType.NUMBER_SHORT;
                        case BYTE_POSTFIX -> TokenType.NUMBER_BYTE;
                        default -> {
                            if (isFloatingPoint) {
                                yield TokenType.NUMBER_DOUBLE;
                            }

                            yield TokenType.NUMBER_INT;
                        }
                    };
                }

                isFloatingPoint = false;
                isNegative = false;
                return new Scalar(type, determineTerminalType(read), buffer);
            }

            if (character != NUMBER_SEPARATOR) {
                append(character);
            }

            if (character == FLOATING_POINT_INDICATOR) {
                isFloatingPoint = true;
            }

            return null;
        }

        private Scalar simpleLiteral() throws IOException {
            int read = reader.read();

            char character;
            if (read == -1 || shouldSimpleLiteralTerminate(character = (char)read)) {
                String buffer = consumeBuffer().trim();
                if (buffer.equalsIgnoreCase("true")) {
                    return new Scalar(TokenType.BOOLEAN_TRUE, determineTerminalType(read), buffer);
                }
                else if (buffer.equalsIgnoreCase("false")) {
                    return new Scalar(TokenType.BOOLEAN_FALSE, determineTerminalType(read), buffer);
                }
                else if (buffer.equalsIgnoreCase("null")) {
                    return new Scalar(TokenType.NULL, determineTerminalType(read), buffer);
                }

                char first = buffer.charAt(0);
                if (read == VALUE_ASSIGN) {
                    if (first == REFERENCE_PREFIX) {
                        return REFERENCE_TAG_AS_KEY;
                    }

                    return new Scalar(TokenType.KEY, determineTerminalType(read), buffer);
                }

                if (first == REFERENCE_PREFIX) {
                    if (read == LIST_START || read == NODE_START) {
                        referenceTargetType = read == LIST_START ? ContainerType.LIST : ContainerType.NODE;
                        return new Scalar(TokenType.REFERENCE_TAG, determineTerminalType(read), buffer);
                    }

                    return new Scalar(TokenType.REFERENCE, determineTerminalType(read), buffer);
                }

                return INVALID_LITERAL;
            }

            append(character);
            return null;
        }

        private Scalar string() throws IOException {
            int read = reader.read();

            if (read == -1) {
                return UNCLOSED_STRING_LITERAL;
            }

            char character = (char) read;
            return switch (character) {
                case STRING_DELIMITER -> {
                    if (escape) {
                        append(STRING_DELIMITER);
                        escape = false;
                        yield null;
                    }

                    yield new Scalar(TokenType.STRING, TerminalType.NONE, consumeBuffer());
                }
                case ESCAPE -> {
                    if (escape) {
                        append(ESCAPE);
                        escape = false;
                    }
                    else {
                        escape = true;
                    }

                    yield null;
                }
                default -> {
                    if (escape) {
                        append(ESCAPE);
                        escape = false;
                    }

                    append(character);
                    yield null;
                }
            };
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    /**
     * Convenience overload of {@link Parser#fromReader(Reader)} that reads from the provided string.
     * @param string the string from which to read from
     * @return a ConfigElement created from parsing the string, which must contain valid Propylene configuration data
     * @throws IOException if there is a syntax error
     */
    public static @NotNull ConfigElement fromString(@NotNull String string) throws IOException {
        return fromReader(new StringReader(string));
    }

    /**
     * Extracts a single {@link ConfigElement} from the reader and closes it.
     * @param reader the reader from which to read Propylene configuration data
     * @return a ConfigElement
     * @throws IOException if an IOException was thrown by the underlying reader while reading, or there is a syntax error
     */
    public static @NotNull ConfigElement fromReader(@NotNull Reader reader) throws IOException {
        try (Tokenizer tokenizer = new Tokenizer(reader)) {
            Scalar token;
            do {
                token = parseScalar(tokenizer.next(), tokenizer);
            }
            while (token.type != TokenType.ERROR && token.type != TokenType.EOF);

            if (token.type == TokenType.ERROR) {
                throw new IOException(token.raw);
            }

            if (tokenizer.root == null) {
                throw new IOException("Input contained no valid tokens");
            }

            return tokenizer.finish();
        }
    }

    private static Scalar parseScalar(Scalar scalar, Tokenizer tokenizer) {
        return switch (scalar.type) {
            case NODE, LIST -> throw new IllegalStateException();
            case STRING -> {
                scalar.element = ConfigPrimitive.of(scalar.raw);
                yield scalar;
            }
            case BOOLEAN_TRUE -> {
                scalar.element = ConfigPrimitive.TRUE;
                yield scalar;
            }
            case BOOLEAN_FALSE -> {
                scalar.element = ConfigPrimitive.FALSE;
                yield scalar;
            }
            case NULL -> {
                scalar.element = ConfigPrimitive.NULL;
                yield scalar;
            }
            case KEY, ERROR, EOF -> scalar;
            case REFERENCE_TAG -> {
                try {
                    int value = Integer.parseInt(scalar.raw.substring(1));
                    if (value < 0) {
                        yield INVALID_REFERENCE_TAG;
                    }

                    tokenizer.acceptReferenceTag(value);
                    yield scalar;
                }
                catch (NumberFormatException ignored) {
                    yield INVALID_REFERENCE_TAG;
                }
            }
            case REFERENCE -> {
                try {
                    int value = Integer.parseInt(scalar.raw.substring(1));
                    if (value < 0) {
                        yield INVALID_REFERENCE_TAG;
                    }

                    scalar.reference = value;
                    yield scalar;
                }
                catch (NumberFormatException ignored) {
                    yield INVALID_REFERENCE_TAG;
                }
            }
            case NUMBER_LONG ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Long.parseLong(s, r)), scalar, true, true);
            case NUMBER_DOUBLE ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Double.parseDouble(s)), scalar, false, false);
            case NUMBER_INT ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Integer.parseInt(s, r)), scalar, false, true);
            case NUMBER_FLOAT ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Float.parseFloat(s)), scalar, true, false);
            case NUMBER_SHORT ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Short.parseShort(s, r)), scalar, true, true);
            case NUMBER_BYTE ->
                parseNumericLiteral((s, r) -> ConfigPrimitive.of(Byte.parseByte(s, r)), scalar, true, true);
        };
    }

    private static Scalar parseNumericLiteral(NumericParseFunction function, Scalar scalar, boolean chopEnd, boolean hasRadix) {
        try  {
            String raw = scalar.raw;

            int length = raw.length();
            int end = chopEnd ? length - 1 : length;
            if (!hasRadix) {
                scalar.element = function.parse(raw.substring(0, end), 10);
                return scalar;
            }

            int offset = raw.charAt(0) == MINUS_SIGN ? 1 : 0;
            int radix = determineRadix(raw, offset);

            scalar.element = function.parse(processString(raw, radix, end, offset), radix);
            return scalar;
        }
        catch (NumberFormatException ignored) {
            return INVALID_LITERAL;
        }
    }

    private static String processString(String raw, int radix, int end, int offset) {
        return switch (radix) {
            case 16, 2 -> {
                if (offset == 0) {
                    yield raw.substring(2, end);
                }

                yield "-" + raw.substring(3, end);
            }
            default -> raw.substring(0, end);
        };
    }

    private static int determineRadix(String raw, int offset) {
        if (raw.charAt(offset) == SPECIAL_RADIX_PREFIX && raw.length() >= 2 + offset) {
            return switch (toUpperAscii(raw.charAt(1 + offset))) {
                case HEXADECIMAL_INDICATOR -> 16;
                case BINARY_INDICATOR -> 2;
                default -> 8;
            };
        }

        return 10;
    }
}