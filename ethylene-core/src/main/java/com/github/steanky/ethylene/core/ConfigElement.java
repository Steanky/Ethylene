package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a particular value from a configuration file. Specialized sub-interfaces include {@link ConfigNode} and
 * {@link ConfigList}. A direct implementation is {@link ConfigPrimitive}. This interface specifies methods to easily
 * convert to implementations as needed, which will all throw {@link IllegalStateException} by default.
 */
public interface ConfigElement {
    /**
     * Determines if this ConfigElement represents a {@link ConfigNode}.
     * @return true if {@link ConfigElement#asNode()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNode() {
        return false;
    }

    /**
     * Converts this ConfigElement into a {@link ConfigNode}.
     * @return this element as a ConfigNode object
     * @throws IllegalStateException if this element is not a ConfigNode
     */
    default @NotNull ConfigNode asNode() {
        throw new IllegalStateException("Element may not be converted to ConfigNode");
    }

    /**
     * Determines if this ConfigElement represents a {@link ConfigList}.
     * @return true if {@link ConfigElement#asList()} will succeed without throwing an exception; false otherwise
     */
    default boolean isList() {
        return false;
    }

    /**
     * Converts this ConfigElement into a {@link ConfigList}.
     * @return this element as a ConfigList object
     * @throws IllegalStateException if this element is not a ConfigList
     */
    default @NotNull ConfigList asList() {
        throw new IllegalStateException("Element may not be converted to ConfigArray");
    }

    /**
     * Determines if this ConfigElement represents a container (holds other ConfigElements).
     * @return true if {@link ConfigElement#isNode()} or {@link ConfigElement#isList()} return true, false otherwise
     */
    default boolean isContainer() { return isNode() || isList(); }

    /**
     * Converts this ConfigElement into a {@link ConfigContainer}.
     * @return this element as a ConfigContainer object
     * @throws IllegalStateException if this element is not a ConfigContainer
     */
    default @NotNull ConfigContainer asContainer() {
        throw new IllegalStateException("Element may not be converted to ConfigContainer");
    }

    /**
     * Determines if this ConfigElement represents a string.
     * @return true if {@link ConfigElement#asString()} will succeed without throwing an exception; false otherwise
     */
    default boolean isString() {
        return false;
    }

    /**
     * Converts this ConfigElement into a string.
     * @return this element as a string
     * @throws IllegalStateException if this element is not a {@link ConfigPrimitive} containing a string
     */
    default @NotNull String asString() {
        throw new IllegalStateException("Element may not be converted to String");
    }

    /**
     * Determines if this ConfigElement represents a Number.
     * @return true if {@link ConfigElement#asNumber()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * Converts this ConfigElement into a Number.
     * @return this element as a Number
     * @throws IllegalStateException if this element cannot be converted into a Number
     */
    default @NotNull Number asNumber() {
        throw new IllegalStateException("Element may not be converted to Number");
    }

    /**
     * Determines if this ConfigElement represents a boolean.
     * @return true if {@link ConfigElement#asBoolean()} will succeed without throwing an exception; false otherwise
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Converts this ConfigElement into a boolean.
     * @return this element as a boolean
     * @throws IllegalStateException if this element cannot be converted into a boolean
     */
    default boolean asBoolean() {
        throw new IllegalStateException("Element may not be converted to boolean");
    }

    /**
     * Determines if this ConfigElement represents a null value.
     * @return true if this ConfigElement represents null, false otherwise
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Determines if this ConfigElement represents an object. This is true for {@link ConfigPrimitive} and should be
     * true for specialized, direct implementations of this interface that do not, themselves, hold on to ConfigElement
     * instances. It should be false for {@link ConfigNode} and {@link ConfigList}.
     * @return true if {@link ConfigElement#asScalar()} will succeed without throwing an exception; false otherwise
     */
    default boolean isScalar() {
        return false;
    }

    /**
     * Converts this ConfigElement into the <i>scalar</i> Java type it represents. Scalar types are types that cannot
     * themselves contain additional ConfigElements. In Ethylene Core, the only scalar ConfigElement implementation is
     * {@link ConfigPrimitive}.
     * @return this element as an object
     * @throws IllegalStateException if this element cannot be converted into an object
     */
    default Object asScalar() { throw new IllegalStateException("Element may not be converted to Object"); }

    /**
     * Obtains a child ConfigElement from this one by following the specified path. Path objects may be either string
     * keys (corresponding to {@link ConfigNode}s) or integers (for accessing {@link ConfigList}s). Other types will
     * result in an {@link IllegalArgumentException}. If the given array is empty, this object will be returned. The
     * path may contain a mix of integers and strings, so long as each ConfigElement at that point in the path matches.
     * @param path the path used to access this element
     * @return the ConfigElement at the specified path, or null if it could not be found. If the path array is empty,
     * this element will be returned
     * @throws IllegalArgumentException if path contains any objects other than strings and integers, or has null values
     * @throws NullPointerException if path is null
     */
    default ConfigElement getElement(@NotNull Object... path) {
        Objects.requireNonNull(path);

        if(path.length == 0) {
            return this;
        }
        else {
            //validate the path first
            for(Object key : path) {
                if(!(key instanceof String || key instanceof Integer)) {
                    throw new IllegalArgumentException("Invalid key type: " + key.getClass().getName());
                }
            }

            ConfigElement current = this;
            boolean currentNonContainer = !current.isContainer();

            for(Object key : path) {
                if(currentNonContainer) {
                    return null;
                }

                if(current.isNode()) {
                    if(key instanceof String string) {
                        ConfigNode currentNode = current.asNode();
                        if(currentNode.containsKey(string)) {
                            current = currentNode.get(string);
                            continue;
                        }
                    }

                    return null;
                }
                else if(current.isList()) {
                    if(key instanceof Integer integer) {
                        ConfigList currentList = current.asList();
                        if(integer >= 0 && integer < currentList.size()) {
                            current = currentList.get(integer);
                            continue;
                        }
                    }

                    return null;
                }
                else {
                    currentNonContainer = true;
                }
            }

            return current;
        }
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default @NotNull ConfigElement getElementOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, element -> true, Function.identity(), path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param elementSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigElement getElementOrDefault(@NotNull Supplier<ConfigElement> elementSupplier,
                                              @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, elementSupplier, element -> true, Function.identity(),
                path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultElement the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigElement getElementOrDefault(ConfigElement defaultElement, @NotNull Object ... path) {
        return getElementOrDefault(() -> defaultElement, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default boolean getBooleanOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isBoolean, ConfigElement::asBoolean, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param booleanSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default boolean getBooleanOrDefault(@NotNull Supplier<Boolean> booleanSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, booleanSupplier, ConfigElement::isBoolean,
                ConfigElement::asBoolean, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultBoolean the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default boolean getBooleanOrDefault(boolean defaultBoolean, @NotNull Object ... path) {
        return getBooleanOrDefault(() -> defaultBoolean, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default @NotNull Number getNumberOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isNumber, ConfigElement::asNumber, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param numberSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default Number getNumberOrDefault(@NotNull Supplier<Number> numberSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, numberSupplier, ConfigElement::isNumber,
                ConfigElement::asNumber, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultNumber the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default Number getNumberOrDefault(Number defaultNumber, @NotNull Object ... path) {
        return getNumberOrDefault(() -> defaultNumber, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default @NotNull String getStringOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isString, ConfigElement::asString, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param stringSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default String getStringOrDefault(@NotNull Supplier<String> stringSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, stringSupplier, ConfigElement::isString,
                ConfigElement::asString, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultString the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default String getStringOrDefault(String defaultString, @NotNull Object ... path) {
        return getStringOrDefault(() -> defaultString, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default @NotNull ConfigList getListOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isList, ConfigElement::asList, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param listSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigList getListOrDefault(@NotNull Supplier<ConfigList> listSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, listSupplier, ConfigElement::isList, ConfigElement::asList,
                path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultList the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigList getListOrDefault(ConfigList defaultList, @NotNull Object ... path) {
        return getListOrDefault(() -> defaultList, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default @NotNull ConfigNode getNodeOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isNode, ConfigElement::asNode, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param nodeSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigNode getNodeOrDefault(@NotNull Supplier<ConfigNode> nodeSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, nodeSupplier, ConfigElement::isNode, ConfigElement::asNode,
                path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultNode the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default ConfigNode getNodeOrDefault(ConfigNode defaultNode, @NotNull Object ... path) {
        return getNodeOrDefault(() -> defaultNode, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but throws an informative {@link ConfigProcessException}
     * if the path is invalid, or the value pointed to by the path is not the right type.
     * @param path the object path
     * @return the value located at the path
     * @throws ConfigProcessException if the path or element type is invalid
     */
    default Object getObjectOrThrow(@NotNull Object... path) throws ConfigProcessException {
        return ConfigElementHelper.getOrThrow(this, ConfigElement::isScalar, ConfigElement::asScalar, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param objectSupplier the supplier used to produce the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default Object getObjectOrDefault(@NotNull Supplier<Object> objectSupplier, @NotNull Object ... path) {
        return ConfigElementHelper.getOrDefault(this, objectSupplier, ConfigElement::isScalar,
                ConfigElement::asScalar, path);
    }

    /**
     * Works like {@link ConfigElement#getElement(Object...)}, but returns a default value if the path is invalid, or
     * the value pointed to by the path is not the right type.
     * @param defaultObject the default value
     * @param path the object path
     * @return the value located at the path, or the default value
     */
    default Object getObjectOrDefault(Object defaultObject, @NotNull Object ... path) {
        return getObjectOrDefault(() -> defaultObject, path);
    }
}