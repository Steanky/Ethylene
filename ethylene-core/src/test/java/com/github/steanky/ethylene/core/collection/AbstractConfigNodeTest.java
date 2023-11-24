package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("CollectionAddedToSelf")
class AbstractConfigNodeTest {
    private final ConfigList innerList;
    private final ConfigNode populatedNode;

    AbstractConfigNodeTest() {
        innerList = new ArrayConfigList();
        ConfigNode innerNode = new LinkedConfigNode();

        populatedNode = new BackedConfigNode(new HashMap<>()) {
        };
        populatedNode.put("int", ConfigPrimitive.of(10));
        populatedNode.put("string", ConfigPrimitive.of("string"));
        populatedNode.put("list", innerList);

        innerList.add(ConfigPrimitive.of("list_string"));
        innerList.add(innerNode);
        innerList.add(ConfigPrimitive.of(10));

        innerNode.put("inner_string", ConfigPrimitive.of("this is an inner string"));
        innerNode.put("circular_reference", populatedNode);
    }

    @Test
    void validType() {
        assertTrue(populatedNode.isNode());
        assertSame(populatedNode, populatedNode.asNode());
    }

    @Test
    void noDefaultOnValidPath() {
        assertEquals(10, populatedNode.getNumberOrDefault(1000, "int"));
        assertEquals("string", populatedNode.getStringOrDefault("default", "string"));
        assertSame(innerList, populatedNode.getListOrDefault((ConfigList) null, "list"));
        assertSame(populatedNode, populatedNode.getNodeOrDefault((ConfigNode) null, "list", 1, "circular_reference"));
    }

    @Test
    void copy() {
        ConfigNode node = ConfigNode.of("a", 10, "b", ConfigList.of("a"));
        node.put("self", node);
        node.get("b").asList().add(node);

        ConfigNode copy = node.copy();

        assertNotSame(node, copy);
        assertNotSame(node, copy.get("self"));
        assertNotSame(node.get("b"), copy.get("b"));
        assertNotSame(node.get("b").asList().get(1), copy);

        assertSame(copy, copy.get("self"));
        assertSame(copy, copy.get("b").asList().get(1));
    }

    @Test
    void simpleImmutableCopy() {
        ConfigNode node = ConfigNode.of("a", 10);
        ConfigNode copy = node.immutableCopy();

        assertEquals(10, copy.get("a").asNumber());
    }

    @Test
    void selfReferentialImmutableCopy() {
        ConfigNode node = ConfigNode.of();
        node.put("self", node);

        ConfigNode copy = node.immutableCopy();

        assertSame(copy, copy.get("self"));
        assertSame(1, copy.size());
    }

    @Test
    void sameInstanceImmutableCopy() {
        ConfigNode node = ConfigNode.of("test", 10).immutableCopy();

        assertSame(node, node.immutableCopy());
    }

    @Test
    void sameInstanceImmutableDeepCopy() {
        ConfigNode node = ConfigNode.of("test", 10, "sub", ConfigNode.of("test", 69));
        ConfigNode copy = node.immutableCopy();

        assertThrows(UnsupportedOperationException.class,
            () -> copy.get("sub").asNode().put("test", ConfigPrimitive.NULL));
    }

    @Test
    void viewTest() {
        ConfigNode underlying = ConfigNode.of("test", 10);

        ConfigNode view = underlying.immutableView();
        assertThrows(UnsupportedOperationException.class, () -> view.put("test", ConfigPrimitive.NULL));

        assertEquals(10, view.get("test").asNumber());

        underlying.putNumber("second", 20);

        assertEquals(20, view.get("second").asNumber());
    }
}