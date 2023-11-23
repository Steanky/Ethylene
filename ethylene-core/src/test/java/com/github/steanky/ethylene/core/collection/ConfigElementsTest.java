package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigElementsTest {
    @Test
    void nullEquating() {
        assertTrue(ConfigElements.equals(null, null));
        assertFalse(ConfigElements.equals(ConfigList.of(), null));
        assertFalse(ConfigElements.equals(null, ConfigList.of()));
        assertTrue(ConfigElements.equals(ConfigPrimitive.of(0), ConfigPrimitive.of(0)));
    }

    @Test
    void simpleEquals() {
        ConfigPrimitive primitive = ConfigPrimitive.of("test");
        ConfigPrimitive equalPrimitive = ConfigPrimitive.of("test");

        assertTrue(ConfigElements.equals(primitive, equalPrimitive));

        ConfigElement emptyList = ConfigList.of();
        ConfigElement otherEmptyList = ConfigList.of();

        assertTrue(ConfigElements.equals(emptyList, otherEmptyList));

        ConfigElement emptyNode = ConfigNode.of();
        ConfigElement otherEmptyNode = ConfigNode.of();

        assertTrue(ConfigElements.equals(emptyNode, otherEmptyNode));
    }

    @Test
    void listEqualsContractSimple() {
        ConfigList emptyList = ConfigList.of();
        List<ConfigElement> nonConfigList = List.of();

        assertTrue(ConfigElements.equals(emptyList, nonConfigList));
        assertEquals(nonConfigList, emptyList);

        ConfigList simpleList = ConfigList.of("a", "b", "c");
        List<ConfigElement> simpleNonConfigList = List.of(ConfigPrimitive.of("a"),
            ConfigPrimitive.of("b"), ConfigPrimitive.of("c"));

        assertTrue(ConfigElements.equals(simpleList, simpleNonConfigList));
        assertEquals(simpleNonConfigList, simpleList);
    }

    @Test
    void selfReferentialList() {
        ConfigList listContainingItself = ConfigList.of();
        listContainingItself.add(listContainingItself);

        ConfigList otherListContainingItself = ConfigList.of();
        otherListContainingItself.add(otherListContainingItself);

        assertTrue(ConfigElements.equals(listContainingItself, otherListContainingItself));
        assertTrue(ConfigElements.equals(otherListContainingItself, listContainingItself));
    }

    @Test
    void cycleWithDifferentPosition() {
        ConfigList cycle = ConfigList.of("a");
        cycle.add(cycle); //a, cycle

        ConfigList otherCycle = ConfigList.of();
        otherCycle.add(otherCycle);
        otherCycle.addString("a"); //cycle, a

        assertFalse(ConfigElements.equals(cycle, otherCycle));
        assertFalse(ConfigElements.equals(otherCycle, cycle));
    }

    @Test
    void hashCodeContract() {
        List<ConfigPrimitive> list = List.of(ConfigPrimitive.of("this is a test"), ConfigPrimitive.of(0));

        ConfigList configList = ConfigList.of("this is a test", 0);
        assertTrue(ConfigElements.equals(configList, list));
        assertEquals(list, configList);

        assertEquals(list.hashCode(), ConfigElements.hashCode(configList));
    }

    @Test
    void complexEquals() {
        ConfigList first = ConfigList.of("first", 0, 'a',
            ConfigNode.of("key", "this is a value", "number", 100));

        List<Object> list = List.of(ConfigPrimitive.of("first"), ConfigPrimitive.of(0),
            ConfigPrimitive.of('a'), Map.of("key", ConfigPrimitive.of("this is a value"), "number",
                ConfigPrimitive.of(100)));

        assertTrue(ConfigElements.equals(first, list));
        assertEquals(list, first);
    }

    @Test
    void complexHashCode() {
        ConfigList first = ConfigList.of("first", 0, 'a',
            ConfigNode.of("key", "this is a value", "number", 100));

        List<Object> list = List.of(ConfigPrimitive.of("first"), ConfigPrimitive.of(0),
            ConfigPrimitive.of('a'), Map.of("key", ConfigPrimitive.of("this is a value"), "number",
                ConfigPrimitive.of(100)));

        assertEquals(list.hashCode(), ConfigElements.hashCode(first));
    }

    @Test
    void whatIsThis() {
        Map<String, Object> compositeFuckery = Map.of("let the pain",
            ConfigPrimitive.of("begin"), "why", List.of(ConfigPrimitive.of("test"),
                ConfigList.of("a", "b", "c")));
        List immutableList = (List)compositeFuckery.get("why");
        ((ConfigList)immutableList.get(1)).add((ConfigElement) immutableList.get(1));

        ConfigNode node = ConfigNode.of("let the pain", "begin", "why",
            ConfigList.of("test", ConfigList.of("a", "b", "c")));
        node.get("why").asList().get(1).asList().add(node.get("why").asList().get(1));

        assertTrue(ConfigElements.equals(node, compositeFuckery));
    }
}