package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A scalar signature capable of converting ConfigElements to ConfigElements. This does not perform any copying. This
 * class is not part of the public API.
 * @param <T> the type of ConfigElement
 */
class ConfigElementSignature<T extends ConfigElement> extends PrioritizedBase implements ScalarSignature<T> {
    /**
     * Signature for {@link ConfigElement}.
     */
    static final ScalarSignature<ConfigElement> CONFIG_ELEMENT = new ConfigElementSignature<>(
        Token.ofClass(ConfigElement.class));

    /**
     * Signature for {@link ConfigContainer}.
     */
    static final ScalarSignature<ConfigContainer> CONFIG_CONTAINER = new ConfigElementSignature<>(
        Token.ofClass(ConfigContainer.class));

    /**
     * Signature for {@link ConfigNode}.
     */
    static final ScalarSignature<ConfigNode> CONFIG_NODE = new ConfigElementSignature<>(
        Token.ofClass(ConfigNode.class));

    /**
     * Signature for {@link ConfigList}.
     */
    static final ScalarSignature<ConfigList> CONFIG_LIST = new ConfigElementSignature<>(
        Token.ofClass(ConfigList.class));

    /**
     * Signature for {@link ConfigPrimitive}.
     */
    static final ScalarSignature<ConfigPrimitive> CONFIG_PRIMITIVE = new ConfigElementSignature<>(
        Token.ofClass(ConfigPrimitive.class));

    private final Token<T> configElementType;

    private ConfigElementSignature(Token<T> configElementType) {
        super(0);
        this.configElementType = Objects.requireNonNull(configElementType);
    }

    @Override
    public @NotNull Token<T> objectType() {
        return configElementType;
    }

    @Override
    public @NotNull ElementType elementType() {
        return ElementType.SCALAR;
    }

    @Override
    public @Nullable T createScalar(@NotNull ConfigElement element) {
        Class<? extends ConfigElement> elementClass = element.getClass();
        if (configElementType.isSuperclassOf(elementClass)) {
            return configElementType.cast(element);
        }

        throw new MapperException("Unable to convert a ConfigElement of type " + elementClass + " to " +
            configElementType);
    }

    @Override
    public @NotNull ConfigElement createElement(@Nullable T t) {
        return t == null ? ConfigPrimitive.NULL : t;
    }
}
