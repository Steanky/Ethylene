package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.github.steanky.ethylene.butylene.ButyleneParseException.builder;

class Util {
    /**
     * The Unicode replacement character, '�'.
     */
    static final int REPLACEMENT = 0xFFFD;

    static final int SPACE = ' ';
    static final int STRING_DELIMITER = '"';
    static final int MULTILINE_STRING_DELIMITER = '\'';
    static final int VALUE_ASSIGN_CHAR = ':';
    static final int VALUE_SEPARATOR_CHAR = ',';
    static final int MAP_START_CHAR = '{';
    static final int MAP_END_CHAR = '}';
    static final int LIST_START_CHAR = '[';
    static final int LIST_END_CHAR = ']';
    static final int ANCHOR_CHAR = '&';
    static final int REFERENCE_CHAR = '*';
    static final int OVERRIDE_CHAR = '>';
    static final int COMMENT_START = '/';
    static final int MULTILINE_COMMENT_SIGNIFIER = '*';

    static final int ESCAPE = '\\';
    static final int BACKSPACE = '\b';
    static final int FORM_FEED = '\f';
    static final int LINE_FEED = '\n';
    static final int CARRIAGE_RETURN = '\r';
    static final int TAB = '\t';

    // generic invalid token position
    static final String E_INVALID_TOKEN_POSITION = "invalid token at this position";
    static final String E_WRONG_CLOSING_BRACE = "wrong closing brace type";
    static final String E_MISSING_CLOSING_BRACE = "missing closing brace";
    static final String E_MISSING_OPENING_BRACE = "missing opening brace";
    static final String E_MISSING_SEPARATOR = "missing separator";

    // relating to EOFs
    static final String E_EOF = "unexpected EOF";
    static final String E_EOF_EXPECTED = "expected EOF";
    static final String E_EOF_IN_QUOTED_STRING = "unexpected EOF in quoted string";
    static final String E_EOF_IN_MULTILINE_STRING = "unexpected EOF in multiline string";
    static final String E_EOF_IN_MULTILINE_COMMENT = "unexpected EOF in multiline comment";
    static final String E_EOF_IN_ANCHOR_NAME = "unexpected EOF in token name";
    static final String E_EOF_IN_ESCAPE = "unexpected EOF in escape code";

    static final String E_INVALID_HEX_DIGIT_IN_UNICODE_ESCAPE = "invalid hex digit in Unicode escape sequence";

    // invalid specific character(s)
    static final String E_INVALID_CHARACTER = "invalid character";
    static final String E_INVALID_ESCAPE = "invalid escape code";
    static final String E_INVALID_CHARACTER_IN_QUOTED_STRING = "invalid character in quoted string";
    static final String E_INVALID_CHARACTER_IN_MULTILINE_STRING = "invalid character in multiline string";
    static final String E_NON_WHITESPACE_IN_MULTILINE_POSTFIX = "non-whitespace character in multiline postfix";

    // anchors/references
    static final String E_INVALID_REFERENCE_NAME = "invalid reference name";
    static final String E_MISSING_ANCHOR = "missing anchor";
    static final String E_DUPLICATE_ANCHOR_NAME = "duplicate anchor name";
    static final String E_ANCHOR_BEFORE_REFERENCE = "anchor before reference or override";
    static final String E_SELF_REFERENTIAL_OVERRIDE = "self referential override";
    static final String E_INVALID_REFERENCED_TYPE = "referenced type mismatch";

    // literals
    static final String E_INVALID_LITERAL = "invalid literal";
    static final String E_INVALID_CHARACTER_IN_UNQUOTED_LITERAL = "invalid character in unquoted literal";

    /**
     * Checks if a character is not a digit.
     *
     * @param c the character to check
     * @return true if {@code c} is not a digit from 0-9, false otherwise
     */
    static boolean isNonDigit(char c) {
        return c < '0' || c > '9';
    }

    private static void appendOrEscape(StringBuilder buffer, char c) {
        switch (c) {
            case BACKSPACE -> buffer.append("\\b");
            case FORM_FEED -> buffer.append("\\f");
            case LINE_FEED -> buffer.append("\\n");
            case CARRIAGE_RETURN -> buffer.append("\\r");
            case TAB -> buffer.append("\\t");
            default -> buffer.append(c);
        }
    }

    static @NotNull String aroundEnd(@NotNull StringBuilder buffer, boolean prependQuote, int append) {
        int start = Math.max(0, buffer.length() - 20);
        String substring = buffer.substring(start, buffer.length());

        StringBuilder cleaned = new StringBuilder(substring.length() + 2);
        if (start == 0) {
            if (prependQuote) cleaned.append('"');
        }
        else cleaned.append("...");

        for (int i = 0; i < substring.length(); i++) appendOrEscape(cleaned, substring.charAt(i));
        if (append >= 0) appendOrEscape(cleaned, (char) append);
        return cleaned.toString();
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

    private static void requireDigit(char sample, Tokenizer tokenizer, int i) throws ButyleneParseException {
        if (Util.isNonDigit(sample)) builder()
            .reason(E_INVALID_CHARACTER_IN_UNQUOTED_LITERAL)
            .token(tokenizer.buffer)
            .tokenIndex(i)
            .locationFrom(tokenizer)
            .raise();
    }

    static @NotNull ConfigPrimitive parseUnquotedText(@NotNull Tokenizer tokenizer, @NotNull CharSequence buffer) throws IOException {
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

                    requireDigit(sample, tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }
                case POST_NEG -> {
                    requireDigit(sample, tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }
                case LEADING_ZERO -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> throw builder()
                        .reason(E_INVALID_CHARACTER_IN_UNQUOTED_LITERAL)
                        .token(buffer)
                        .tokenIndex(i)
                        .locationFrom(tokenizer)
                        .build();
                };
                case INT -> state = switch (sample) {
                    case '.' -> NumberParseState.LEADING_FRAC_DIGIT;
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        requireDigit(sample, tokenizer, i);
                        yield NumberParseState.INT;
                    }
                };
                case LEADING_FRAC_DIGIT -> {
                    requireDigit(sample, tokenizer, i);
                    state = NumberParseState.FRAC;
                }
                case FRAC -> state = switch (sample) {
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        requireDigit(sample, tokenizer, i);
                        yield NumberParseState.FRAC;
                    }
                };
                case EXP_SIGN -> state = switch (sample) {
                    case '-', '+' -> NumberParseState.EXP_START;
                    default -> {
                        requireDigit(sample, tokenizer, i);
                        yield NumberParseState.EXP;
                    }
                };
                case EXP_START, EXP -> {
                    requireDigit(sample, tokenizer, i);
                    state = NumberParseState.EXP;
                }
            }
        }

        switch (state) {
            case INT, FRAC, EXP, LEADING_ZERO -> {}
            default -> builder()
                .reason(E_INVALID_LITERAL)
                .token(buffer)
                .locationFrom(tokenizer)
                .raise();
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
}
