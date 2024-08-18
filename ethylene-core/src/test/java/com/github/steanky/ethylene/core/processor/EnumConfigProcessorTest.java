package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumConfigProcessorTest {
    public enum TestEnum {
        UPPER_CASE,
        lower_case,
        mixed_CASE
    }

    @Test
    void simple() throws ConfigProcessException {
        ConfigProcessor<TestEnum> processor = new EnumConfigProcessor<>(TestEnum.class);
        TestEnum result = processor.dataFromElement(ConfigPrimitive.of("UPPER_CASE"));

        assertEquals(TestEnum.UPPER_CASE, result);
        assertThrows(ConfigProcessException.class, () -> processor.dataFromElement(ConfigPrimitive.of("upper_case")));
    }

    @Test
    void mixedCase() throws ConfigProcessException {
        ConfigProcessor<TestEnum> processor = new EnumConfigProcessor<>(TestEnum.class, false);
        TestEnum result = processor.dataFromElement(ConfigPrimitive.of("mIxEd_case"));

        assertEquals(TestEnum.mixed_CASE, result);
    }
}