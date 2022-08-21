package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.processor.ConfigLoader;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * General interface used to manage multiple {@link ConfigLoader} instances.
 */
public interface ConfigHandler {
    /**
     * Simple data object used as a key for {@link ConfigLoader} classes.
     * @param <TData> the type of data object returned by the loader
     */
    class ConfigKey<TData> {
        private final String keyString;

        /**
         * Creates a new ConfigKey instance from the provided class and name.
         * @param dataClass the type of data
         * @param name the key name
         * @throws NullPointerException if any of the arguments are null
         */
        public ConfigKey(@NotNull Class<TData> dataClass, @NotNull String name) {
            this.keyString = Objects.requireNonNull(dataClass).getName() + ':' + Objects.requireNonNull(name);
        }

        @Override
        public int hashCode() {
            return keyString.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof ConfigKey key) {
                return keyString.equals(key.keyString);
            }

            return false;
        }

        @Override
        public String toString() {
            return keyString;
        }
    }

    /**
     * Calls {@link ConfigLoader#writeDefaultIfAbsent()} on all managed {@link ConfigLoader} instances.
     * @return a {@link CompletableFuture} object which may be used to await this operation
     */
    @NotNull CompletableFuture<Void> writeDefaults();

    /**
     * Calls {@link ConfigLoader#writeDefaultIfAbsent()}. This method will only return when all of these calls have
     * completed.
     * @throws ConfigProcessException if an error occurs during writing
     */
    void writeDefaultsAndGet() throws ConfigProcessException;

    /**
     * Determines if this ConfigHandler has a loader registered under the given {@link ConfigKey}.
     * @param key the key to check for
     * @return true if the key is associated with a {@link ConfigLoader}, false otherwise.
     * @throws NullPointerException if the argument is null
     */
    boolean hasLoader(@NotNull ConfigKey<?> key);

    /**
     * Registers a loader with this ConfigHandler.
     * @param key the key associated with the loader
     * @param loader the loader to register
     * @param <TData> the type of data the ConfigLoader returns
     * @throws IllegalArgumentException if the given key is already associated with a loader
     * @throws NullPointerException if any of the arguments are null
     */
    <TData> void registerLoader(@NotNull ConfigKey<TData> key, @NotNull ConfigLoader<TData> loader);

    /**
     * Removes the loader associated with this key.
     * @param key the key associated with the loader
     * @param <TData> the type of data returned by the loader
     * @return true if a loader was removed, false otherwise
     */
    <TData> boolean removeLoader(@NotNull ConfigKey<TData> key);

    /**
     * Retrieves the {@link ConfigLoader} instances associated with the provided key.
     * @param key the ConfigKey object associated with a loader
     * @param <TData> the type of data returned by the loader
     * @return a ConfigLoader object associated with the given key
     * @throws IllegalArgumentException if the given key has no loader associated with it
     * @throws NullPointerException if the argument is null
     */
    <TData> @NotNull ConfigLoader<TData> getLoader(@NotNull ConfigKey<TData> key);

    /**
     * Retrieves a {@link CompletableFuture} object representing the result of loading some data from the associated
     * {@link ConfigKey}.
     * @param key the key used to retrieve the {@link ConfigLoader}, on which {@link ConfigLoader#load()}
     * @param <TData> the type of data returned by the loader
     * @return a CompletableFuture object representing the result of loading some data from the associated ConfigKey
     * @throws IllegalArgumentException if there is no loader associated with the given key
     * @throws NullPointerException if the argument is null
     */
    <TData> @NotNull CompletableFuture<TData> loadData(@NotNull ConfigKey<TData> key);

    /**
     * Fetches some data from the {@link ConfigLoader} associated with the {@link ConfigKey}. This method will block
     * until the operation completes.
     * @param key the key used to retrieve the ConfigLoader
     * @param <TData> the type of data returned by the ConfigLoader
     * @return the data itself, obtained by calling {@link Future#get()} on the Future returned by
     * {@link ConfigLoader#load()}
     * @throws ConfigProcessException if an exception occurs when loading the data
     * @throws IllegalArgumentException if there is no loader associated with the given key
     * @throws NullPointerException if the argument is null
     */
    <TData> @NotNull TData loadDataNow(@NotNull ConfigKey<TData> key) throws ConfigProcessException;

    /**
     * Writes some data to a configuration. This method may or may not block, depending on if asynchronous operations
     * are supported by the underlying {@link ConfigLoader}.
     * @param key the key used to retrieve the ConfigLoader
     * @param data the data object to write
     * @return a {@link CompletableFuture} object which may be used to await the write operation
     * @param <TData> the type of data to write
     */
    <TData> @NotNull CompletableFuture<Void> writeData(@NotNull ConfigKey<TData> key, @NotNull TData data);

    /**
     * Writes some data to a configuration. This method will block until the operation is complete.
     * @param key  the key used to retrieve the ConfigLoader
     * @param data the data object to write
     * @param <TData> the type of data to write
     * @throws ConfigProcessException if an error occurs when converting or writing data
     */
    <TData> void writeDataNow(@NotNull ConfigKey<TData> key, @NotNull TData data) throws ConfigProcessException;
}
