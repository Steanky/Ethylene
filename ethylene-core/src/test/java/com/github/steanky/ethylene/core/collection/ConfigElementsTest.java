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
        List<ConfigElement> simpleNonConfigList =
            List.of(ConfigPrimitive.of("a"), ConfigPrimitive.of("b"), ConfigPrimitive.of("c"));

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
    void cycleWithDifferentElement() {
        ConfigList cycle = ConfigList.of("first");
        cycle.add(cycle);

        ConfigList otherList = ConfigList.of("first", ConfigList.of(0));

        assertFalse(ConfigElements.equals(cycle, otherList));
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
    void repeatedHash() {
        List<ConfigElement> simpleHash = List.of(ConfigPrimitive.of("a"), ConfigPrimitive.of("b"));
        List<List<ConfigElement>> nested = List.of(simpleHash, simpleHash);
        ConfigList nestedList = ConfigList.of("a", "b");

        assertEquals(nested.hashCode(), ConfigElements.hashCode(ConfigList.of(nestedList, nestedList)));
    }

    @Test
    void emptyHash() {
        assertEquals(Map.of().hashCode(), ConfigElements.hashCode(ConfigNode.of()));
        assertEquals(List.of().hashCode(), ConfigElements.hashCode(ConfigList.of()));
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
    void repeatingListEquals() {
        ConfigList first = ConfigList.of("first");
        ConfigList sublist = ConfigList.of("a");

        first.add(sublist);
        first.add(sublist);
        first.addNumber(0);

        ConfigList second = ConfigList.of("first", ConfigList.of("a"), ConfigList.of("b"), 0);

        assertFalse(ConfigElements.equals(first, second));
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

    @Test
    void simpleHash2() {
        Map<String, Object> map = Map.of("first", List.of(ConfigPrimitive.of("test")));

        ConfigNode node = ConfigNode.of("first", ConfigList.of(ConfigPrimitive.of("test")));

        assertEquals(map.hashCode(), ConfigElements.hashCode(node));
    }

    @Test
    void referentialEquals() {
        ConfigList list = ConfigList.of("a");
        list.add(list);
        list.addString("b");

        System.out.println(list);

        ConfigList otherList = ConfigList.of("a", ConfigList.of(0, 2, 3), "b");

        assertFalse(ConfigElements.equals(list, otherList));
    }

    //naming these tests is getting really hard...
    @Test
    void referentialEqualsWithEquivalentTopology() {
        ConfigList list = ConfigList.of("a");
        list.add(list);
        list.addString("b");

        System.out.println(list);

        ConfigList otherList = ConfigList.of("a");
        otherList.add(otherList);
        otherList.addString("b");

        assertTrue(ConfigElements.equals(list, otherList));
    }

    @Test
    void aaaaaaaaaaaaaaaaaaaaaaaaa() {
        Map<String, Object> compositeFuckery = Map.of("let the pain",
            ConfigPrimitive.of("begin"), "why", List.of(ConfigPrimitive.of("test"),
                ConfigList.of("a", "b", "c")));

        ConfigNode node = ConfigNode.of("let the pain", "begin", "why",
            ConfigList.of("test", ConfigList.of("a", "b", "c")));

        assertTrue(ConfigElements.equals(node, compositeFuckery));
        assertEquals(ConfigElements.hashCode(node), compositeFuckery.hashCode());
    }

    @Test
    void simpleToString() {
        ConfigNode emptyNode = ConfigNode.of();
        assertEquals("{}", ConfigElements.toString(emptyNode));

        ConfigList emptyList = ConfigList.of();
        assertEquals("[]", ConfigElements.toString(emptyList));

        ConfigPrimitive primitive = ConfigPrimitive.of(0);
        assertEquals("0", ConfigElements.toString(primitive));
    }

    @Test
    void primitiveListToString() {
        ConfigList list = ConfigList.of(0, 1, 2, 3);
        assertEquals("[0, 1, 2, 3]", ConfigElements.toString(list));
    }

    @Test
    void primitiveNodeToString() {
        ConfigNode node = ConfigNode.of("first", 0, "second", 1);
        assertEquals("{first=0, second=1}", ConfigElements.toString(node));
    }

    @Test
    void nodeContainingListToString() {
        ConfigNode node = ConfigNode.of("first", 0, "second", ConfigList.of("a", "b", "c"));
        assertEquals("{first=0, second=['a', 'b', 'c']}", ConfigElements.toString(node));
    }

    @Test
    void duplicateReferenceToString() {
        ConfigList list = ConfigList.of("a");
        ConfigNode node = ConfigNode.of("first", list, "second", list, "third", list);

        assertEquals("{first=$0['a'], second=$0, third=$0}", ConfigElements.toString(node));
    }

    @Test
    void selfReferentialToString() {
        ConfigList list = ConfigList.of();
        list.add(list);

        assertEquals("$0[$0]", ConfigElements.toString(list));
    }

    @Test
    void selfReferentialToString2() {
        ConfigList list = ConfigList.of();
        list.add(list);
        list.add(list);

        assertEquals("$0[$0, $0]", ConfigElements.toString(list));
    }

    @Test
    void selfReferentialContainingOthers() {
        ConfigList list = ConfigList.of(0, 1, 2);
        list.add(list);

        assertEquals("$0[0, 1, 2, $0]", ConfigElements.toString(list));
    }

    @Test
    void wtf() {
        ConfigNode node = ConfigNode.of("pain", ConfigList.of());
        node.getElement("pain").asList().add(node);
        node.put("self", node);
        node.putNumber("value", 100);
        ConfigList selfReferential = ConfigList.of();

        selfReferential.add(selfReferential);
        selfReferential.addString("no");
        node.put("suffering", selfReferential);
        node.put("suffering2", selfReferential);

        assertEquals("$0{pain=[$0], self=$0, value=100, suffering=$1[$1, 'no'], suffering2=$1}", ConfigElements.toString(node));
    }

    @Test
    void repeatingSeveral() {
        ConfigList list = ConfigList.of();

        ConfigList repeating = ConfigList.of("a");
        ConfigList otherRepeating = ConfigList.of("b");

        list.add(repeating);
        list.add(repeating);

        list.add(otherRepeating);
        list.add(otherRepeating);
        list.add(otherRepeating);

        assertEquals("[$0['a'], $0, $1['b'], $1, $1]", ConfigElements.toString(list));
    }

    @Test
    void repeatingSeveralAndSelfReference() {
        ConfigList list = ConfigList.of();

        ConfigList repeating = ConfigList.of("a");
        ConfigList otherRepeating = ConfigList.of("b");

        list.add(repeating);
        list.add(repeating);

        list.add(otherRepeating);
        list.add(otherRepeating);
        list.add(otherRepeating);

        list.add(list);

        assertEquals("$2[$0['a'], $0, $1['b'], $1, $1, $2]", ConfigElements.toString(list));
    }

    @Test
    void repeatingList() {
        ConfigList first = ConfigList.of("first");
        ConfigList sublist = ConfigList.of("a");

        first.add(sublist);
        first.add(sublist);

        assertEquals("['first', $0['a'], $0]", ConfigElements.toString(first));
    }
}