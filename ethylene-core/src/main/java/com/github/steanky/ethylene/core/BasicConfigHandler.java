package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.processor.ConfigLoader;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Basic implementation of {@link ConfigHandler}.
 */
public class BasicConfigHandler implements ConfigHandler {
    private final Map<ConfigKey<?>, ConfigLoader<?>> loaderMap = new HashMap<>();

    @Override
    public @NotNull Future<Void> writeDefaults() {
        return CompletableFuture.allOf(loaderMap.values().stream().map(ConfigLoader::writeDefaultIfAbsent)
                .toArray(CompletableFuture[]::new));
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

        if(loaderMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already registered");
        }

        loaderMap.put(key, loader);
    }

    @Override
    public @NotNull <TData> ConfigLoader<TData> getLoader(@NotNull ConfigKey<TData> key) {
        validatePresentKey(key);

        //noinspection unchecked
        return (ConfigLoader<TData>) loaderMap.get(key);
    }

    @Override
    public @NotNull <TData> Future<TData> loadData(@NotNull ConfigKey<TData> key) {
        validatePresentKey(key);

        //noinspection unchecked
        return ((ConfigLoader<TData>)loaderMap.get(key)).load();
    }

    @Override
    public <TData> @NotNull TData getData(@NotNull ConfigKey<TData> key) throws ConfigProcessException {
        validatePresentKey(key);

        //noinspection unchecked
        return FutureUtils.getAndWrapException(((ConfigLoader<TData>)loaderMap.get(key)).load(),
                ConfigProcessException::new, ConfigProcessException.class);
    }

    private void validatePresentKey(ConfigKey<?> key) {
        Objects.requireNonNull(key);

        if(!loaderMap.containsKey(key)) {
            throw new IllegalArgumentException("No loader registered with key");
        }
    }
}
