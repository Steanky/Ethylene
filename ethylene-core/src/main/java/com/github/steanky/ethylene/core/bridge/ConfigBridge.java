package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Reads and writes {@link ConfigElement} objects to and from an implementation-defined in. This in may be
 * the network, file IO, memory, a database, etc. Synchronicity is also implementation-specific:
 * {@link CompletableFuture} is used to provide a common interface between synchronous and non-synchronous usage.</p>
 *
 * <p>For consistent behavior between synchronous and non-synchronous implementations, simply calling
 * {@link ConfigBridge#read()} or {@link ConfigBridge#write(ConfigElement)} should not throw any {@link IOException}s,
 * even if one occurred. This exception will be raised when {@link CompletableFuture#get()} is invoked later.</p>
 */
public interface ConfigBridge {
    /**
     * Loads a {@link ConfigElement} object from this loader's source. Asynchronous implementations may choose to load
     * ConfigElement objects on another thread; in which case this method should return immediately.
     *
     * @return a {@link CompletableFuture} object which will contain a ConfigElement object when it has finished
     * loading, and can be used to query or await the completion of the read task
     */
    @NotNull CompletableFuture<ConfigElement> read();

    /**
     * Writes a {@link ConfigElement} object to this loader's source. This operation may occur asynchronously in some
     * implementations, in which case this method should return immediately.
     *
     * @param element the element to write to the in
     * @return a {@link CompletableFuture} object, which may be used to query or await the completion of the write
     * operation. {@link CompletableFuture#get()} will return null for the returned value.
     * @throws NullPointerException if element is null
     */
    @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element);
}