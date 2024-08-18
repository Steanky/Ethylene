package com.github.steanky.ethylene.core.propylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
    static @NotNull ConfigElement fromString(String string) throws IOException {
        return Parser.fromString(string, LinkedConfigNode::new, ArrayConfigList::new);
    }

    @Test
    void topLevelPrimitives() throws IOException {
        assertEquals(fromString("1S"), ConfigPrimitive.of((short)1));
        assertEquals(fromString("-0x1"), ConfigPrimitive.of(-0x1));
        assertEquals(fromString("0xFF"), ConfigPrimitive.of(255));
        assertEquals(fromString("0x7Fz"), ConfigPrimitive.of((byte)127));
        assertEquals(fromString("-0x7Fz"), ConfigPrimitive.of((byte)-127));
        assertEquals(fromString("0"), ConfigPrimitive.of(0));
        assertEquals(fromString("100"), ConfigPrimitive.of(100));
        assertEquals(fromString("-1"), ConfigPrimitive.of(-1));
        assertEquals(fromString("'test'"), ConfigPrimitive.of("test"));
        assertEquals(fromString("1F"), ConfigPrimitive.of(1F));
        assertEquals(fromString("-1F"), ConfigPrimitive.of(-1F));
        assertEquals(fromString("1.0"), ConfigPrimitive.of(1.0));
        assertEquals(fromString("-1.0"), ConfigPrimitive.of(-1.0));
        assertEquals(fromString("0700"), ConfigPrimitive.of(0700));
        assertEquals(fromString("0b101010"), ConfigPrimitive.of(0b101010));
        assertEquals(fromString("-0700"), ConfigPrimitive.of(-0700));
        assertEquals(fromString("-0b101010"), ConfigPrimitive.of(-0b101010));
        assertEquals(fromString("1B"), ConfigPrimitive.of((byte)1));
        assertEquals(fromString("1s"), ConfigPrimitive.of((short)1));
        assertEquals(fromString("1b"), ConfigPrimitive.of((byte)1));
        assertEquals(fromString("1000L"), ConfigPrimitive.of(1000L));
        assertEquals(ConfigPrimitive.of(10000000L), fromString("10________000000L"));
        assertEquals(ConfigPrimitive.NULL, fromString("null"));
        assertEquals(ConfigPrimitive.TRUE, fromString("true"));
        assertEquals(ConfigPrimitive.TRUE, fromString("TRUE"));
        assertEquals(ConfigPrimitive.TRUE, fromString("TrUe"));

        assertEquals(ConfigPrimitive.FALSE, fromString("false"));
        assertEquals(ConfigPrimitive.FALSE, fromString("FALSE"));
        assertEquals(ConfigPrimitive.FALSE, fromString("FaLsE"));
    }

    @Test
    void basicLists() throws IOException {
        assertEquals(ConfigList.of(), fromString("[]"));
        assertEquals(ConfigList.of(0), fromString("[0]"));
        assertEquals(ConfigList.of(ConfigList.of(ConfigList.of())), fromString("[[[]]]"));
    }

    @Test
    void rootReference() {
        assertThrows(IOException.class, () -> fromString("$0"));
    }

    @Test
    void mismatchedBrackets() {
        assertThrows(IOException.class, () -> fromString("[}"));
    }

    @Test
    void unclosedBrackets() {
        assertThrows(IOException.class, () -> fromString("[[]"));
    }

    @Test
    void simpleSelfReference() throws IOException {
        ConfigList list = ConfigList.of();
        list.add(list);

        assertEquals(list, fromString("&0[&0]"));
    }

    @Test
    void doubleSelfReference() throws IOException {
        ConfigList list = ConfigList.of();
        list.add(list);
        list.add(list);

        assertEquals(list, fromString("&0[&0, &0]"));
    }

    @Test
    void missingReference() {
        assertThrows(IOException.class, () -> fromString("[$1, 'test', 0, 0x1]"));
    }

    @Test
    void basicNodes() throws IOException {
        assertEquals(ConfigNode.of(), fromString("{}"));
        assertEquals(ConfigNode.of("a", ConfigNode.of()), fromString("{a={}}"));
        assertEquals(ConfigNode.of("a", ConfigNode.of("a", ConfigNode.of())), fromString("{a={a={}}}"));
    }
}