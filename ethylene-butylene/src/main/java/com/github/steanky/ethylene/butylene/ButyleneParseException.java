package com.github.steanky.ethylene.butylene;

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

    public ButyleneParseException(String reason, String token, int tokenIndex, int line, int column) {
        this.reason = reason;
        this.token = token;
        this.tokenIndex = token == null ? -1 : tokenIndex;
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

    @Override
    public String toString() {
        return "";
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
     * The line where the problem occurred. -1 if there is no specific line. Lines start at 1.
     * @return the line where the parsing exception occurred
     */
    public int getLine() {
        return line;
    }

    /**
     * The colum where the problem occurred. -1 if there is no specific column. Columns start at 1.
     * @return the column where the parsing exception occurred
     */
    public int getColumn() {
        return column;
    }
}
