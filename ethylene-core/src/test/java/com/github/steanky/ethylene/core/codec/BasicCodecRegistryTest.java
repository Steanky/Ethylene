package com.github.steanky.ethylene.core.codec;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BasicCodecRegistryTest {
    CodecRegistry testRegistry;

    BasicCodecRegistryTest() {
        testRegistry = new BasicCodecRegistry();
    }

    @Test
    void nullNameSetThrows() {
        ConfigCodec mockCodec = Mockito.mock(ConfigCodec.class);
        Mockito.when(mockCodec.getNames()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> testRegistry.registerCodec(mockCodec));
    }

    @Test
    void nullNameThrows() {
        ConfigCodec mockCodec = Mockito.mock(ConfigCodec.class);

        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("b");
        set.add(null);

        Mockito.when(mockCodec.getNames()).thenReturn(set);
        assertThrows(NullPointerException.class, () -> testRegistry.registerCodec(mockCodec));
    }

    @Test
    void emptyNameThrows() {
        ConfigCodec mockCodec = Mockito.mock(ConfigCodec.class);
        Mockito.when(mockCodec.getNames()).thenReturn(Set.of());
        assertThrows(IllegalArgumentException.class, () -> testRegistry.registerCodec(mockCodec));
    }

    @Test
    void duplicateNameThrows() {
        ConfigCodec mockCodec = Mockito.mock(ConfigCodec.class);
        Mockito.when(mockCodec.getNames()).thenReturn(Set.of("NaMe"));
        testRegistry.registerCodec(mockCodec);

        ConfigCodec mockCodec2 = Mockito.mock(ConfigCodec.class);
        Mockito.when(mockCodec2.getNames()).thenReturn(Set.of("NAME"));

        assertThrows(IllegalArgumentException.class, () -> testRegistry.registerCodec(mockCodec2));
    }

    @Test
    void containsCodec() {
        ConfigCodec mockCodec = Mockito.mock(ConfigCodec.class);
        Mockito.when(mockCodec.getNames()).thenReturn(Set.of("namE"));
        testRegistry.registerCodec(mockCodec);

        assertTrue(testRegistry.hasCodec("NAME"));
        assertSame(mockCodec, testRegistry.getCodec("Name"));
    }
}