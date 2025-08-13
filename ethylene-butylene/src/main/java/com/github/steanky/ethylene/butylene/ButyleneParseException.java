package com.github.steanky.ethylene.butylene;

import java.io.IOException;

public class ButyleneParseException extends IOException {
    private final String reason;
    private final String token;
    private final int tokenIndex;
    private final int line;
    private final int column;

    public ButyleneParseException(String reason, String token, int tokenIndex, int line, int column) {
        this.reason = reason;
        this.token = token;
        this.tokenIndex = tokenIndex;
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
}
