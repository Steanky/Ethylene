package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Reads and writes {@link ConfigElement} objects to and from an implementation-defined source. This source may be the
 * network, file IO, memory, a database, etc. Synchronicity is also implementation-specific: {@link Future} is used to
 * provide a common interface between synchronous and non-synchronous usage.
 */
public interface ConfigBridge {
    /**
     * Loads a {@link ConfigElement} object from this loader's source. Asynchronous implementations may choose to load
     * ConfigElement objects on another thread; in which case this method should return immediately.
     * @return a {@link Future} object which will contain a ConfigElement object when it has finished loading, and can
     * be used to query or await the completion of the read task
     * @throws IOException if an IO error occurs
     */
    @NotNull Future<ConfigElement> read() throws IOException;

    /**
     * Writes a {@link ConfigElement} object to this loader's source. This operation may occur asynchronously in some
     * implementations, in which case this method should return immediately.
     * @param element the element to write to the source
     * @return a {@link Future} object, which may be used to query or await the completion of the write task
     * @throws NullPointerException if element is null
     * @throws IOException if an IO error occurs
     */
    @NotNull Future<Void> write(@NotNull ConfigElement element) throws IOException;
}