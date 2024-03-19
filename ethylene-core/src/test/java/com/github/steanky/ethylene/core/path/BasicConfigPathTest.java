package com.github.steanky.ethylene.core.path;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BasicConfigPathTest {
    @Test
    void backCommand() {
        ConfigPath base = ConfigPath.of("");
        assertEquals(ConfigPath.of(".."), base.resolve(".."));
    }

    @Test
    void sibling4() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        assertEquals(ConfigPath.of("/a/b/g"), base.resolveSibling("../g"));
    }

    @Test
    void sibling3() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        assertEquals(ConfigPath.of("/a/b/c/f/g/h"), base.resolveSibling("./f/g/h"));
    }

    @Test
    void sibling1() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        assertEquals(ConfigPath.of("f"), base.resolveSibling("f"));
    }

    @Test
    void sibling() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        assertEquals(ConfigPath.of("a/b/c/f"), base.resolveSibling("./f"));
    }

    @Test
    void parent() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        assertEquals(ConfigPath.of("/a/b/c"), base.getParent());
    }

    @Test
    void rootParentNull() {
        assertNull(ConfigPath.of("").getParent());
    }

    private static void assertNormalized(ConfigPath configPath) {
        boolean foundName = false;
        for (ConfigPath.Node node : configPath.nodes()) {
            if (node.nodeType() == ConfigPath.NodeType.NAME) {
                foundName = true;
            }
            else if (foundName) {
                fail(configPath + " is not normalized");
            }
        }
    }

    @Test
    void relativeRelativize4() {
        ConfigPath base = ConfigPath.of("../../");
        ConfigPath other = ConfigPath.of("../../../../../a/b/c/d/e/f/g");

        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void relativeRelativize3() {
        ConfigPath base = ConfigPath.of("../../a/b/c");
        ConfigPath other = ConfigPath.of("../..");

        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void relativeRelativize2() {
        ConfigPath base = ConfigPath.of("../../");
        ConfigPath other = ConfigPath.of("../../a/b/c");

        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void relativeRelativize1() {
        ConfigPath base = ConfigPath.of("..");
        ConfigPath other = ConfigPath.of("../a");

        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void relativeRelativize() {
        ConfigPath base = ConfigPath.of("./a/b/c/d");
        ConfigPath other = ConfigPath.of("./a/b/c");

        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize8() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/f/g/h");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize7() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/a/b/c/d");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize6() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/a/b/c/d/e/f/g");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize5() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/a/b/c/f");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize4() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("f");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize3() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/a");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize2() {
        ConfigPath base = ConfigPath.of("/a/b/c/d");
        ConfigPath other = ConfigPath.of("/a/b/c");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void simpleRelativize() {
        ConfigPath base = ConfigPath.of("/a/b/c");
        ConfigPath other = ConfigPath.of("/a/b/c/d");
        ConfigPath relative = base.relativize(other);

        assertEquals(other, base.resolve(relative));
        assertNormalized(relative);
    }

    @Test
    void appendToCurrent() {
        ConfigPath current = ConfigPath.of(".");
        assertEquals(ConfigPath.of("./0"), current.append("0"));
    }

    @Test
    void chainedPreviousMakingEmpty() {
        ConfigPath path = BasicConfigPath.parse("/test/test1/test2/test3/test4/../../../../..");
        assertEquals(List.of(), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void chainedPrevious() {
        ConfigPath path = BasicConfigPath.parse("/test/test1/test2/test3/test4/../../../..");
        assertEquals(List.of("test"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void previousToAbsolute() {
        ConfigPath path = BasicConfigPath.parse("./..").toAbsolute();
        assertEquals(List.of(), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void messyToAbsolute() {
        ConfigPath path = BasicConfigPath.parse("./test///././././././././././.").toAbsolute();
        assertEquals(List.of("test"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void simpleToAbsolute() {
        ConfigPath path = BasicConfigPath.parse("./test").toAbsolute();
        assertEquals(List.of("test"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void pain() {
        BasicConfigPath path = BasicConfigPath.parse("////./././//////./test/../..//////test2/../././//.//////");
        assertEquals(List.of(".."), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void redundantSlashes() {
        BasicConfigPath path = BasicConfigPath.parse("////./././//////./test/../..//////test2/./././//.//////");
        assertEquals(List.of("..", "test2"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void redundantPrevious() {
        BasicConfigPath path = BasicConfigPath.parse("./../..");
        assertEquals(List.of("..", ".."), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void resolvePrevious1() {
        ConfigPath base = ConfigPath.of("..");
        ConfigPath resolved = base.resolve("./a/b/c");

        assertEquals(ConfigPath.of("../a/b/c"), resolved);
    }

    @Test
    void resolvePrevious() {
        ConfigPath base = ConfigPath.of("..");
        ConfigPath resolved = base.resolve(".");

        assertEquals(ConfigPath.of(".."), resolved);
    }

    @Test
    void currentToPrevious() {
        BasicConfigPath path = BasicConfigPath.parse("./..");
        assertEquals(List.of(".."), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void mixedPath() {
        BasicConfigPath path = BasicConfigPath.parse("test/../../test2/./././.");
        assertEquals(List.of("..", "test2"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void mixedPath2() {
        BasicConfigPath path = BasicConfigPath.parse("./test/../../test2/./././.");
        assertEquals(List.of("..", "test2"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void escapedBackslash() {
        BasicConfigPath path = BasicConfigPath.parse("\\\\test/test");
        assertEquals(List.of("\\test", "test"), path.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void escapedCommandNodes() {
        BasicConfigPath path = BasicConfigPath.parse("\\../\\.");
        List<ConfigPath.Node> nodes = path.nodes();

        assertEquals(nodes.get(0).nodeType(), ConfigPath.NodeType.NAME);
        assertEquals(nodes.get(1).nodeType(), ConfigPath.NodeType.NAME);
    }

    @Test
    void simpleRelativePath() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("./relative/path");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of("this", "is", "a", "test", "relative", "path"),
                result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void relativeRelativePath() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("./this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("./relative/path");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of(".", "this", "is", "a", "test", "relative", "path"),
                result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void backReference() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("..");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of("this", "is", "a"), result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void doubleBackReference() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("../..");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of("this", "is"), result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void tripleBackReference() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("../../..");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of("this"), result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void quadrupleBackReference() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("../../../..");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of(), result.nodes().stream().map(ConfigPath.Node::name).toList());
    }

    @Test
    void quintupleBackReference() {
        BasicConfigPath absolutePath = BasicConfigPath.parse("/this/is/a/test");
        BasicConfigPath relativePath = BasicConfigPath.parse("../../../../..");

        ConfigPath result = absolutePath.resolve(relativePath);

        assertEquals(List.of(".."), result.nodes().stream().map(ConfigPath.Node::name).toList());
    }
}