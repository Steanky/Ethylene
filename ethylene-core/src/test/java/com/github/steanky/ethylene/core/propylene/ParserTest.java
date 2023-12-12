package com.github.steanky.ethylene.core.propylene;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @Test
    void topLevelPrimitives() throws IOException {
        assertEquals(Parser.fromString("1S"), ConfigPrimitive.of((short)1));
        assertEquals(Parser.fromString("-0x1"), ConfigPrimitive.of(-0x1));
        assertEquals(Parser.fromString("0xFF"), ConfigPrimitive.of(255));
        assertEquals(Parser.fromString("0x7Fz"), ConfigPrimitive.of((byte)127));
        assertEquals(Parser.fromString("-0x7Fz"), ConfigPrimitive.of((byte)-127));
        assertEquals(Parser.fromString("0"), ConfigPrimitive.of(0));
        assertEquals(Parser.fromString("100"), ConfigPrimitive.of(100));
        assertEquals(Parser.fromString("-1"), ConfigPrimitive.of(-1));
        assertEquals(Parser.fromString("'test'"), ConfigPrimitive.of("test"));
        assertEquals(Parser.fromString("1F"), ConfigPrimitive.of(1F));
        assertEquals(Parser.fromString("-1F"), ConfigPrimitive.of(-1F));
        assertEquals(Parser.fromString("1.0"), ConfigPrimitive.of(1.0));
        assertEquals(Parser.fromString("-1.0"), ConfigPrimitive.of(-1.0));
        assertEquals(Parser.fromString("0700"), ConfigPrimitive.of(0700));
        assertEquals(Parser.fromString("0b101010"), ConfigPrimitive.of(0b101010));
        assertEquals(Parser.fromString("-0700"), ConfigPrimitive.of(-0700));
        assertEquals(Parser.fromString("-0b101010"), ConfigPrimitive.of(-0b101010));
        assertEquals(Parser.fromString("1B"), ConfigPrimitive.of((byte)1));
        assertEquals(Parser.fromString("1s"), ConfigPrimitive.of((short)1));
        assertEquals(Parser.fromString("1b"), ConfigPrimitive.of((byte)1));
        assertEquals(Parser.fromString("1000L"), ConfigPrimitive.of(1000L));
        assertEquals(ConfigPrimitive.of(10000000L), Parser.fromString("10________000000L"));
        assertEquals(ConfigPrimitive.NULL, Parser.fromString("null"));
        assertEquals(ConfigPrimitive.TRUE, Parser.fromString("true"));
        assertEquals(ConfigPrimitive.TRUE, Parser.fromString("TRUE"));
        assertEquals(ConfigPrimitive.TRUE, Parser.fromString("TrUe"));

        assertEquals(ConfigPrimitive.FALSE, Parser.fromString("false"));
        assertEquals(ConfigPrimitive.FALSE, Parser.fromString("FALSE"));
        assertEquals(ConfigPrimitive.FALSE, Parser.fromString("FaLsE"));
    }

    @Test
    void basicLists() throws IOException {
        assertEquals(ConfigList.of(), Parser.fromString("[]"));
        assertEquals(ConfigList.of(0), Parser.fromString("[0]"));
        assertEquals(ConfigList.of(ConfigList.of(ConfigList.of())), Parser.fromString("[[[]]]"));
    }

    @Test
    void rootReference() {
        assertThrows(IOException.class, () -> Parser.fromString("$0"));
    }

    @Test
    void mismatchedBrackets() {
        assertThrows(IOException.class, () -> Parser.fromString("[}"));
    }

    @Test
    void unclosedBrackets() {
        assertThrows(IOException.class, () -> Parser.fromString("[[]"));
    }

    @Test
    void simpleSelfReference() throws IOException {
        ConfigList list = ConfigList.of();
        list.add(list);

        assertEquals(list, Parser.fromString("&0[&0]"));
    }

    @Test
    void doubleSelfReference() throws IOException {
        ConfigList list = ConfigList.of();
        list.add(list);
        list.add(list);

        assertEquals(list, Parser.fromString("&0[&0, &0]"));
    }

    @Test
    void missingReference() {
        assertThrows(IOException.class, () -> Parser.fromString("[$1, 'test', 0, 0x1]"));
    }

    @Test
    void basicNodes() throws IOException {
        assertEquals(ConfigNode.of(), Parser.fromString("{}"));
        assertEquals(ConfigNode.of("a", ConfigNode.of()), Parser.fromString("{a={}}"));
        assertEquals(ConfigNode.of("a", ConfigNode.of("a", ConfigNode.of())), Parser.fromString("{a={a={}}}"));
    }
}