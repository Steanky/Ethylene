package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.IntFunction;

/**
 * Utilities for parsing out Butylene configuration data.
 * <p>
 * This class is public to enable cross-package access, but is not considered part of the public API, and may change at
 * any time.
 */
@ApiStatus.Internal
public class Parser {
    /**
     * A descriptive enum for tokens encountered in Butylene data. The contents of the token are available inside the
     * tokenizer buffer, and are not stored in this enum.
     */
    private enum Token {
        /**
         * Text that's been enclosed in quotation marks. It may contain a broader range of characters, as well as
         * escape sequences.
         */
        QUOTED_TEXT,

        /**
         * Text that's not enclosed in quotation marks. These are more restricted than quoted text when it comes to the
         * allowed characters, and does not allow escape sequences.
         */
        UNQUOTED_TEXT,

        /**
         * An explicit value separator token.
         */
        VALUE_SEPARATOR,

        /**
         * A value assignment token (colon).
         */
        VALUE_ASSIGN,

        /**
         * A "map start" token, i.e. an opening curly brace.
         */
        MAP_START,

        /**
         * A "map end" token, i.e. a closing curly brace.
         */
        MAP_END,

        /**
         * A "list start" token, i.e. an opening square bracket.
         */
        LIST_START,

        /**
         * A "list end" token, i.e. a closing square bracket.
         */
        LIST_END,

        /**
         * An anchor token, i.e. an ampersand.
         */
        ANCHOR,

        /**
         * A reference token, i.e. an asterisk.
         */
        REFERENCE,

        /**
         * An override token, i.e. a closing angle bracket.
         */
        OVERRIDE,

        /**
         * End-of-file token.
         */
        EOF
    }

    /**
     * The state that the tokenizer is currently in.
     */
    private enum TokenizerState {
        /**
         * Initial state. We're searching for new tokens.
         */
        SEEK,

        /**
         * Parsing an unquoted TEXT token.
         */
        UNQUOTED_TEXT,

        /**
         * Looking for something to terminate the last text token in a list or map.
         */
        UNQUOTED_TEXT_TERMINATOR,

        /**
         * Parsing an unquoted ANCHOR token.
         */
        ANCHOR,

        /**
         * Parsing a quoted TEXT token.
         */
        QUOTED_TEXT,
    }

    /**
     * The mode used when parsing unquoted text.
     */
    private enum UnquotedTextMode {
        /**
         * "Normal" unquoted text, that is not an anchor. This is the key part of a key:value pair, an unquoted literal
         * like {@code true}, {@code null} or {@code 42}, or an override/reference.
         */
        NORMAL,

        /**
         * Anchor unquoted text.
         */
        ANCHOR,
    }

    private static final int SPACE = ' ';
    private static final int STRING_DELIMITER = '"';
    private static final int VALUE_ASSIGN = ':';
    private static final int VALUE_SEPARATOR = ',';
    private static final int MAP_START = '{';
    private static final int MAP_END = '}';
    private static final int LIST_START = '[';
    private static final int LIST_END = ']';
    private static final int ANCHOR = '&';
    private static final int REFERENCE = '*';
    private static final int OVERRIDE = '>';
    private static final int COMMENT_START = '/';
    private static final int MULTILINE_COMMENT_SIGNIFIER = '*';
    private static final int REPLACEMENT = 0xFFFD;

    private static final int ESCAPE = '\\';
    private static final int BACKSPACE = '\b';
    private static final int FORM_FEED = '\f';
    private static final int LINE_FEED = '\n';
    private static final int CARRIAGE_RETURN = '\r';
    private static final int TAB = '\t';

    private static final int INITIAL_CAPACITY = 10;

    private static boolean validInUnquotedText(int character) {
        // excludes LINE_FEED and CARRIAGE_RETURN, too
        if (character < 0x20) return false;

        return switch (character) {
            case STRING_DELIMITER, SPACE,
                 VALUE_ASSIGN, VALUE_SEPARATOR,
                 MAP_START, MAP_END,
                 LIST_START, LIST_END,
                 ANCHOR, REFERENCE, OVERRIDE,
                 COMMENT_START, ESCAPE, '<' -> false;
            default -> Character.isValidCodePoint(character);
        };
    }

    private static boolean mayTerminateUnquotedText(int character, @NotNull UnquotedTextMode mode) {
        // anchors are terminated by a space, a {, or a [
        if (mode == UnquotedTextMode.ANCHOR) return switch (character) {
            case SPACE, TAB, MAP_START, LIST_START, LINE_FEED, CARRIAGE_RETURN -> true;
            default -> false;
        };

        return switch (character) {
            // carriage return may terminate as well, which conveniently lets us work with CRLF endings
            // the proceeding linefeed will be ignored when in seek mode
            case VALUE_ASSIGN,
                 VALUE_SEPARATOR, MAP_END,
                 LIST_END, LINE_FEED,
                 CARRIAGE_RETURN -> true;

            default -> false;
        };
    }

    private enum NumberParseState {
        NEG,
        POST_NEG,
        LEADING_ZERO,
        INT,
        LEADING_FRAC_DIGIT,
        FRAC,
        EXP_SIGN,
        EXP_START,
        EXP
    }

    private static @NotNull ButyleneParseException invalidCharacterInUnquotedLiteral(@NotNull Tokenizer tokenizer, int i) {
        return new ButyleneParseException("invalid character in unquoted literal", tokenizer.buffer.toString(), i,
            tokenizer.tokenLine, tokenizer.tokenColumn);
    }

    private static boolean nonDigit(char c) {
        return c < '0' || c > '9';
    }

    private static @NotNull ConfigPrimitive parseUnquotedText(@NotNull Tokenizer tokenizer, CharSequence buffer) throws IOException {
        if ("true".contentEquals(buffer)) return ConfigPrimitive.TRUE;
        else if ("false".contentEquals(buffer)) return ConfigPrimitive.FALSE;
        else if ("null".contentEquals(buffer)) return ConfigPrimitive.NULL;

        // check for other special values: [+|-]?(NaN|Infinity)
        if (!buffer.isEmpty()) {
            char first = buffer.charAt(0);
            boolean positive = true;

            int start = switch (first) {
                case '-' -> {
                    positive = false;
                    yield 1;
                }
                case '+' -> 1;
                default -> 0;
            };

            CharSequence remaining = buffer.subSequence(start, buffer.length());
            if ("NaN".contentEquals(remaining)) return ConfigPrimitive.NaN;
            else if ("Infinity".contentEquals(remaining)) return positive ? ConfigPrimitive.POSITIVE_INFINITY :
                ConfigPrimitive.NEGATIVE_INFINITY;
        }

        NumberParseState state = NumberParseState.NEG;
        for (int i = 0; i < buffer.length(); i++) {
            char sample = buffer.charAt(i);

            switch (state) {
                case NEG -> {
                    if (sample == '-') {
                        state = NumberParseState.POST_NEG;
                        continue;
                    }

                    if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }

                case POST_NEG -> {
                    if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }

                case LEADING_ZERO -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                };

                case INT -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.INT;
                    }
                };

                case LEADING_FRAC_DIGIT -> {
                    if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = NumberParseState.FRAC;
                }

                case FRAC -> state = switch (sample) {
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.FRAC;
                    }
                };

                case EXP_SIGN -> state = switch (sample) {
                    case '-', '+' -> NumberParseState.EXP_START;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.EXP;
                    }
                };

                case EXP_START, EXP -> {
                    if (nonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = NumberParseState.EXP;
                }
            }
        }

        switch (state) {
            case INT, FRAC, EXP, LEADING_ZERO -> {}
            default -> throw new ButyleneParseException("malformed number", buffer.toString(), -1, tokenizer.tokenLine,
                tokenizer.tokenColumn);
        }

        // this should never throw an exception as we validate the number above
        double value = Double.parseDouble(buffer.toString());

        // for integers: use the smallest data type that can hold the value without data loss
        if (value == Math.rint(value)) {
            if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) return ConfigPrimitive.of((byte) value);
            else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) return ConfigPrimitive.of((short) value);
            else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) return ConfigPrimitive.of((int) value);
            else if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) return ConfigPrimitive.of((long) value);
        }

        float floatValue = (float) value;

        // we may be able to store the value in a float without data loss
        if (value == floatValue) return ConfigPrimitive.of(floatValue);

        return ConfigPrimitive.of(value);
    }

    private static final class Tokenizer {
        private final ButyleneReader reader;
        private StringBuilder buffer;

        private TokenizerState state;

        private int tokenLine;
        private int tokenColumn;

        private boolean expectLowSurrogate;
        private char highSurrogate;

        private Token nextToken;
        private int nextTokenLine;
        private int nextTokenColumn;
        private StringBuilder nextBuffer;

        private Tokenizer(@NotNull ButyleneReader reader) {
            this.reader = Objects.requireNonNull(reader);
            this.buffer = new StringBuilder();
            this.nextBuffer = new StringBuilder();

            this.state = TokenizerState.SEEK;
        }

        private @NotNull String display(@NotNull Token token) {
            return switch (token) {
                case QUOTED_TEXT, UNQUOTED_TEXT -> buffer.toString();
                case VALUE_SEPARATOR -> ",";
                case VALUE_ASSIGN -> ":";
                case MAP_START -> "{";
                case MAP_END -> "}";
                case LIST_START -> "[";
                case LIST_END -> "]";
                case ANCHOR -> "&";
                case REFERENCE -> "*";
                case OVERRIDE -> ">";
                case EOF -> " ";
            };
        }

        private int nextHexDigit(char[] ctx, int idx) throws IOException {
            int next = reader.next();
            int nextSave = next;

            if (next == -1) {
                ctx[idx] = ' ';
                String token = new String(ctx, 0, idx + 1);

                throw new ButyleneParseException("unexpected EOF when parsing Unicode escape sequence",
                    token, idx, reader.getLine(), reader.getColumn());
            }

            if (next >= 0x61 && next <= 0x66) next -= 39;
            else if (next >= 0x41 && next <= 0x46) next -= 7;

            // 0-F becomes 0-15
            next -= 0x30;

            if (next < 0 || next > 15) {
                String token = new String(ctx, 0, idx) + Character.toString(nextSave);
                throw new ButyleneParseException("invalid hexadecimal digit when parsing Unicode escape sequence",
                    token, idx, reader.getLine(), reader.getColumn());
            }

            // safe to cast to char: nextSave is guaranteed to be in [a-fA-F0-9]
            ctx[idx] = (char) nextSave;
            return next;
        }

        private char readHexdigits(char[] ctx) throws IOException {
            int one = nextHexDigit(ctx, 2); // MSB
            int two = nextHexDigit(ctx, 3);
            int three = nextHexDigit(ctx, 4);
            int four = nextHexDigit(ctx, 5); // LSB

            return (char) ((one << 12) | (two << 8) | (three << 4) | four);
        }

        private void swapBuffers() {
            StringBuilder saved = nextBuffer;

            nextBuffer = buffer;
            buffer = saved;
        }

        private @NotNull Token peekNext() throws IOException {
            if (nextToken != null) return nextToken;

            swapBuffers();
            int lineSave = tokenLine;
            int columnSave = tokenColumn;

            nextToken = next();

            nextTokenLine = tokenLine;
            nextTokenColumn = tokenColumn;

            tokenLine = lineSave;
            tokenColumn = columnSave;
            swapBuffers();

            return nextToken;
        }

        private @NotNull Token next() throws IOException {
            if (nextToken != null) {
                Token nextSave = nextToken;
                nextToken = null;

                tokenLine = nextTokenLine;
                tokenColumn = nextTokenColumn;

                nextTokenLine = 0;
                nextTokenColumn = 0;

                nextBuffer.setLength(0);
                return nextSave;
            }

            buffer.setLength(0);
            Token nextToken;

            do {
                int columnStart = reader.getColumn();

                // the first non-null token will be returned
                nextToken = switch (state) {
                    case SEEK -> doSeek(reader.next());
                    case UNQUOTED_TEXT -> doUnquotedText(UnquotedTextMode.NORMAL);
                    case UNQUOTED_TEXT_TERMINATOR -> doUnquotedTextTerminator();
                    case ANCHOR -> doUnquotedText(UnquotedTextMode.ANCHOR);
                    case QUOTED_TEXT -> doQuotedText(reader.next());
                };

                if (nextToken != null) {
                    tokenLine = reader.getLine();
                    tokenColumn = columnStart - buffer.length();
                }
            } while (nextToken == null);

            return nextToken;
        }

        private @Nullable Token doSeek(int character) throws IOException {
            return switch (character) {
                // end of file
                case -1 -> Token.EOF;

                // as per https://www.rfc-editor.org/rfc/rfc8259 "JSON Grammar", these are "insignificant whitespace"
                // characters that are entirely ignored when in seek mode
                case SPACE, TAB, LINE_FEED, CARRIAGE_RETURN -> null;

                // start a quoted text sequence
                case STRING_DELIMITER -> {
                    state = TokenizerState.QUOTED_TEXT;
                    yield null;
                }

                // single-character tokens
                case VALUE_ASSIGN -> Token.VALUE_ASSIGN;
                case VALUE_SEPARATOR -> Token.VALUE_SEPARATOR;
                case MAP_START -> Token.MAP_START;
                case MAP_END -> Token.MAP_END;
                case LIST_START -> Token.LIST_START;
                case LIST_END -> Token.LIST_END;

                case ANCHOR -> {
                    state = TokenizerState.ANCHOR;
                    yield Token.ANCHOR;
                }

                case REFERENCE -> {
                    state = TokenizerState.UNQUOTED_TEXT;
                    yield Token.REFERENCE;
                }

                case OVERRIDE -> {
                    state = TokenizerState.UNQUOTED_TEXT;
                    yield Token.OVERRIDE;
                }

                case COMMENT_START -> {
                    int next = reader.next();

                    if (next == -1)
                        throw new ButyleneParseException("unexpected EOF", "/ ", 1, reader.getLine(), reader.getColumn());

                    switch (next) {
                        case COMMENT_START -> drainLineComment();
                        case MULTILINE_COMMENT_SIGNIFIER -> drainMultilineComment();

                        default -> throw new ButyleneParseException("invalid character",
                            "/" + Character.toString(next), 1, reader.getLine(), reader.getColumn());
                    }

                    yield null;
                }

                default -> {
                    if (!validInUnquotedText(character)) throw new ButyleneParseException("invalid character",
                        Character.toString(character), 0, reader.getLine(), reader.getColumn());

                    buffer.appendCodePoint(character);
                    state = TokenizerState.UNQUOTED_TEXT;
                    yield null;
                }
            };
        }

        private @Nullable Token doUnquotedText(@NotNull UnquotedTextMode mode) throws IOException {
            // we may not want to "consume" the next character if it's something like a curly bracket (that should emit
            // a separate token)
            int preview = reader.peekNext();

            if (mode != UnquotedTextMode.ANCHOR) {
                switch (preview) {
                    // spaces and tabs MAY terminate unquoted text, but only if it is proceeded by non-text
                    case SPACE, TAB -> {
                        // consume the peeked character
                        reader.next();
                        state = TokenizerState.UNQUOTED_TEXT_TERMINATOR;
                        return null;
                    }
                }
            }

            // EOF may terminate unquoted text, too
            if (preview == -1 || mayTerminateUnquotedText(preview, mode)) {
                state = TokenizerState.SEEK;
                return Token.UNQUOTED_TEXT;
            }

            if (!validInUnquotedText(preview)) {
                String copy = buffer + Character.toString(preview);

                throw new ButyleneParseException("invalid character",
                    copy, buffer.length(), reader.getLine(), reader.getColumn());
            }

            // actually advance the reader since our next is valid in unquoted text
            reader.next();
            buffer.appendCodePoint(preview);

            return null;
        }

        private @Nullable Token doUnquotedTextTerminator() throws IOException {
            int lookahead = reader.peekNext();

            return switch (lookahead) {
                case SPACE, TAB -> {
                    reader.next();
                    yield null;
                }

                default -> {
                    if (lookahead == -1 || mayTerminateUnquotedText(lookahead, UnquotedTextMode.NORMAL)) {
                        state = TokenizerState.SEEK;

                        tokenLine = reader.getLine();
                        tokenColumn = reader.getColumn();
                        yield Token.UNQUOTED_TEXT;
                    }

                    if (lookahead == COMMENT_START) {
                        reader.next();

                        int comment = reader.next();
                        switch (comment) {
                            case COMMENT_START -> drainLineComment();
                            case MULTILINE_COMMENT_SIGNIFIER -> drainMultilineComment();

                            default -> throw new ButyleneParseException("invalid character",
                                new String(new int[] { comment }, 0, 1), buffer.length(), reader.getLine(),
                                reader.getColumn());
                        }

                        yield null;
                    } else throw new ButyleneParseException("missing separator", buffer + " ", buffer.length(),
                        reader.getLine(), reader.getColumn());
                }
            };
        }

        private void appendIfValid(int codepoint) {
            if (Character.isValidCodePoint(codepoint)) buffer.appendCodePoint(codepoint);
            else buffer.appendCodePoint(REPLACEMENT);
        }

        private void resetWith(int codepoint) {
            appendIfValid(codepoint);
            expectLowSurrogate = false;
            highSurrogate = 0;
        }

        private @Nullable Token doQuotedText(int character) throws IOException {
            if (character == -1) {
                String message = "\"" + buffer + ' ';
                throw new ButyleneParseException("unexpected EOF when parsing quoted string", message,
                    message.length() - 1, reader.getLine(), reader.getColumn());
            }

            if (character < 0x20) {
                String message = "\"" + buffer + ((char) character);
                throw new ButyleneParseException("invalid character in quoted string", message, message.length() - 1,
                    reader.getLine(), reader.getColumn());
            }

            if (expectLowSurrogate && (character != ESCAPE || reader.peekNext() != 'u')) resetWith(REPLACEMENT);

            switch (character) {
                case STRING_DELIMITER -> {
                    state = TokenizerState.SEEK;
                    return Token.QUOTED_TEXT;
                }

                case ESCAPE -> {
                    int next = reader.next();

                    switch (next) {
                        // common sequences that just escape the next character
                        case '"', '\\', '/' -> buffer.append((char) next);

                        // escape sequences that are shorthand for special characters
                        case 'b' -> buffer.append(BACKSPACE);
                        case 'f' -> buffer.append(FORM_FEED);
                        case 'n' -> buffer.append(LINE_FEED);
                        case 'r' -> buffer.append(CARRIAGE_RETURN);
                        case 't' -> buffer.append(TAB);

                        // as per https://www.rfc-editor.org/rfc/rfc8259, we may encode arbitrary Unicode characters
                        // with 4 hex digits
                        case 'u' -> {
                            char[] ctx = new char[6];
                            ctx[0] = '\\';
                            ctx[1] = 'u';

                            char decoded = readHexdigits(ctx);

                            boolean high = Character.isHighSurrogate(decoded);
                            boolean low = Character.isLowSurrogate(decoded);

                            if ((expectLowSurrogate && !low) || (!expectLowSurrogate && low)) resetWith(REPLACEMENT);
                            else if (high) {
                                expectLowSurrogate = true;
                                highSurrogate = decoded;
                            }
                            else if (low) resetWith(Character.toCodePoint(highSurrogate, decoded));
                            else buffer.append(decoded);
                        }

                        case -1 -> {
                            String message = "\"" + buffer + '\\' + ' ';
                            throw new ButyleneParseException("unexpected EOF when parsing escape code", message,
                                message.length() - 1, reader.getLine(), reader.getColumn());
                        }

                        default -> {
                            String message = "\"" + buffer + '\\' + Character.toString(next);
                            throw new ButyleneParseException("invalid escape code", message, message.length() - 1,
                                reader.getLine(), reader.getColumn());
                        }
                    }
                }

                default -> appendIfValid(character);
            }

            return null;
        }

        private void drainLineComment() throws IOException {
            while (true) {
                int peek = reader.peekNext();

                // don't drain the EOF
                if (peek == -1) return;

                reader.next();
                if (peek == LINE_FEED) return;
            }
        }

        private void drainMultilineComment() throws IOException {
            while (true) {
                int peek = reader.peekNext();

                // EOF in multiline comment is an error
                if (peek == -1) throw multilineCommentException();

                if (peek != MULTILINE_COMMENT_SIGNIFIER) {
                    reader.next();
                    continue;
                }

                // this is the *
                reader.next();

                // only exit if we found the terminating */
                if (reader.next() == COMMENT_START) return;
            }
        }

        private ButyleneParseException multilineCommentException() {
            return new ButyleneParseException("unexpected EOF in multiline comment", null, -1, reader.getLine(),
                reader.getColumn());
        }
    }

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

    private record DeferredResolve(String name, String key, int idx, ContainerContext context, int tokenLine, int tokenColumn, boolean isReference) { }

    private static @NotNull ButyleneParseException invalidToken(@NotNull String string, @NotNull Token token, @NotNull Tokenizer tokenizer) {
        return new ButyleneParseException(string, tokenizer.display(token), -1, tokenizer.tokenLine,
            tokenizer.tokenColumn);
    }

    private static @NotNull ButyleneParseException invalidToken(@NotNull Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("invalid token at this position", token, tokenizer);
    }

    private static @NotNull ButyleneParseException unclosedCurlyBraces() {
        return new ButyleneParseException("missing one or more closing curly braces", null, -1, -1, -1);
    }

    private static @NotNull ButyleneParseException missingAnchor(@NotNull String name, int tokenLine, int tokenColumn) {
        return new ButyleneParseException("missing anchor", name, -1, tokenLine, tokenColumn);
    }

    private static @NotNull ButyleneParseException missingAnchor(@NotNull DeferredResolve deferredResolve) {
        return missingAnchor((deferredResolve.isReference ? "*" : ">") + deferredResolve.name,
            deferredResolve.tokenLine, deferredResolve.tokenColumn);
    }

    public static @NotNull ConfigElement fromReader(@NotNull Reader reader,
        @NotNull IntFunction<? extends ConfigNode> nodeFunction,
        @NotNull IntFunction<? extends ConfigList> listFunction) throws IOException {
        Tokenizer tokenizer = new Tokenizer(new ButyleneReader(reader));

        int listDepth = 0;
        int mapDepth = 0;

        Map<String, ConfigElement> anchorMap = null;
        Deque<DeferredResolve> resolves = null;

        Token token = tokenizer.next();
        String rootAnchor = null;

        // special case for root node or value with anchor
        if (token == Token.ANCHOR) {
            Token anchorName = tokenizer.next();
            if (anchorName != Token.UNQUOTED_TEXT) throw invalidToken(anchorName, tokenizer);

            rootAnchor = tokenizer.buffer.toString();

            switch (token = tokenizer.next()) {
                case LIST_START, MAP_START, QUOTED_TEXT, UNQUOTED_TEXT -> { }
                default -> throw invalidToken(token, tokenizer);
            }
        }

        boolean topLevelMap = token != Token.LIST_START;
        boolean eofClosesTopLevelMap = rootAnchor == null && token != Token.LIST_START && token != Token.MAP_START;

        if (token == Token.LIST_START || token == Token.MAP_START) {
            if (token == Token.LIST_START) listDepth ++;
            else mapDepth++;

            token = tokenizer.next();
        }
        else if ((token == Token.UNQUOTED_TEXT || token == Token.QUOTED_TEXT) && tokenizer.peekNext() == Token.EOF) {
            String value = tokenizer.buffer.toString();

            // handle top-level primitives
            if (token == Token.QUOTED_TEXT) return ConfigPrimitive.of(value);
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
                if (context.foundAny && token == Token.VALUE_SEPARATOR) token = tokenizer.next();
                context.foundAny = true;

                String anchorName = null;
                String key = null;

                if (contextNode != null && token != Token.OVERRIDE) {
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
                            } else throw unclosedCurlyBraces();
                        }
                        default -> throw invalidToken(token, tokenizer);
                    }

                    key = tokenizer.buffer.toString();

                    Token next = tokenizer.next();
                    if (next != Token.VALUE_ASSIGN) throw invalidToken(next, tokenizer);

                    token = tokenizer.next();

                    if (token == Token.OVERRIDE)
                        throw invalidToken("invalid position for override", Token.OVERRIDE, tokenizer);
                }

                // anchors can appear before any value
                if (token == Token.ANCHOR) {
                    if (tokenizer.next() != Token.UNQUOTED_TEXT)
                        throw invalidToken("invalid anchor or override name", token, tokenizer);

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

                        if (tokenizer.next() != Token.UNQUOTED_TEXT) throw invalidToken(token, tokenizer);
                        if (anchorName != null)
                            throw invalidToken("anchor before reference or override", token, tokenizer);

                        boolean isReference = token == Token.REFERENCE;

                        String name = tokenizer.buffer.toString();
                        ConfigElement referenced;
                        if (isReference && anchorMap != null && (referenced = anchorMap.get(name)) != null) {
                            // no need to defer, we already have the anchor
                            if (contextNode != null) contextNode.put(key, referenced);
                            else contextList.add(referenced);
                        } else {
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
                        if (token == Token.MAP_START) {
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
                        if (contextNode != null) throw invalidToken("wrong closing brace type", token, tokenizer);
                        if (--listDepth < 0) throw invalidToken("missing opening brace", token, tokenizer);

                        contextStack.removeLast().maybeMinimize();
                        break entryLoop;
                    }

                    default -> throw invalidToken(token, tokenizer);
                }

                token = tokenizer.next();
            }

            token = tokenizer.next();
        } while (!contextStack.isEmpty());

        if (token != Token.EOF) throw invalidToken("expected EOF", token, tokenizer);
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
                throw new ButyleneParseException("type referenced by override must match the container",
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

                // further indices in the same scope will be
                context.offset += referencedList.size();
            }
        }

        return topLevel;
    }
}