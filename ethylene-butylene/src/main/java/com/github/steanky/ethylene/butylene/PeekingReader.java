package com.github.steanky.ethylene.butylene;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * A wrapper for a {@link Reader} that supports "looking ahead" a single character through its
 * {@link PeekingReader#peekNext()} method.
 * <p>
 * This class is not thread-safe, even if the underlying reader is.
 */
class PeekingReader implements Closeable {
    private final Reader inner;

    private boolean closed;
    private int peekedValue;
    private boolean hasPeeked;

    private int row = 1;
    private int column = 1;

    /**
     * Creates a new PeekingReader backed by <i>inner</i>.
     *
     * @param inner the inner reader
     */
    public PeekingReader(@NotNull Reader inner) {
        this.inner = Objects.requireNonNull(inner);
    }

    private void updatePos(int character) {
        if (character == '\n') {
            this.row++;
            this.column = 1;
        } else if (character != -1) this.column++;
    }

    /**
     * Advances the reader and returns the current value.
     *
     * @return the current character, or -1 to indicate end-of-stream
     * @throws IOException if {@link Reader#read()} throws an exception
     */
    public int next() throws IOException {
        if (hasPeeked) {
            hasPeeked = false;
            updatePos(peekedValue);
            return peekedValue;
        }

        int next = inner.read();
        updatePos(next);
        return next;
    }

    /**
     * Returns the next value <i>without</i> advancing the reader. Calling this method multiple times will return the
     * same value, until the reader is actually advanced using {@link PeekingReader#next()}.
     * <p>
     * Note that this may need to call {@link Reader#read()} on the inner reader.
     *
     * @return the next value, or -1 to indicate end-of-stream
     * @throws IOException if the inner reader throws an exception
     */
    public int peekNext() throws IOException {
        if (hasPeeked) return this.peekedValue;
        this.hasPeeked = true;
        this.peekedValue = inner.read();

        return this.peekedValue;
    }

    public int getLine() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;

        inner.close();
    }
}
