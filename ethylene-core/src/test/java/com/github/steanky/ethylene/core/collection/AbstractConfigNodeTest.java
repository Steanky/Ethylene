package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AbstractConfigNodeTest {
    private final ConfigList innerList;
    private final ConfigNode populatedNode;

    AbstractConfigNodeTest() {
        innerList = new ArrayConfigList();
        ConfigNode innerNode = new LinkedConfigNode();

        populatedNode = new AbstractConfigNode(new HashMap<>()) {
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

    @SuppressWarnings("CollectionAddedToSelf")
    @Test
    void toStringTest() {
        assertEquals("$0{}", new AbstractConfigNode(new HashMap<>()) {
        }.toString());

        AbstractConfigNode node = new AbstractConfigNode(new LinkedHashMap<>()) {
        };
        node.put("self", node);

        assertEquals("$0{self=$0}", node.toString());

        ConfigList list = new ArrayConfigList();
        node.put("list", list);

        assertEquals("$0{self=$0, list=$1{}}", node.toString());

        list.add(list);
        list.add(node);
        assertEquals("$0{self=$0, list=$1{$1, $0}}", node.toString());

        list.add(ConfigPrimitive.of(10));

        assertEquals("$0{self=$0, list=$1{$1, $0, [10]}}", node.toString());
    }

    @SuppressWarnings("CollectionAddedToSelf")
    @Test
    void cloneTest() {
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
}