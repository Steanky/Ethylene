package com.github.steanky.ethylene.butylene;

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
    ButyleneParseException(@Nullable String reason, @Nullable String token, int tokenIndex, int line, int column) {
        this.reason = reason;
        this.token = token;
        this.tokenIndex = (token == null || tokenIndex < 0 || tokenIndex > token.length()) ? -1 : tokenIndex;
        this.line = line;
        this.column = column;
    }

    /**
     * Convenience override for {@link ButyleneParseException#ButyleneParseException(String, String, int, int, int)}.
     *
     * @param reason the reason why the Butylene data is invalid; may be null if no reason is given
     * @param line the line at which the error occurred; negative if no specific line is invalid
     * @param column the column of the start of the invalid token; negative if no specific column is invalid
     */
    ButyleneParseException(String reason, int line, int column) {
        this(reason, null, -1, line, column);
    }

    /**
     * Convenience override for {@link ButyleneParseException#ButyleneParseException(String, String, int, int, int)}.
     *
     * @param reason the reason why the Butylene data is invalid; may be null if no reason is given
     */
    ButyleneParseException(String reason) {
        this(reason, null, -1, -1, -1);
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
}
