package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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

    /**
     * Checks if a character is not a digit.
     *
     * @param c the character to check
     * @return true if {@code c} is not a digit from 0-9, false otherwise
     */
    static boolean isNonDigit(char c) {
        return c < '0' || c > '9';
    }

    static @NotNull ButyleneParseException invalidToken(@NotNull String string, @NotNull Tokenizer.Token token, @NotNull Tokenizer tokenizer) {
        int offset = 0;
        String display = switch (token) {
            case UNQUOTED_TEXT -> tokenizer.buffer.toString();
            case QUOTED_TEXT -> {
                offset = 1;
                yield "\"" + tokenizer.buffer.toString() + "\"";
            }
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

        return new ButyleneParseException(string, display, -1, tokenizer.tokenLine, tokenizer.tokenColumn - offset);
    }

    static @NotNull ButyleneParseException wrongBraceType(@NotNull Tokenizer.Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("wrong closing brace type", token, tokenizer);
    }

    static @NotNull ButyleneParseException eofInAnchorName(int line, int column) {
        return new ButyleneParseException("EOF when token name was expected", "& ", 1, line, column);
    }

    static @NotNull ButyleneParseException invalidAnchorOrOverrideName(@NotNull Tokenizer.Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("invalid anchor or override name", token, tokenizer);
    }

    static @NotNull ButyleneParseException invalidToken(@NotNull Tokenizer.Token token, @NotNull Tokenizer tokenizer) {
        return invalidToken("invalid token at this position", token, tokenizer);
    }

    static @NotNull ButyleneParseException unclosedCurlyBraces() {
        return new ButyleneParseException("missing one or more closing curly braces");
    }

    static @NotNull ButyleneParseException missingAnchor(@NotNull String name, int tokenLine, int tokenColumn) {
        return new ButyleneParseException("missing anchor", name, -1, tokenLine, tokenColumn);
    }

    static @NotNull ButyleneParseException missingAnchor(@NotNull Parser.DeferredResolve deferredResolve) {
        return missingAnchor((deferredResolve.isReference() ? "*" : ">") + deferredResolve.name(),
            deferredResolve.tokenLine(), deferredResolve.tokenColumn());
    }

    static @NotNull ButyleneParseException invalidCharacterInUnquotedLiteral(@NotNull Tokenizer tokenizer, int i) {
        return new ButyleneParseException("invalid character in unquoted literal", tokenizer.buffer.toString(), i,
            tokenizer.tokenLine, tokenizer.tokenColumn);
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

                    if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = sample == '0' ? NumberParseState.LEADING_ZERO : NumberParseState.INT;
                }

                case POST_NEG -> {
                    if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
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
                        if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.INT;
                    }
                };

                case LEADING_FRAC_DIGIT -> {
                    if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                    state = NumberParseState.FRAC;
                }

                case FRAC -> state = switch (sample) {
                    case 'e', 'E' -> NumberParseState.EXP_SIGN;
                    default -> {
                        if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.FRAC;
                    }
                };

                case EXP_SIGN -> state = switch (sample) {
                    case '-', '+' -> NumberParseState.EXP_START;
                    default -> {
                        if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
                        yield NumberParseState.EXP;
                    }
                };

                case EXP_START, EXP -> {
                    if (Util.isNonDigit(sample)) throw invalidCharacterInUnquotedLiteral(tokenizer, i);
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
}
