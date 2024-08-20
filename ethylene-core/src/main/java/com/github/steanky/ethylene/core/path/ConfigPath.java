package com.github.steanky.ethylene.core.path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents a particular path through a configuration tree. Implementations of this interface should be
 * equality-comparable with all other implementations of this interface. Therefore, equality (and hash code) should be
 * determined using the list returned by {@link ConfigPath#nodes()}. Furthermore, implementations must be immutable;
 * their nodes list should never change throughout their lifetime.
 */
public interface ConfigPath {
    /**
     * The empty path, pointing at the root node.
     */
    ConfigPath EMPTY = BasicConfigPath.EMPTY_PATH;

    /**
     * The current (relative) path {@code .}, pointing at this node.
     */
    ConfigPath CURRENT = BasicConfigPath.RELATIVE_BASE;

    /**
     * The previous (relative) path {@code ..}, pointing at the previous node.
     */
    ConfigPath PREVIOUS = BasicConfigPath.PREVIOUS_BASE;

    /**
     * Parses the given string, assuming UNIX-like formatting and semantics. The resulting path will be normalized such
     * that redundant nodes are removed.
     * <p>
     * Like UNIX paths, path entries are separated by slashes. Unlike UNIX, leading slashes have no significance. All
     * paths that start with {@code .} or {@code ..} are considered "relative". All paths that do not are considered
     * absolute.
     * <p>
     * Backslashes are considered escape characters. They can be used to include slashes, {@code .} and {@code ..} in
     * path entries.
     *
     * @param path the path string to parse
     * @return a new ElementPath
     */
    static @NotNull ConfigPath of(@NotNull String path) {
        Objects.requireNonNull(path);
        return BasicConfigPath.parse(path);
    }

    /**
     * Gets the nodes represented by this path.
     *
     * @return the nodes represented by this path
     */
    @NotNull @Unmodifiable List<Node> nodes();

    /**
     * Determines if this path represents an absolute or a relative path.
     *
     * @return true if this path is absolute; false otherwise
     */
    boolean isAbsolute();

    /**
     * Computes a new path by considering the given relative path as relative to this one. If the given path is not
     * actually relative, it will be returned as-is. Otherwise, the relative path's nodes will be appropriately added to
     * this path's, and a new path will be returned.
     * <p>
     * This method is analogous to {@link Path#resolve(Path)}.
     *
     * @param relativePath the relative path
     * @return a new path representing the combination of this path and another
     */
    @NotNull ConfigPath resolve(@NotNull ConfigPath relativePath);

    /**
     * Convenience overload for {@link ConfigPath#resolve(ConfigPath)} that parses the given string before
     * resolution.
     *
     * @param relativePath the relative path string
     * @return a new path representing the combination of this path and another
     */
    @NotNull ConfigPath resolve(@NotNull String relativePath);

    /**
     * Appends a string to this path as a <i>single</i>, new node. Commands will not be interpreted; the value is used
     * as-is for the name of the node only.
     *
     * @param node the object to append
     * @return a new path
     */
    @NotNull ConfigPath append(@NotNull String node);

    /**
     * Appends a number to this path as a <i>single</i>, new node. Commands will not be interpreted; the value is used
     * as-is for the name of the node only.
     *
     * @param node the index to append
     * @return a new path
     * @throws IllegalArgumentException if {@code node < 0}
     */
    @NotNull ConfigPath append(int node);

    /**
     * Converts this path into an absolute path. Path commands will be removed.
     *
     * @return a new path
     */
    @NotNull ConfigPath toAbsolute();

    /**
     * Gets the parent path. If this path is empty, {@code null} is returned.
     *
     * @return the parent path, or {@code null} if empty
     */
    ConfigPath getParent();

    /**
     * Relativizes this path with respect to another. This is the inverse of {@link ConfigPath#resolve(ConfigPath)},
     * and is analogous to {@link Path#relativize(Path)}.
     *
     * @param other the other path
     * @return the relativized path
     */
    @NotNull ConfigPath relativize(@NotNull ConfigPath other);

    /**
     * Same behavior as {@link ConfigPath#relativize(ConfigPath)}, but interprets the given string as an ElementPath.
     * @param other the other path
     * @return the relativized path
     */
    @NotNull ConfigPath relativize(@NotNull String other);

    /**
     * Analogous operation to {@link Path#resolveSibling(Path)}.
     *
     * @param sibling the sibling path
     * @return a new path
     */
    @NotNull ConfigPath resolveSibling(@NotNull ConfigPath sibling);

    /**
     * Same behavior as {@link ConfigPath#resolveSibling(ConfigPath)}, but interprets the given string as an
     * ElementPath.
     *
     * @param sibling the sibling path
     * @return a new path
     */
    @NotNull ConfigPath resolveSibling(@NotNull String sibling);

    /**
     * Returns a path representing a sub-path of this one. Analogous to {@link Path#subpath(int, int)}.
     *
     * @param beginIndex the starting index (inclusive)
     * @param endIndex the ending index (exclusive)
     * @return a new path
     */
    @NotNull ConfigPath subpath(int beginIndex, int endIndex);

    /**
     * Checks if this path starts with the same entries as the given path.
     *
     * @param otherPath the other path
     * @return true if this path starts with the same entries as {@code startsWith}, false otherwise
     */
    boolean startsWith(@NotNull ConfigPath otherPath);

    /**
     * Equivalent of {@link ConfigPath#startsWith(ConfigPath)}, but interprets the given string as a path.
     * @param otherPath the other path as a string
     * @return true if this path starts with the same entries as {@code startsWith}, false otherwise
     */
    boolean startsWith(@NotNull String otherPath);

    /**
     * Determines if this path is empty.
     *
     * @return true iff this path is empty (the nodes list is empty)
     */
    boolean isEmpty();

    /**
     * The identifier of {@link NodeType#CURRENT}.
     */
    int CURRENT_ID = 0;

    /**
     * The identifier of {@link NodeType#PREVIOUS}.
     */
    int PREVIOUS_ID = 1;

    /**
     * The identifier of {@link NodeType#NAME} and {@link NodeType#INDEX}.
     */
    int NODE_OR_NAME_ID = 2;

    /**
     * Indicates various types of nodes.
     */
    enum NodeType {
        /**
         * A node representing the "current" command.
         */
        CURRENT(CURRENT_ID),

        /**
         * A node representing the "previous" command.
         */
        PREVIOUS(PREVIOUS_ID),

        /**
         * A node representing the name of a particular point along a path.
         */
        NAME(NODE_OR_NAME_ID),

        /**
         * A node representing an index into an array.
         */
        INDEX(NODE_OR_NAME_ID);

        private final int id;

        NodeType(int id) {
            this.id = id;
        }

        /**
         * Checks if this NodeType is {@link NodeType#NAME} or {@link NodeType#INDEX}.
         *
         * @return true if this type is {@code NAME} or {@code INDEX}, false otherwise
         */
        public boolean isNameOrIndex() {
            return this.id == NODE_OR_NAME_ID;
        }

        /**
         * The identifier of this node type. Should be used in place of the ordinal for equality comparisons.
         * {@link NodeType#NAME} and {@link NodeType#INDEX} have the same identifier.
         *
         * @return the id
         */
        public int id() {
            return id;
        }
    }

    /**
     * A record representing an individual node in a path.
     *
     * @param name     the name of the node
     * @param index    the index of the node
     * @param nodeType the kind of node this is
     */
    record Node(@NotNull String name, int index, @NotNull NodeType nodeType) {
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof Node node)) {
                return false;
            }

            return nodeType.id == node.nodeType.id && name.equals(node.name);
        }

        @Override
        public int hashCode() {
            return 31 * (31 + name.hashCode()) + Integer.hashCode(nodeType.id);
        }
    }
}
