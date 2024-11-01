package com.github.steanky.ethylene.core.path;

import com.github.steanky.toolkit.collection.Containers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.invoke.VarHandle;
import java.util.*;

/**
 * Basic ConfigPath implementation with UNIX-like semantics.
 */
class BasicConfigPath implements ConfigPath {
    private static final int MAX_DIGIT_LENGTH = Integer.toString(Integer.MAX_VALUE).length();

    private static final String CURRENT_COMMAND = ".";
    private static final String PREVIOUS_COMMAND = "..";

    private static final Node CURRENT_NODE = new Node(CURRENT_COMMAND, -1, NodeType.CURRENT);
    private static final Node PREVIOUS_NODE = new Node(PREVIOUS_COMMAND, -1, NodeType.PREVIOUS);

    private static final Node[] EMPTY_NODE_ARRAY = new Node[0];

    static final BasicConfigPath EMPTY_PATH = new BasicConfigPath(EMPTY_NODE_ARRAY);
    static final BasicConfigPath RELATIVE_BASE = new BasicConfigPath(new Node[]{CURRENT_NODE});
    static final BasicConfigPath PREVIOUS_BASE = new BasicConfigPath(new Node[]{PREVIOUS_NODE});

    private static final char DELIMITER = '/';
    private static final char ESCAPE = '\\';

    private static final char CURRENT = '.';

    private final Node[] nodes;
    private final List<Node> nodeView;

    private int hash;
    private boolean hashed;

    private BasicConfigPath(@NotNull Node @NotNull [] nodeArray) {
        this.nodes = nodeArray;
        this.nodeView = Containers.arrayView(nodeArray);
    }

    private static boolean isCharacterEscapable(char current) {
        return current == DELIMITER || current == CURRENT || current == ESCAPE;
    }

    /**
     * Parses the given UNIX-style path string into a {@link BasicConfigPath}. The resulting object will represent the
     * normalized path, with redundant elements removed.
     *
     * @param path the path string
     * @return a normalized BasicElementPath
     */
    static @NotNull BasicConfigPath parse(@NotNull String path) {
        if (path.isEmpty() || path.equals("/")) {
            return EMPTY_PATH;
        }

        if (path.equals(".")) {
            return RELATIVE_BASE;
        }

        if(path.equals("..")) {
            return PREVIOUS_BASE;
        }

        int pathLength = path.length();

        List<Node> nodes = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        boolean escape = false;
        boolean nodeEscape = false;
        for (int i = 0; i < pathLength; i++) {
            char current = path.charAt(i);

            if (escape) {
                if (!isCharacterEscapable(current)) {
                    //re-add the escape character that we previously encountered
                    builder.append(ESCAPE);
                }

                builder.append(current);
                escape = false;
            } else if (current == ESCAPE) {
                escape = true;

                if (builder.isEmpty()) {
                    nodeEscape = true;
                }
            } else if (current == DELIMITER) {
                tryAddNode(nodes, builder, nodeEscape);
                nodeEscape = false;
            } else {
                builder.append(current);
            }
        }

        tryAddNode(nodes, builder, nodeEscape);

        if (nodes.isEmpty()) {
            return BasicConfigPath.EMPTY_PATH;
        }

        //process the path to remove unnecessary or redundant PREVIOUS commands
        //we don't have to worry about redundant CURRENTs because they wouldn't have been added in the first place
        for (int i = nodes.size() - 1; i > 0; i--) {
            Node node = nodes.get(i);
            if (node.nodeType() != NodeType.PREVIOUS) {
                continue;
            }

            int previousIndex = i - 1;
            Node previous = nodes.get(previousIndex);
            NodeType previousType = previous.nodeType();
            if (previousType == NodeType.PREVIOUS) {
                //don't remove the previous node if it's also a previous
                continue;
            }

            //the current PREVIOUS command erased the previous node
            nodes.remove(previousIndex);

            if (previousType.isNameOrIndex()) {
                //strip out redundant PREVIOUS commands, otherwise leave them alone
                nodes.remove(previousIndex);
            }

            if (i > nodes.size()) {
                i--;
            }
        }

        if (nodes.isEmpty()) {
            return EMPTY_PATH;
        }

        return new BasicConfigPath(nodes.toArray(Node[]::new));
    }

    /**
     * A fast check to validate if a string is parseable to a positive number using {@link Integer#parseInt(String)}.
     * If this method returns {@code false}, the number is definitely not parseable using
     * {@link Integer#parseUnsignedInt(String)}. If it returns {@code true}, the number may still (but is unlikely to
     * be) not parseable.
     * <p>
     * This should be substantially faster than checking with a regex, or catching a {@link NumberFormatException}. This
     * is generally worthwhile since it is expected that the vast majority of strings we encounter are <i>not</i> valid
     * integers!
     *
     * @param string the string to check
     * @return true if the string is possibly parseable, false if the string is definitely not parseable
     */
    private static boolean fastValidateNumber(String string) {
        if (string.isEmpty()) {
            return false;
        }

        int unusedLeadingCharacters = 0;

        boolean foundNonLeadingCharacter = false;
        int len = string.length();
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (i == 0 && c == '+') {
                if (len == 1) {
                    return false;
                }

                unusedLeadingCharacters++;
                continue;
            }

            if (!foundNonLeadingCharacter && c == '0') {
                unusedLeadingCharacters++;
                continue;
            }

            if (c >= '0' && c <= '9') {
                if (!foundNonLeadingCharacter && len - unusedLeadingCharacters > MAX_DIGIT_LENGTH) {
                    // fast exit, we just found too many digits
                    return false;
                }

                foundNonLeadingCharacter = true;
                continue;
            }

            return false;
        }

        return true;
    }

    private static int tryParseNumber(String string) {
        if (!fastValidateNumber(string)) {
            return -1;
        }

        try {
            return Integer.parseUnsignedInt(string);
        }
        catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void tryAddNode(List<Node> nodes, StringBuilder builder, boolean escape) {
        if (builder.isEmpty()) {
            return;
        }

        String string = builder.toString();
        if (!nodes.isEmpty() && !escape && string.length() <= 1 && string.charAt(0) == CURRENT) {
            builder.setLength(0);
            return;
        }

        int parseResult = -1;
        NodeType type = escape ? ((parseResult = tryParseNumber(string)) != -1 ? NodeType.INDEX : NodeType.NAME) :
            switch (string) {
            case PREVIOUS_COMMAND -> NodeType.PREVIOUS;
            case CURRENT_COMMAND -> NodeType.CURRENT;
            default -> (parseResult = tryParseNumber(string)) != -1 ? NodeType.INDEX : NodeType.NAME;
        };

        nodes.add(switch (type) {
            case CURRENT -> CURRENT_NODE;
            case PREVIOUS -> PREVIOUS_NODE;
            case INDEX -> new Node(string, parseResult, NodeType.INDEX);
            case NAME -> new Node(string, -1, NodeType.NAME);
        });

        builder.setLength(0);
    }

    @Override
    public @NotNull @Unmodifiable List<Node> nodes() {
        return nodeView;
    }

    @Override
    public boolean isAbsolute() {
        return nodes.length == 0 || nodes[0].nodeType().isNameOrIndex();
    }

    @Override
    public @NotNull ConfigPath resolve(@NotNull ConfigPath relativePath) {
        if (relativePath.isAbsolute()) {
            //mimic behavior of Path#resolve(Path)
            return relativePath;
        }

        if (relativePath.equals(ConfigPath.CURRENT)) {
            return this;
        }

        List<Node> ourNodes = nodes();
        List<Node> relativeNodes = relativePath.nodes();

        Deque<Node> newNodes = new ArrayDeque<>(ourNodes.size() + relativeNodes.size());

        for (Node node : ourNodes) {
            newNodes.addLast(node);
        }

        for (Node node : relativeNodes) {
            switch (node.nodeType()) {
                case NAME, INDEX -> newNodes.addLast(node);
                case CURRENT -> {
                } //no-op
                case PREVIOUS -> { //resolve previous command
                    if (!newNodes.isEmpty() && newNodes.peekLast().nodeType() != NodeType.PREVIOUS) {
                        newNodes.removeLast();
                    }
                    else {
                        newNodes.addLast(node);
                    }
                }
            }
        }

        if (newNodes.isEmpty()) {
            return EMPTY_PATH;
        }

        return new BasicConfigPath(newNodes.toArray(Node[]::new));
    }

    @Override
    public @NotNull ConfigPath resolve(@NotNull String relativePath) {
        return resolve(parse(relativePath));
    }

    @Override
    public @NotNull ConfigPath append(@NotNull String node) {
        Objects.requireNonNull(node);
        int parseResult = tryParseNumber(node);

        Node newNode;
        if (parseResult == -1) {
            newNode = new Node(node, -1, NodeType.NAME);
        }
        else {
            newNode = new Node(node, parseResult, NodeType.INDEX);
        }

        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
        newNodes[newNodes.length - 1] = newNode;
        return new BasicConfigPath(newNodes);
    }

    @Override
    public @NotNull ConfigPath append(int node) {
        if (node < 0) {
            throw new IllegalArgumentException("node index is negative");
        }

        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
        newNodes[newNodes.length - 1] = new Node(Integer.toString(node), node, NodeType.INDEX);
        return new BasicConfigPath(newNodes);
    }

    @Override
    public @NotNull ConfigPath toAbsolute() {
        if (isAbsolute()) {
            return this;
        }

        int i;
        for (i = 0; i < nodes.length; i++) {
            if (nodes[i].nodeType().isNameOrIndex()) {
                break;
            }
        }

        if (i == nodes.length) {
            return EMPTY_PATH;
        }

        Node[] newNodes = new Node[nodes.length - i];
        System.arraycopy(this.nodes, i, newNodes, 0, newNodes.length);
        return new BasicConfigPath(newNodes);
    }

    @Override
    public ConfigPath getParent() {
        if (nodes.length == 0) {
            return null;
        }

        if (nodes.length == 1) {
            return EMPTY_PATH;
        }

        Node[] newNodes = new Node[nodes.length - 1];
        System.arraycopy(nodes, 0, newNodes, 0, newNodes.length);
        return new BasicConfigPath(newNodes);
    }

    @Override
    public @NotNull ConfigPath relativize(@NotNull ConfigPath other) {
        if (this.equals(other)) {
            return RELATIVE_BASE;
        }

        if (this.isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("Can only relativize paths of the same type");
        }

        Node[] otherNodes = nodeArray(other);
        int min = Math.min(otherNodes.length, this.nodes.length);
        int max = Math.max(otherNodes.length, this.nodes.length);

        int matched = matched(this, this.nodes, other, otherNodes, min);

        if (otherNodes.length < this.nodes.length || matched < min) {
            int unmatched = min - matched;
            int previousCount = max - matched;
            Node[] newNodes = new Node[previousCount + unmatched];

            Arrays.fill(newNodes, 0, previousCount, PREVIOUS_NODE);
            System.arraycopy(otherNodes, matched, newNodes, previousCount, unmatched);
            return new BasicConfigPath(newNodes);
        }

        //matched == min && otherNodes.length >= this.nodes.length
        int unmatched = max - min;
        Node[] newNodes = new Node[unmatched + 1];
        newNodes[0] = CURRENT_NODE;
        System.arraycopy(otherNodes, matched, newNodes, 1, unmatched);
        return new BasicConfigPath(newNodes);

    }

    private static int matched(ConfigPath self, Node[] selfNodes, ConfigPath other,
            Node[] otherNodes, int min) {
        int matched = 0;

        //count number of shared elements
        while (matched < min) {
            if (!otherNodes[matched].equals(selfNodes[matched])) {
                break;
            }

            matched++;
        }

        //remaining .. in this path means we have no sane way to resolve the relative path
        for (int i = matched; i < selfNodes.length; i++) {
            if (selfNodes[i].nodeType() == NodeType.PREVIOUS) {
                throw new IllegalArgumentException("Cannot compute relative path from " + self + " to " + other);
            }
        }

        return matched;
    }

    @Override
    public @NotNull ConfigPath relativize(@NotNull String other) {
        return relativize(parse(other));
    }

    @Override
    public @NotNull ConfigPath resolveSibling(@NotNull ConfigPath sibling) {
        if (sibling.isAbsolute()) {
            return sibling;
        }

        ConfigPath parentPath = getParent();
        if (parentPath == null) {
            return sibling;
        }

        return parentPath.resolve(sibling);
    }

    @Override
    public @NotNull ConfigPath resolveSibling(@NotNull String sibling) {
        return resolveSibling(parse(sibling));
    }

    @Override
    public @NotNull ConfigPath subpath(int beginIndex, int endIndex) {
        if (beginIndex < 0 || beginIndex >= nodes.length || endIndex < 0 || endIndex > nodes.length) {
            throw new IndexOutOfBoundsException();
        }

        if (endIndex < beginIndex) {
            throw new IllegalArgumentException("endIndex < beginIndex");
        }

        int length = endIndex - beginIndex;
        if (!nodes[beginIndex].nodeType().isNameOrIndex()) {
            Node[] newNodes = new Node[length];
            System.arraycopy(nodes, beginIndex, newNodes, 0, length);
            return new BasicConfigPath(newNodes);
        }

        Node[] newNodes = new Node[length + 1];
        newNodes[0] = CURRENT_NODE;
        System.arraycopy(nodes, 0, newNodes, 1, length);
        return new BasicConfigPath(newNodes);
    }

    @Override
    public boolean startsWith(@NotNull ConfigPath other) {
        if (this.nodes.length == 0) {
            return other.nodes().isEmpty();
        }

        List<Node> otherNodes = other.nodes();
        if (otherNodes.size() > this.nodes.length) {
            return false;
        }

        for (int i = 0; i < otherNodes.size(); i++) {
            if (!this.nodes[i].equals(otherNodes.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean startsWith(@NotNull String otherPath) {
        return startsWith(parse(otherPath));
    }

    @Override
    public boolean isEmpty() {
        return this.nodes.length == 0;
    }

    private static Node[] nodeArray(ConfigPath configPath) {
        if (configPath instanceof BasicConfigPath basicElementPath) {
            return basicElementPath.nodes;
        }

        return configPath.nodes().toArray(Node[]::new);
    }

    @Override
    public int hashCode() {
        if (hashed) {
            return hash;
        }

        int hash = Arrays.hashCode(nodes);
        this.hash = hash;
        VarHandle.fullFence();
        hashed = true;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof BasicConfigPath other) {
            //do array equality comparison if we can
            return Arrays.equals(nodes, other.nodes);
        }

        if (obj instanceof ConfigPath other) {
            return nodes().equals(other.nodes());
        }

        return false;
    }

    @Override
    public String toString() {
        if (this.nodes.length == 0) {
            return "/";
        }

        return String.join("/", Arrays.stream(nodes).map(Node::name).toArray(String[]::new));
    }
}
