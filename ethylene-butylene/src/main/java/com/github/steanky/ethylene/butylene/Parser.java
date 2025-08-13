package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
         * Same as UNQUOTED_TEXT_TERMINATOR, but for references instead of text.
         */
        UNQUOTED_TEXT_TERMINATOR_REFERENCE,

        /**
         * Parsing an unquoted ANCHOR token.
         */
        ANCHOR_OR_OVERRIDE,

        /**
         * Parsing an unquoted REFERENCE token.
         */
        REFERENCE,

        /**
         * Parsing a quoted TEXT token.
         */
        QUOTED_TEXT,

        /**
         * Parsing a single-line comment.
         */
        LINE_COMMENT,

        /**
         * Parsing a multiline comment.
         */
        MULTILINE_COMMENT
    }

    /**
     * The mode used when parsing unquoted text.
     */
    private enum UnquotedTextMode {
        /**
         * "Normal" unquoted text, that is not an anchor or reference. This may be the key part of a key-value pair, or
         * an unquoted string.
         */
        NORMAL,

        /**
         * Anchor unquoted text.
         */
        ANCHOR_OR_OVERRIDE,

        /**
         * Reference unquoted text.
         */
        REFERENCE
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
                 ANCHOR, REFERENCE,
                 COMMENT_START, ESCAPE, OVERRIDE -> false;
            default -> true;
        };
    }

    private static boolean mayTerminateUnquotedText(int character, @NotNull UnquotedTextMode mode) {
        // anchors MUST be terminated by a space
        if (mode == UnquotedTextMode.ANCHOR_OR_OVERRIDE) return character == SPACE;

        if (mode == UnquotedTextMode.REFERENCE) {
            return switch (character) {
                case VALUE_SEPARATOR, MAP_END,
                     LIST_END, LINE_FEED, CARRIAGE_RETURN -> true;

                default -> false;
            };
        }

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

    private static @NotNull ButyleneParseException invalidCharacterInNumber(@NotNull Tokenizer tokenizer, int i) {
        return new ButyleneParseException("invalid character in number", tokenizer.buffer.toString(), i,
            tokenizer.tokenLine, tokenizer.tokenColumn);
    }

    private static boolean nonDigit(char c) {
        return c < '0' || c > '9';
    }

    private static @NotNull ConfigPrimitive parseUnquotedText(@NotNull Tokenizer tokenizer, CharSequence buffer) throws IOException {
        if ("true".contentEquals(buffer)) return ConfigPrimitive.TRUE;
        else if ("false".contentEquals(buffer)) return ConfigPrimitive.FALSE;
        else if ("null".contentEquals(buffer)) return ConfigPrimitive.NULL;

        NumberParseState state = NumberParseState.NEG;
        for (int i = 0; i < buffer.length(); i++) {
            char sample = buffer.charAt(i);

            switch (state) {
                case NEG -> {
                    if (sample == '-') {
                        state = NumberParseState.POST_NEG;
                        continue;
                    }

                    if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }

                case POST_NEG -> {
                    if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }

                case LEADING_ZERO -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> throw invalidCharacterInNumber(tokenizer, i);
                };

                case INT -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                        yield NumberParseState.INT;
                    }
                };

                case LEADING_FRAC_DIGIT -> {
                    if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                    state = NumberParseState.FRAC;
                }

                case FRAC -> state = switch (sample) {
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                        yield NumberParseState.FRAC;
                    }
                };

                case EXP_SIGN -> state = switch (sample) {
                    case '-', '+' -> NumberParseState.EXP_START;
                    default -> {
                        if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
                        yield NumberParseState.EXP;
                    }
                };

                case EXP_START, EXP -> {
                    if (nonDigit(sample)) throw invalidCharacterInNumber(tokenizer, i);
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
        return ConfigPrimitive.of(Double.valueOf(buffer.toString()));
    }

    private static final class Tokenizer {
        private final PeekingReader reader;
        private final StringBuilder buffer;

        private TokenizerState state;

        private int tokenLine;
        private int tokenColumn;

        private Tokenizer(@NotNull PeekingReader reader) {
            this.reader = Objects.requireNonNull(reader);
            this.buffer = new StringBuilder();

            this.state = TokenizerState.SEEK;
        }

        private @NotNull String display(@NotNull Token token) {
            return switch (token) {
                case QUOTED_TEXT, UNQUOTED_TEXT -> this.buffer.toString();
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

            if (next >= 0x61 && next <= 0x66) next -= 47;
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

        private char readHexdigits() throws IOException {
            char[] ctx = new char[6];
            ctx[0] = '\\';
            ctx[1] = 'u';

            int one = nextHexDigit(ctx, 2); // MSB
            int two = nextHexDigit(ctx, 3);
            int three = nextHexDigit(ctx, 4);
            int four = nextHexDigit(ctx, 5); // LSB

            return (char) ((one << 12) | (two << 8) | (three << 4) | four);
        }

        private @NotNull Token next() throws IOException {
            buffer.setLength(0);

            Token nextToken;

            do {
                int columnStart = this.reader.getColumn();

                // the first non-null token will be returned
                nextToken = switch (this.state) {
                    case SEEK -> doSeek(reader.next());
                    case UNQUOTED_TEXT -> doUnquotedText(UnquotedTextMode.NORMAL);
                    case UNQUOTED_TEXT_TERMINATOR -> doUnquotedTextTerminatorLookahead(UnquotedTextMode.NORMAL);
                    case UNQUOTED_TEXT_TERMINATOR_REFERENCE -> doUnquotedTextTerminatorLookahead(UnquotedTextMode.REFERENCE);
                    case ANCHOR_OR_OVERRIDE -> doUnquotedText(UnquotedTextMode.ANCHOR_OR_OVERRIDE);
                    case REFERENCE -> doUnquotedText(UnquotedTextMode.REFERENCE);
                    case QUOTED_TEXT -> doQuotedText(reader.next());
                    case LINE_COMMENT -> doLineComment(reader.next());
                    case MULTILINE_COMMENT -> doMultilineComment(reader.next());
                };

                if (nextToken != null) {
                    this.tokenLine = this.reader.getLine();
                    this.tokenColumn = columnStart - this.buffer.length();
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
                    this.state = TokenizerState.QUOTED_TEXT;
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
                    this.state = TokenizerState.ANCHOR_OR_OVERRIDE;
                    yield Token.ANCHOR;
                }

                case REFERENCE -> {
                    this.state = TokenizerState.REFERENCE;
                    yield Token.REFERENCE;
                }

                case OVERRIDE -> {
                    this.state = TokenizerState.ANCHOR_OR_OVERRIDE;
                    yield Token.OVERRIDE;
                }

                case COMMENT_START -> {
                    int next = reader.next();

                    if (next == -1)
                        throw new ButyleneParseException("unexpected EOF", "/ ", 1, reader.getLine(), reader.getColumn());

                    this.state = switch (next) {
                        // single-line comment
                        case COMMENT_START -> TokenizerState.LINE_COMMENT;

                        // multi-line comment
                        case MULTILINE_COMMENT_SIGNIFIER -> TokenizerState.MULTILINE_COMMENT;

                        default -> throw new ButyleneParseException("invalid character",
                            "/" + Character.toString(next), 1, reader.getLine(), reader.getColumn());
                    };

                    yield null;
                }

                default -> {
                    if (!validInUnquotedText(character)) throw new ButyleneParseException("invalid character",
                        Character.toString(character), 0, reader.getLine(), reader.getColumn());

                    buffer.appendCodePoint(character);
                    this.state = TokenizerState.UNQUOTED_TEXT;
                    yield null;
                }
            };
        }

        private @Nullable Token doUnquotedText(@NotNull UnquotedTextMode mode) throws IOException {
            // we may not want to "consume" the next character if it's something like a curly bracket (that should emit
            // a separate token)
            int preview = reader.peekNext();

            if (mode != UnquotedTextMode.ANCHOR_OR_OVERRIDE) {
                switch (preview) {
                    // spaces and tabs MAY terminate unquoted text, but only if it is proceeded by non-text
                    case SPACE, TAB -> {
                        // consume the peeked character
                        reader.next();

                        if (mode == UnquotedTextMode.NORMAL) this.state = TokenizerState.UNQUOTED_TEXT_TERMINATOR;
                        else this.state = TokenizerState.UNQUOTED_TEXT_TERMINATOR_REFERENCE;

                        return null;
                    }
                }
            }

            // EOF may terminate unquoted text, too
            if (preview == -1 || mayTerminateUnquotedText(preview, mode)) {
                this.state = TokenizerState.SEEK;
                return Token.UNQUOTED_TEXT;
            }

            if (!validInUnquotedText(preview)) {
                String copy = buffer + Character.toString(preview);

                throw new ButyleneParseException("invalid character",
                    copy, buffer.length(), reader.getLine(), reader.getColumn());
            }

            // actually advance the reader since our next is valid in unquoted text
            this.reader.next();
            this.buffer.appendCodePoint(preview);

            return null;
        }

        private @Nullable Token doUnquotedTextTerminatorLookahead(@NotNull UnquotedTextMode mode) throws IOException {
            int lookahead = reader.peekNext();

            return switch (lookahead) {
                case SPACE, TAB, LINE_FEED, CARRIAGE_RETURN -> {
                    reader.next();
                    yield null;
                }

                default -> {
                    if (lookahead == -1 || mayTerminateUnquotedText(lookahead, mode)) {
                        this.state = TokenizerState.SEEK;

                        this.tokenLine = this.reader.getLine();
                        this.tokenColumn = this.reader.getColumn();
                        yield Token.UNQUOTED_TEXT;
                    }

                    throw new ButyleneParseException("missing comma", buffer + " ", buffer.length(), reader.getLine(),
                        reader.getColumn());
                }
            };
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

            switch (character) {
                case STRING_DELIMITER -> {
                    this.state = TokenizerState.SEEK;
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
                        case 'u' -> buffer.append(readHexdigits());

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

                default -> buffer.appendCodePoint(character);
            }

            return null;
        }

        private @Nullable Token doLineComment(int character) {
            if (character == -1) {
                this.state = TokenizerState.SEEK;
                return Token.EOF;
            }

            if (character == LINE_FEED) this.state = TokenizerState.SEEK;
            return null;
        }

        private ButyleneParseException multilineCommentException() {
            return new ButyleneParseException("unexpected EOF in multiline comment", null, -1, reader.getLine(),
                reader.getColumn());
        }

        private @Nullable Token doMultilineComment(int character) throws IOException {
            if (character == -1) throw multilineCommentException();

            if (character == MULTILINE_COMMENT_SIGNIFIER) {
                int next = reader.next();

                if (next == -1) throw multilineCommentException();
                if (next == COMMENT_START) this.state = TokenizerState.SEEK;
            }

            return null;
        }
    }

    @VisibleForTesting
    public record TokenData(Token token, String value) {}

    @VisibleForTesting
    public static List<TokenData> tokenize(@NotNull Reader reader) throws IOException {
        Tokenizer tokenizer = new Tokenizer(new PeekingReader(reader));

        List<TokenData> data = new ArrayList<>();
        Token token;
        do {
            token = tokenizer.next();
            data.add(new TokenData(token, tokenizer.buffer.toString()));
        } while (token != Token.EOF);

        return data;
    }

    private record DeferredResolve(String name, String key, int idx, ConfigList list, ConfigNode map, int tokenLine, int tokenColumn) { }

    private record DeferredOverride(String name, ConfigList list, ConfigNode map, int tokenLine, int tokenColumn) { }

    private static @NotNull ButyleneParseException invalidToken(@NotNull String string, @NotNull Token token, @NotNull Tokenizer tokenizer) {
        return new ButyleneParseException(string, tokenizer.display(token), -1, tokenizer.tokenLine,
            tokenizer.tokenColumn);
    }

    private static @NotNull ButyleneParseException invalidToken(@NotNull Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("invalid token at this position", token, tokenizer);
    }

    private static @NotNull ButyleneParseException invalidOverrideTarget(@NotNull Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("override must be applied to a map or a list", token, tokenizer);
    }

    private static @NotNull ButyleneParseException unclosedCurlyBraces() {
        return new ButyleneParseException("missing one or more closing curly braces", null, -1, -1, -1);
    }

    private static @NotNull ButyleneParseException missingAnchor(@NotNull String name, int tokenLine, int tokenColumn) {
        return new ButyleneParseException("missing corresponding anchor", name, -1, tokenLine, tokenColumn);
    }

    private static @NotNull ButyleneParseException missingAnchor(@NotNull DeferredResolve deferredResolve) {
        return missingAnchor(deferredResolve.name, deferredResolve.tokenLine, deferredResolve.tokenColumn);
    }

    private static @NotNull ButyleneParseException missingAnchor(@NotNull DeferredOverride deferredOverride) {
        return missingAnchor(deferredOverride.name, deferredOverride.tokenLine, deferredOverride.tokenColumn);
    }

    public static @NotNull ConfigElement fromReader(@NotNull Reader reader,
        @NotNull IntFunction<? extends ConfigNode> nodeFunction,
        @NotNull IntFunction<? extends ConfigList> listFunction) throws IOException {
        Tokenizer tokenizer = new Tokenizer(new PeekingReader(reader));

        int listDepth = 0;
        int mapDepth = 0;

        Deque<ConfigContainer> containerStack = new ArrayDeque<>();

        boolean topLevelMap;
        boolean eofClosesTopLevelMap = true;
        boolean maybeTopLevelScalar = false;

        ConfigContainer topLevel = null;
        boolean isFirst = true;

        Map<String, ConfigElement> anchorMap = null;
        List<DeferredResolve> resolves = null;
        List<DeferredOverride> overrides = null;

        Token token;
        do {
            token = tokenizer.next();

            if (isFirst) {
                topLevelMap = token != Token.LIST_START;
                eofClosesTopLevelMap = token != Token.LIST_START && token != Token.MAP_START;
                maybeTopLevelScalar = token == Token.QUOTED_TEXT || token == Token.UNQUOTED_TEXT;

                topLevel = topLevelMap
                    ? nodeFunction.apply(INITIAL_CAPACITY)
                    : listFunction.apply(INITIAL_CAPACITY);

                containerStack.addLast(topLevel);

                switch (token) {
                    case LIST_START -> {
                        listDepth++;
                        token = tokenizer.next();
                    }
                    case MAP_START -> {
                        mapDepth++;
                        token = tokenizer.next();
                    }
                }

                isFirst = false;
            }

            ConfigContainer context = containerStack.peekLast();

            ConfigNode contextNode = (context instanceof ConfigNode node) ? node : null;
            ConfigList contextList = (context instanceof ConfigList list) ? list : null;

            assert (contextNode != null) ^ (contextList != null);

            // iterate through container entries
            entryLoop:
            for (;;) {
                if (!context.elementCollection().isEmpty() && token == Token.VALUE_SEPARATOR) token = tokenizer.next();

                String anchorName = null;
                String key = null;
                boolean isTopLevelScalar = false;

                if (contextNode != null) {
                    // the key part of a key: value pair, or EOF, or a closing curly bracket
                    switch (token) {
                        case QUOTED_TEXT, UNQUOTED_TEXT -> {}

                        case MAP_END -> {
                            if (--mapDepth < 0) throw invalidToken("missing opening brace", token, tokenizer);

                            containerStack.removeLast().minimizeStorage();
                            break entryLoop;
                        }

                        case EOF -> {
                            if (eofClosesTopLevelMap && context == topLevel) {
                                containerStack.removeLast().minimizeStorage();
                                break entryLoop;
                            } else throw unclosedCurlyBraces();
                        }

                        default -> throw invalidToken(token, tokenizer);
                    }

                    // this can also be a plain value, in the case of top-level scalars
                    key = tokenizer.buffer.toString();
                    switch (tokenizer.next()) {
                        case VALUE_ASSIGN -> token = tokenizer.next();
                        case EOF -> {
                            if (!maybeTopLevelScalar)
                                throw new ButyleneParseException("expected a value but got EOF instead", null, -1, -1, -1);
                            isTopLevelScalar = true;
                        }

                        default -> throw invalidToken(token, tokenizer);
                    }
                }

                // maybeTopLevelScalar must be true if isTopLevelScalar, so token is QUOTED_TEXT or UNQUOTED_TEXT
                if (isTopLevelScalar) {
                    if (token == Token.QUOTED_TEXT) return ConfigPrimitive.of(key);
                    else return parseUnquotedText(tokenizer, key);
                }

                // this is never again true for the rest of the document
                maybeTopLevelScalar = false;

                /*
                Note: we don't check for duplicate keys! I'd like to disallow it, but doing so breaks JSON compliance
                (JSON can contain duplicate keys).

                Thus, the duplicate key behavior is to just go with the value of whatever entry comes latest.
                 */

                boolean override = false;
                int tokenLine = -1;
                int tokenColumn = -1;

                // anchors can appear before any value
                if (token == Token.ANCHOR || (override = token == Token.OVERRIDE)) {
                    if (tokenizer.next() != Token.UNQUOTED_TEXT)
                        throw invalidToken("invalid anchor or override name", token, tokenizer);

                    tokenLine = tokenizer.tokenLine;
                    tokenColumn = tokenizer.tokenColumn;

                    anchorName = tokenizer.buffer.toString();

                    if (token == Token.ANCHOR && anchorMap != null && anchorMap.containsKey(anchorName))
                        throw invalidToken("duplicate anchor name", token, tokenizer);

                    token = tokenizer.next();
                }

                // value part
                switch (token) {
                    // quoted text here is always a string
                    case QUOTED_TEXT -> {
                        if (override) throw invalidOverrideTarget(token, tokenizer);

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
                        if (override) throw invalidOverrideTarget(token, tokenizer);

                        ConfigPrimitive primitive = parseUnquotedText(tokenizer, tokenizer.buffer);

                        if (contextNode != null) contextNode.put(key, primitive);
                        else contextList.add(primitive);

                        if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, primitive);
                        }
                    }

                    case REFERENCE -> {
                        if (override) throw invalidOverrideTarget(token, tokenizer);
                        if (tokenizer.next() != Token.UNQUOTED_TEXT) throw invalidToken(token, tokenizer);

                        String referenceName = tokenizer.buffer.toString();

                        ConfigElement referenced;
                        if (anchorMap != null && (referenced = anchorMap.get(referenceName)) != null) {
                            // no need to defer, we already have the anchor
                            if (contextNode != null) contextNode.put(key, referenced);
                            else contextList.add(referenced);
                        } else {
                            // references are allowed to refer to anchors that appear later in the config file
                            // so, we defer resolving until later
                            if (resolves == null) resolves = new ArrayList<>();

                            if (contextNode != null) {
                                resolves.add(new DeferredResolve(referenceName, key, -1, null, contextNode,
                                    tokenizer.tokenLine, tokenizer.tokenColumn));

                                // not technically necessary, but we want to increase the size of the map now:
                                // we will trim it after all values have been added
                                contextNode.put(key, ConfigPrimitive.NULL);
                            }
                            else {
                                resolves.add(new DeferredResolve(referenceName, null, contextList.size(),
                                    contextList, null, tokenizer.tokenLine, tokenizer.tokenColumn));

                                // temporary value to occupy this index
                                contextList.add(ConfigPrimitive.NULL);
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

                        containerStack.addLast(newContainer);

                        if (contextNode != null) contextNode.put(key, newContainer);
                        else contextList.add(newContainer);

                        // override implies anchorName != null
                        if (override) {
                            if (overrides == null) overrides = new ArrayList<>();

                            if (token == Token.MAP_START)
                                overrides.add(new DeferredOverride(anchorName, null, (ConfigNode) newContainer,
                                    tokenLine, tokenColumn));
                            else overrides.add(new DeferredOverride(anchorName, (ConfigList) newContainer, null,
                                tokenLine, tokenColumn));
                        } else if (anchorName != null) {
                            if (anchorMap == null) anchorMap = new HashMap<>();
                            anchorMap.put(anchorName, newContainer);
                        }

                        break entryLoop;
                    }

                    case LIST_END -> {
                        if (override) throw invalidToken("expected valid override target", token, tokenizer);
                        if (contextNode != null) throw invalidToken("wrong closing brace type", token, tokenizer);
                        if (--listDepth < 0) throw invalidToken("missing opening brace", token, tokenizer);

                        containerStack.removeLast().minimizeStorage();
                        break entryLoop;
                    }

                    default -> throw invalidToken(token, tokenizer);
                }

                token = tokenizer.next();
            }
        } while (!containerStack.isEmpty());

        if (token != Token.EOF) {
            Token next = tokenizer.next();
            if (next != Token.EOF) throw invalidToken("expected EOF", next, tokenizer);
        }

        if ((resolves != null || overrides != null) && anchorMap == null) {
            if (resolves != null) throw missingAnchor(resolves.get(0));
            else throw missingAnchor(overrides.get(0));
        }

        if (resolves != null) {
            for (DeferredResolve deferred : resolves) {
                ConfigElement referenced = anchorMap.get(deferred.name);

                if (referenced == null) throw missingAnchor(deferred);

                if (deferred.map != null) deferred.map.put(deferred.key, referenced);
                else deferred.list.set(deferred.idx, referenced);
            }
        }

        if (overrides != null) {
            for (DeferredOverride override : overrides) {
                ConfigElement referenced = anchorMap.get(override.name);

                if (referenced == null) throw missingAnchor(override);

                if ((override.map != null && !referenced.isNode()) || (override.list != null && !referenced.isList())) {
                    if (referenced.isNode())
                        throw new ButyleneParseException("override references node but the target is a list",
                            override.name, -1, override.tokenLine, override.tokenColumn);
                    else
                        throw new ButyleneParseException("override references list but the target is a node",
                            override.name, -1, override.tokenLine, override.tokenColumn);
                }

                if (override.map != null) {
                    referenced.asNode().forEach(override.map::putIfAbsent);
                    override.map.minimizeStorage();
                    continue;
                }

                // non-null because deferred.map is null
                assert override.list != null;

                override.list.addAll(referenced.asList());
                override.list.minimizeStorage();
            }
        }

        return topLevel;
    }
}