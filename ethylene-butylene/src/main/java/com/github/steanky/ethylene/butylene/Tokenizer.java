package com.github.steanky.ethylene.butylene;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static com.github.steanky.ethylene.butylene.Util.*;
import static com.github.steanky.ethylene.butylene.Tokenizer.Token.*;

/**
 * Tokenizes Butylene data.
 */
@ApiStatus.Internal
class Tokenizer {
    /**
     * The state that the tokenizer is currently in.
     */
    enum TokenizerState {
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
         * Parsing a quoted text token.
         */
        QUOTED_TEXT,

        /**
         * Parsing a single-quoted text token.
         */
        SINGLE_QUOTED_TEXT
    }

    /**
     * A descriptive enum for tokens encountered in Butylene data. The contents of the token are available inside the
     * tokenizer buffer, and are not stored in this enum.
     */
    enum Token {
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
     * The mode used when parsing unquoted text.
     */
    enum UnquotedTextMode {
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

    private final ButyleneReader reader;

    StringBuilder buffer;

    private TokenizerState state;

    int tokenLine;
    int tokenColumn;

    private boolean expectLowSurrogate;
    private char highSurrogate;

    private Token nextToken;
    private int nextTokenLine;
    private int nextTokenColumn;
    private StringBuilder nextBuffer;

    private static boolean validInUnquotedText(int character) {
        // excludes LINE_FEED and CARRIAGE_RETURN, too
        if (character < 0x20) return false;

        return switch (character) {
            case STRING_DELIMITER, MULTILINE_STRING_DELIMITER,
                 SPACE, VALUE_ASSIGN_CHAR,
                 VALUE_SEPARATOR_CHAR, MAP_START_CHAR,
                 MAP_END_CHAR, LIST_START_CHAR,
                 LIST_END_CHAR, ANCHOR_CHAR,
                 REFERENCE_CHAR, OVERRIDE_CHAR,
                 COMMENT_START, ESCAPE, '<' -> false;
            default -> Character.isValidCodePoint(character);
        };
    }

    private static boolean mayTerminateUnquotedText(int character, @NotNull UnquotedTextMode mode) {
        // anchors are terminated by a space, a {, or a [
        if (mode == UnquotedTextMode.ANCHOR) return switch (character) {
            case SPACE, TAB, MAP_START_CHAR, LIST_START_CHAR, LINE_FEED, CARRIAGE_RETURN -> true;
            default -> false;
        };

        return switch (character) {
            // carriage return may terminate as well, which conveniently lets us work with CRLF endings
            // the proceeding linefeed will be ignored when in seek mode
            case VALUE_ASSIGN_CHAR, VALUE_SEPARATOR_CHAR, MAP_END_CHAR, LIST_END_CHAR, LINE_FEED,
                 CARRIAGE_RETURN -> true;

            default -> false;
        };
    }

    Tokenizer(@NotNull ButyleneReader reader) {
        this.reader = Objects.requireNonNull(reader);
        this.buffer = new StringBuilder();
        this.nextBuffer = new StringBuilder();

        this.state = TokenizerState.SEEK;
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

    @NotNull Token peekNext() throws IOException {
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

    @NotNull Token next() throws IOException {
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
                case QUOTED_TEXT -> doQuotedText(reader.next(), false);
                case SINGLE_QUOTED_TEXT -> doQuotedText(reader.next(), true);
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
            case -1 -> EOF;

            // as per https://www.rfc-editor.org/rfc/rfc8259 "JSON Grammar", these are "insignificant whitespace"
            // characters that are entirely ignored when in seek mode
            case SPACE, TAB, LINE_FEED, CARRIAGE_RETURN -> null;

            // start a quoted text sequence
            case STRING_DELIMITER -> {
                state = TokenizerState.QUOTED_TEXT;
                yield null;
            }

            // multiline string
            case MULTILINE_STRING_DELIMITER -> {
                state = TokenizerState.SINGLE_QUOTED_TEXT;
                yield null;
            }

            // single-character tokens
            case VALUE_ASSIGN_CHAR -> VALUE_ASSIGN;
            case VALUE_SEPARATOR_CHAR -> VALUE_SEPARATOR;
            case MAP_START_CHAR -> MAP_START;
            case MAP_END_CHAR -> MAP_END;
            case LIST_START_CHAR -> LIST_START;
            case LIST_END_CHAR -> LIST_END;

            case ANCHOR_CHAR -> {
                state = TokenizerState.ANCHOR;
                yield ANCHOR;
            }

            case REFERENCE_CHAR -> {
                state = TokenizerState.UNQUOTED_TEXT;
                yield REFERENCE;
            }

            case OVERRIDE_CHAR -> {
                state = TokenizerState.UNQUOTED_TEXT;
                yield OVERRIDE;
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
            return UNQUOTED_TEXT;
        }

        if (!validInUnquotedText(preview)) {
            String copy = buffer + Character.toString(preview);

            throw new ButyleneParseException("invalid character for unquoted text",
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
                    yield UNQUOTED_TEXT;
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

    private void resetWith(int codepoint) {
        if (Character.isValidCodePoint(codepoint)) buffer.appendCodePoint(codepoint);
        else buffer.appendCodePoint(REPLACEMENT_CHARACTER);

        expectLowSurrogate = false;
        highSurrogate = 0;
    }

    private @NotNull ButyleneParseException unexpectedEofInEscapeCode(String quote) {
        String token = quote + buffer + '\\' + ' ';
        return new ButyleneParseException("unexpected EOF when parsing escape code", token, token.length() - 1,
            reader.getLine(), reader.getColumn());
    }

    private @NotNull ButyleneParseException invalidEscapeCode(String quote, int next) {
        String message = quote + buffer + '\\' + Character.toString(next);
        return new ButyleneParseException("invalid escape code", message, message.length() - 1, reader.getLine(),
            reader.getColumn() - 1);
    }

    private @Nullable Token doQuotedText(int character, boolean singleQuote) throws IOException {
        if (character == -1) {
            String message = (singleQuote ? "'" : "\"") + buffer + ' ';
            throw new ButyleneParseException("unexpected EOF when parsing quoted string", message,
                message.length() - 1, reader.getLine(), reader.getColumn());
        }

        if (singleQuote) {
            switch (character) {
                case MULTILINE_STRING_DELIMITER -> {
                    state = TokenizerState.SEEK;
                    return QUOTED_TEXT;
                }

                case ESCAPE -> {
                    int next = reader.next();

                    switch (next) {
                        case -1 -> throw unexpectedEofInEscapeCode("'");
                        case MULTILINE_STRING_DELIMITER -> buffer.append((char) MULTILINE_STRING_DELIMITER);
                        case ESCAPE -> buffer.append((char) ESCAPE);
                        default -> throw invalidEscapeCode("'", next);
                    }
                }

                default -> buffer.appendCodePoint(character);
            }

            return null;
        }

        if (character < 0x20) {
            String message = "\"" + buffer + ((char) character);
            throw new ButyleneParseException("invalid character in quoted string", message, message.length() - 1,
                reader.getLine(), reader.getColumn());
        }

        if (expectLowSurrogate && (character != ESCAPE || reader.peekNext() != 'u'))
            resetWith(REPLACEMENT_CHARACTER);

        switch (character) {
            case STRING_DELIMITER -> {
                state = TokenizerState.SEEK;
                return QUOTED_TEXT;
            }

            case ESCAPE -> {
                int next = reader.next();

                switch (next) {
                    case -1 -> throw unexpectedEofInEscapeCode("\"");

                    // common sequences that just escape the next character
                    case '"', '\\', '/' -> buffer.append((char) next);

                    // escape sequences that are shorthand for special characters
                    case 'b' -> buffer.append((char) BACKSPACE);
                    case 'f' -> buffer.append((char) FORM_FEED);
                    case 'n' -> buffer.append((char) LINE_FEED);
                    case 'r' -> buffer.append((char) CARRIAGE_RETURN);
                    case 't' -> buffer.append((char) TAB);

                    // as per https://www.rfc-editor.org/rfc/rfc8259, we may encode arbitrary Unicode characters
                    // with 4 hex digits
                    case 'u' -> {
                        char[] ctx = new char[6];
                        ctx[0] = '\\';
                        ctx[1] = 'u';

                        char decoded = readHexdigits(ctx);

                        boolean high = Character.isHighSurrogate(decoded);
                        boolean low = Character.isLowSurrogate(decoded);

                        if ((expectLowSurrogate && !low) || (!expectLowSurrogate && low))
                            resetWith(REPLACEMENT_CHARACTER);
                        else if (high) {
                            expectLowSurrogate = true;
                            highSurrogate = decoded;
                        }
                        else if (low) resetWith(Character.toCodePoint(highSurrogate, decoded));
                        else buffer.append(decoded);
                    }

                    default -> throw invalidEscapeCode("\"", next);
                }
            }

            default -> buffer.appendCodePoint(character);
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
            if (peek == -1)
                throw new ButyleneParseException("unexpected EOF in multiline comment", reader.getLine(),
                    reader.getColumn());

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
}
