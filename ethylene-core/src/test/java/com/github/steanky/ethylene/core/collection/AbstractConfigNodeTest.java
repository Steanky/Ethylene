package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigPrimitive;
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

        populatedNode = new AbstractConfigNode(new HashMap<>()) {};
        populatedNode.put("int", new ConfigPrimitive(10));
        populatedNode.put("string", new ConfigPrimitive("string"));
        populatedNode.put("list", innerList);

        innerList.add(new ConfigPrimitive("list_string"));
        innerList.add(innerNode);
        innerList.add(new ConfigPrimitive(10));

        innerNode.put("inner_string", new ConfigPrimitive("this is an inner string"));
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
        assertEquals("$0{}", new AbstractConfigNode(new HashMap<>()) {}.toString());

        AbstractConfigNode node = new AbstractConfigNode(new LinkedHashMap<>()) {};
        node.put("self", node);

        assertEquals("$0{self=$0}", node.toString());

        ConfigList list = new ArrayConfigList();
        node.put("list", list);

        assertEquals("$0{self=$0, list=$1{}}", node.toString());

        list.add(list);
        list.add(node);
        assertEquals("$0{self=$0, list=$1{$1, $0}}", node.toString());

        list.add(new ConfigPrimitive(10));

        assertEquals("$0{self=$0, list=$1{$1, $0, [10]}}", node.toString());
    }
}