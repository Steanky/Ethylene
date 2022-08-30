package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.processor.ConfigLoader;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic implementation of {@link ConfigHandler}.
 */
public class BasicConfigHandler implements ConfigHandler {
    private final Map<ConfigKey<?>, ConfigLoader<?>> loaderMap = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<Void> writeDefaults() {
        return CompletableFuture.allOf(
                loaderMap.values().stream().map(ConfigLoader::writeDefaultIfAbsent).toArray(CompletableFuture[]::new));
    }

    @Override
    public void writeDefaultsAndGet() throws ConfigProcessException {
        FutureUtils.getAndWrapException(writeDefaults(), ConfigProcessException::new, ConfigProcessException.class);
    }

    @Override
    public boolean hasLoader(@NotNull ConfigKey<?> key) {
        Objects.requireNonNull(key);
        return loaderMap.containsKey(key);
    }

    @Override
    public <TData> void registerLoader(@NotNull ConfigKey<TData> key, @NotNull ConfigLoader<TData> loader) {
        Objects.requireNonNull(loader);
        Objects.requireNonNull(key);

        if (loaderMap.putIfAbsent(key, loader) != null) {
            throw new IllegalArgumentException("Key '" + key + "' already registered");
        }
    }

    @Override
    public <TData> boolean removeLoader(@NotNull ConfigKey<TData> key) {
        return loaderMap.remove(key) != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <TData> ConfigLoader<TData> getLoader(@NotNull ConfigKey<TData> key) {
        validatePresentKey(key);
        return (ConfigLoader<TData>) loaderMap.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <TData> CompletableFuture<TData> loadData(@NotNull ConfigKey<TData> key) {
        validatePresentKey(key);
        return ((ConfigLoader<TData>) loaderMap.get(key)).load();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TData> @NotNull TData loadDataNow(@NotNull ConfigKey<TData> key) throws ConfigProcessException {
        validatePresentKey(key);
        return FutureUtils.getAndWrapException(((ConfigLoader<TData>) loaderMap.get(key)).load(),
                ConfigProcessException::new, ConfigProcessException.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TData> @NotNull CompletableFuture<Void> writeData(@NotNull ConfigKey<TData> key, @NotNull TData data) {
        validatePresentKey(key);
        return CompletableFuture.allOf(((ConfigLoader<TData>) loaderMap.get(key)).write(data));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TData> void writeDataNow(@NotNull ConfigKey<TData> key, @NotNull TData data) throws ConfigProcessException {
        validatePresentKey(key);
        FutureUtils.getAndWrapException(((ConfigLoader<TData>) loaderMap.get(key)).write(data),
                ConfigProcessException::new, ConfigProcessException.class);
    }

    private void validatePresentKey(ConfigKey<?> key) {
        Objects.requireNonNull(key);

        if (!loaderMap.containsKey(key)) {
            throw new IllegalArgumentException("No loader registered with key '" + key + "'");
        }
    }
}
