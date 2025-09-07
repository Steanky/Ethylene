package com.github.steanky.ethylene.butylene;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * An exception thrown when invalid Butylene is encountered during parsing or tokenization.
 */
public class ButyleneParseException extends IOException {
    private final String reason;
    private final String token;
    private final int tokenIndex;
    private final int line;
    private final int column;

    /**
     * Creates a new instance of this exception.
     *
     * @param reason the reason why the Butylene data is invalid; may be null if no reason is given
     * @param token the particular malformed token; null if no specific token is incorrect
     * @param tokenIndex the index within {@code token} that is invalid; negative indicates no specific invalid index
     * @param line the line at which the error occurred; negative if no specific line is invalid
     * @param column the column of the start of the invalid token; negative if no specific column is invalid
     */
    private ButyleneParseException(@Nullable String reason, @Nullable String token, int tokenIndex, int line, int column) {
        this.reason = reason;
        this.token = token;
        this.tokenIndex = (token == null || tokenIndex < 0 || tokenIndex > token.length()) ? -1 : tokenIndex;
        this.line = line;
        this.column = column;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();
        if (reason != null) {
            builder.append(reason);
            builder.append('\n');
        }

        if (token != null) {
            builder.append('\t');
            builder.append(token);
            builder.append('\n');

            builder.append('\t');
            for (int i = 0; i < token.length(); i++) {
                if (i != tokenIndex) builder.append('~');
                else builder.append('^');
            }

            builder.append('\n');
        }

        if (line >= 1) {
            builder.append("line ");
            builder.append(line);
        }

        if (column >= 1) {
            if (line >= 1) builder.append(' ');
            builder.append("column ");
            builder.append(column);
        }

        return builder.toString();
    }

    /**
     * The error reason.
     * @return the error reason, or null if non was provided
     */
    public String getDescription() {
        return reason;
    }

    /**
     * The offending token. May be null if there is no particular malformed token.
     *
     * @return the out of place or otherwise invalid token; or null if there is no such token associated with this
     * exception
     */
    public String getToken() {
        return token;
    }

    /**
     * The index within the token string ({@link ButyleneParseException#getToken()}) where the problem occurred. -1 if
     * {@code getToken} is null, or if there is no specific index where the token is invalid.
     *
     * @return an index into the token string; or -1 if null or no specific location within the token string is invalid
     */
    public int getTokenIndex() {
        return tokenIndex;
    }

    /**
     * The line where the problem occurred. Negative if there is no specific line. Lines start at 1.
     * @return the line where the parsing exception occurred
     */
    public int getLine() {
        return line;
    }

    /**
     * The colum where the problem occurred. Negative if there is no specific column. Columns start at 1.
     * @return the column where the parsing exception occurred
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return A new builder for creating or throwing ButyleneParseException instances.
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String reason;
        private String token;
        private int tokenIndex = -1;
        private int line = -1;
        private int column = -1;

        private Builder() { }

        @NotNull Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        @NotNull Builder tokenEnd(@NotNull CharSequence token) {
            this.token = token.toString();
            this.tokenIndex = token.length() - 1;
            return this;
        }

        @NotNull Builder token(@Nullable CharSequence token) {
            this.token = token == null ? null : token.toString();
            return this;
        }

        @NotNull Builder token(@NotNull Tokenizer.Token token, @NotNull CharSequence buffer) {
            this.token = switch (token) {
                case QUOTED_TEXT -> '"' + buffer.toString() + '"';
                case UNQUOTED_TEXT -> buffer.toString();
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

            return this;
        }

        @NotNull Builder tokenIndex(int tokenIndex) {
            this.tokenIndex = tokenIndex;
            return this;
        }

        @NotNull Builder line(int line) {
            this.line = line;
            return this;
        }

        @NotNull Builder column(int column) {
            this.column = column;
            return this;
        }

        @NotNull Builder locationFrom(@NotNull ButyleneReader reader) {
            this.line = reader.getLine();
            this.column = reader.getColumn();
            return this;
        }

        @NotNull Builder locationFrom(@NotNull Tokenizer tokenizer) {
            this.line = tokenizer.tokenLine;
            this.column = tokenizer.tokenColumn;
            return this;
        }

        @NotNull Builder from(@NotNull Parser.DeferredResolve resolve) {
            this.line = resolve.tokenLine();
            this.column = resolve.tokenColumn();
            this.token = (resolve.isReference() ? "*" : ">") + resolve.name();
            return this;
        }

        @NotNull Builder from(@NotNull Tokenizer tokenizer, @NotNull Tokenizer.Token token) {
            token(token, tokenizer.buffer);
            locationFrom(tokenizer);
            return this;
        }

        @NotNull ButyleneParseException build() {
            return new ButyleneParseException(reason, token, tokenIndex, line, column);
        }

        @Contract("-> fail")
        void raise() throws ButyleneParseException {
            throw build();
        }
    }
}