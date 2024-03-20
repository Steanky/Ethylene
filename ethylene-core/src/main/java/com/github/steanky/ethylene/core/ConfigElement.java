package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.path.ConfigPath;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.propylene.Parser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.steanky.ethylene.core.processor.ConfigProcessor;

/**
 * Represents a particular value from configuration. Specialized sub-interfaces include {@link ConfigNode} and
 * {@link ConfigList}. A direct implementation is {@link ConfigPrimitive}. This interface specifies methods to easily
 * convert to implementations as needed.
 * <p>
 * Instances can be directly obtained using {@link ConfigElement#of(String)}. Specialized subclasses will often have
 * their own constructors or static methods for creating instances.
 */
public interface ConfigElement {
    /**
     * Constructs a new {@link ConfigElement} using the Propylene format. This method is intended for conveniently
     * creating simple to moderately complex ConfigElement instances directly in code. A few examples:
     * <ul>
     *     <li>{@code ConfigElement.of("['test', 0, 10L]") // a ConfigList containing the string "test", the integer 0, and the long 10}</li>
     *     <li>{@code ConfigElement.of("&0[&0]") // a ConfigList containing itself}</li>
     *     <li>{@code ConfigElement.of("0") // a ConfigPrimitive for the integer 0}</li>
     *     <li>{@code ConfigElement.of("null") // a ConfigPrimitive representing the null value}</li>
     *     <li>{@code ConfigElement.of("TRUE") // a ConfigPrimitive representing true}</li>
     *     <li>{@code ConfigElement.of("{a='b', c='d'}") // a ConfigNode where 'a' is mapped to the string "b", and 'c' is mapped to string "d"}</li>
     * </ul>
     * Although Propylene is lightweight and parses fairly quickly, creating ConfigElement instances this way will never
     * be as fast as directly instantiating them.
     * @param input the Propylene-formatted string to convert into a ConfigElement
     * @return a ConfigElement representing the string
     * @throws RuntimeException wrapping an {@link IOException} containing details of the syntax error
     */
    static @NotNull ConfigElement of(@NotNull String input) {
        try {
            return Parser.fromString(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines if this ConfigElement represents a null value.
     *
     * @return true if this ConfigElement represents null, false otherwise
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Returns the type of this ConfigElement. The value of this method must correspond to the results of
     * {@link ConfigElement#isScalar()}, {@link ConfigElement#isList()}, and {@link ConfigElement#isNode()}:
     *
     * <ul>
     *     <li>{@link ElementType#SCALAR} iff {@link ConfigElement#isScalar()}</li>
     *     <li>{@link ElementType#NODE} iff {@link ConfigElement#isNode()}</li>
     *      <li>{@link ElementType#LIST} iff {@link ConfigElement#isList()}</li>
     * </ul>
     * <p>
     * The corresponding conversion methods (and their {@code asXOrThrow} variants) must follow these rules:
     * <ul>
     *     <li>{@link ConfigElement#asScalar()} does not throw iff {@link ConfigElement#isScalar()}</li>
     *     <li>{@link ConfigElement#asNode()} does not throw iff {@link ConfigElement#isNode()}</li>
     *     <li>{@link ConfigElement#asList()} does not throw iff {@link ConfigElement#isList()}</li>
     * </ul>
     *
     * @return the type of this ConfigElement
     */
    @NotNull ElementType type();

    /**
     * Convenience method; equivalent to {@code at(ConfigPath.of(path))}.
     *
     * @param path the path string, interpreted as if by {@link ConfigPath#of(String)}
     * @return the ConfigElement at the path, or null if it does not exist
     */
    default ConfigElement at(@NotNull String path) {
        return at(ConfigPath.of(path));
    }

    /**
     * Gets a ConfigElement by following the given {@link ConfigPath}. If one or more path elements are not present,
     * {@code null} is returned. If any elements along the path are lists, they are indexed into by converting the
     * corresponding path elements to a list.
     * <p>
     * This element is considered the root of the path. Therefore, if the path contains any PREVIOUS commands
     * ({@code ..}), {@code null} will be returned.
     * <p>
     * Scalar ConfigElements have no children, therefore, any path other than {@code .} ({@link ConfigPath#CURRENT}) or
     * the empty path {@link ConfigPath#EMPTY} will cause this function to return {@code null}.
     *
     * @param path the path to follow
     * @return the ConfigElement at the path, or null if no such element exists
     * @see ConfigPath#of(String)
     */
    default ConfigElement at(@NotNull ConfigPath path) {
        Objects.requireNonNull(path);

        if (path.equals(ConfigPath.EMPTY) || path.equals(ConfigPath.CURRENT)) {
            return this;
        }

        List<ConfigPath.Node> nodes = path.nodes();
        if (nodes.get(0).nodeType() == ConfigPath.NodeType.PREVIOUS) {
            return null;
        }

        ConfigElement current = this;
        for (ConfigPath.Node node : nodes) {
            if (current == null) {
                return null;
            }

            if (node.nodeType() == ConfigPath.NodeType.CURRENT) {
                continue;
            }

            String name = node.name();
            if (current.isNode()) {
                current = current.asNode().get(name);
            }
            else if (current.isList()) {
                int value;
                try {
                    value = Integer.parseInt(name);
                }
                catch (NumberFormatException ignored) {
                    current = null;
                    continue;
                }

                ConfigList list = current.asList();
                if (value >= 0 && value < list.size()) {
                    current = list.get(value);
                    continue;
                }

                current = null;
            }
            else {
                current = null;
            }
        }

        return current;
    }

    /**
     * Convenience method, equivalent to {@link ConfigElement#atOrThrow(ConfigPath)} but parses the given string as if
     * by calling {@link ConfigPath#of(String)}.
     *
     * @param path the path string
     * @return the element at the given path
     * @throws ConfigProcessException if the element does not exist
     */
    default @NotNull ConfigElement atOrThrow(@NotNull String path) throws ConfigProcessException {
        return atOrThrow(ConfigPath.of(path));
    }

    /**
     * Works identically to {@link ConfigElement#at(ConfigPath)}, but throws a {@link ConfigProcessException} if the
     * path does not exist. Useful when writing {@link ConfigProcessor} implementations, when it is desirable to fail if
     * some entry cannot be found.
     *
     * @param path the path to search for an element along
     * @return the ConfigElement at the path
     * @throws ConfigProcessException if there is no element at the given path
     */
    default @NotNull ConfigElement atOrThrow(@NotNull ConfigPath path) throws ConfigProcessException {
        ConfigElement element = at(path);
        if (element == null) {
            throw new ConfigProcessException("No element at " + path);
        }

        return element;
    }

    /**
     * Convenience method, equivalent to {@link ConfigElement#atOrDefault(ConfigPath, Supplier)} but parses the given
     * string as if by calling {@link ConfigPath#of(String)}.
     *
     * @param path the path string
     * @param defaultElementSupplier the supplier of default values
     * @return the element at the given path, or the default value
     */
    default @NotNull ConfigElement atOrDefault(@NotNull String path,
        @NotNull Supplier<? extends @NotNull ConfigElement> defaultElementSupplier) {
        return atOrDefault(ConfigPath.of(path), defaultElementSupplier);
    }

    /**
     * Equivalent to {@link ConfigElement#atOrDefault(ConfigPath, ConfigElement)}, but calls the given {@link Supplier}
     * to generate the default value.
     *
     * @param path the path
     * @param defaultElementSupplier the default value supplier; must return a non-null value
     * @return the ConfigElement present at the path, or the generated non-null default if it does not exist
     */
    default @NotNull ConfigElement atOrDefault(@NotNull ConfigPath path,
        @NotNull Supplier<? extends @NotNull ConfigElement> defaultElementSupplier) {
        return Objects.requireNonNullElseGet(at(path), defaultElementSupplier);
    }

    /**
     * Convenience method, equivalent to {@link ConfigElement#atOrDefault(ConfigPath, ConfigElement)} but parses the
     * given string as if by calling {@link ConfigPath#of(String)}.
     *
     * @param path the path string
     * @param defaultElement the default element
     * @return the element at the given path, or the default value if it does not exist
     */
    default @NotNull ConfigElement atOrDefault(@NotNull String path, @NotNull ConfigElement defaultElement) {
        return atOrDefault(ConfigPath.of(path), defaultElement);
    }

    /**
     * Equivalent to {@link ConfigElement#at(ConfigPath)}, but returns {@code defaultElement} iff there is no element
     * at the path.
     *
     * @param path the path
     * @param defaultElement the default element
     * @return the ConfigElement present at the path, or the non-null default if it does not exist
     */
    default @NotNull ConfigElement atOrDefault(@NotNull ConfigPath path, @NotNull ConfigElement defaultElement) {
        return Objects.requireNonNullElse(at(path), defaultElement);
    }

    /**
     * Determines if this ConfigElement represents a container (holds other ConfigElements).
     *
     * @return true if {@link ConfigElement#isNode()} or {@link ConfigElement#isList()} return true, false otherwise
     */
    default boolean isContainer() {
        return isNode() || isList();
    }

    /**
     * Converts this ConfigElement into a {@link ConfigContainer}.
     *
     * @return this element as a ConfigContainer object
     * @throws IllegalStateException if this element is not a ConfigContainer
     */
    default @NotNull ConfigContainer asContainer() {
        throw new IllegalStateException("Element may not be converted to ConfigContainer");
    }

    /**
     * Converts this ConfigElement into a {@link ConfigContainer}. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a ConfigContainer object
     * @throws ConfigProcessException if this element is not a container
     */
    default @NotNull ConfigContainer asContainerOrThrow() throws ConfigProcessException {
        if (isContainer()) {
            return asContainer();
        }

        throw new ConfigProcessException("This ConfigElement is not a container");
    }

    /**
     * Determines if this ConfigElement represents a {@link ConfigNode}.
     *
     * @return true if {@link ConfigElement#asNode()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNode() {
        return type().isNode();
    }

    /**
     * Converts this ConfigElement into a {@link ConfigNode}.
     *
     * @return this element as a ConfigNode object
     * @throws IllegalStateException if this element is not a ConfigNode
     */
    default @NotNull ConfigNode asNode() {
        throw new IllegalStateException("Element may not be converted to ConfigNode");
    }

    /**
     * Converts this ConfigElement into a {@link ConfigNode}. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a ConfigNode object
     * @throws ConfigProcessException if this element is not a node
     */
    default @NotNull ConfigNode asNodeOrThrow() throws ConfigProcessException {
        if (isNode()) {
            return asNode();
        }

        throw new ConfigProcessException("This ConfigElement is not a node");
    }

    /**
     * Determines if this ConfigElement represents a {@link ConfigList}.
     *
     * @return true if {@link ConfigElement#asList()} will succeed without throwing an exception; false otherwise
     */
    default boolean isList() {
        return type().isList();
    }

    /**
     * Converts this ConfigElement into a {@link ConfigList}.
     *
     * @return this element as a ConfigList object
     * @throws IllegalStateException if this element is not a ConfigList
     */
    default @NotNull ConfigList asList() {
        throw new IllegalStateException("Element may not be converted to ConfigArray");
    }

    /**
     * Converts this ConfigElement into a {@link ConfigList}. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a ConfigList object
     * @throws ConfigProcessException if this element is not a list
     */
    default @NotNull ConfigList asListOrThrow() throws ConfigProcessException {
        if (isList()) {
            return asList();
        }

        throw new ConfigProcessException("This ConfigElement is not a list");
    }

    /**
     * Determines if this ConfigElement represents a boolean.
     *
     * @return true if {@link ConfigElement#asBoolean()} will succeed without throwing an exception; false otherwise
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Converts this ConfigElement into a boolean.
     *
     * @return this element as a boolean
     * @throws IllegalStateException if this element cannot be converted into a boolean
     */
    default boolean asBoolean() {
        throw new IllegalStateException("Element may not be converted to boolean");
    }

    /**
     * Converts this ConfigElement into a boolean. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a boolean
     * @throws ConfigProcessException if this element is not a boolean
     */
    default boolean asBooleanOrThrow() throws ConfigProcessException {
        if (isBoolean()) {
            return asBoolean();
        }

        throw new ConfigProcessException("This ConfigElement is not a boolean");
    }

    /**
     * Determines if this ConfigElement represents a Number.
     *
     * @return true if {@link ConfigElement#asNumber()} will succeed without throwing an exception; false otherwise
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * Converts this ConfigElement into a Number.
     *
     * @return this element as a Number
     * @throws IllegalStateException if this element cannot be converted into a Number
     */
    default @NotNull Number asNumber() {
        throw new IllegalStateException("Element may not be converted to Number");
    }

    /**
     * Converts this ConfigElement into a {@link Number}. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a Number
     * @throws ConfigProcessException if this element is not a number
     */
    default @NotNull Number asNumberOrThrow() throws ConfigProcessException {
        if (isNumber()) {
            return asNumber();
        }

        throw new ConfigProcessException("This ConfigElement is not a number");
    }

    /**
     * Determines if this ConfigElement represents a string.
     *
     * @return true if {@link ConfigElement#asString()} will succeed without throwing an exception; false otherwise
     */
    default boolean isString() {
        return false;
    }

    /**
     * Converts this ConfigElement into a string.
     *
     * @return this element as a string
     * @throws IllegalStateException if this element is not a {@link ConfigPrimitive} containing a string
     */
    default @NotNull String asString() {
        throw new IllegalStateException("Element may not be converted to String");
    }

    /**
     * Converts this ConfigElement into a string. Useful for making {@link ConfigProcessor}
     * implementations, when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a string
     * @throws ConfigProcessException if this element is not a string
     */
    default @NotNull String asStringOrThrow() throws ConfigProcessException {
        if (isString()) {
            return asString();
        }

        throw new ConfigProcessException("This ConfigElement is not a string");
    }

    /**
     * Determines if this ConfigElement represents an object. This is true for {@link ConfigPrimitive} and should be
     * true for specialized, direct implementations of this interface that do not, themselves, hold on to ConfigElement
     * instances. It should be false for {@link ConfigNode} and {@link ConfigList}.
     *
     * @return true if {@link ConfigElement#asScalar()} will succeed without throwing an exception; false otherwise
     */
    default boolean isScalar() {
        return type().isScalar();
    }

    /**
     * Converts this ConfigElement into the <i>scalar</i> Java type it represents. Scalar types are types that cannot
     * themselves contain additional ConfigElements. In Ethylene Core, the only scalar ConfigElement implementation is
     * {@link ConfigPrimitive}. Other modules may add scalar types specific to certain formats.
     *
     * @return this element as an object
     * @throws IllegalStateException if this element cannot be converted into an object
     */
    default Object asScalar() {
        throw new IllegalStateException("Element may not be converted to Object");
    }

    /**
     * Converts this ConfigElement into a scalar object. Useful for making {@link ConfigProcessor} implementations,
     * when it is desirable to throw an exception if this element is the wrong type.
     *
     * @return this element as a scalar
     * @throws ConfigProcessException if this element is not a scalar
     */
    default Object asScalarOrThrow() throws ConfigProcessException {
        if (isScalar()) {
            return asScalar();
        }

        throw new ConfigProcessException("This ConfigElement is not a scalar");
    }
}