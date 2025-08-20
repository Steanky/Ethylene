package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    private ConfigElement fromString(String string) throws IOException {
        try (Reader reader = new StringReader(string)) {
            return Parser.fromReader(reader, LinkedConfigNode::new, ArrayConfigList::new);
        }
    }

    private ConfigElement fromInputStream(InputStream is) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(is,
            StandardCharsets.UTF_8.newDecoder()
                .replaceWith("�")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE))) {
            return Parser.fromReader(reader, LinkedConfigNode::new, ArrayConfigList::new);
        }
    }

    @Test
    void unpairedHighSurrogate() {
        String data = """
            value: "\\uD800"
            """;

        ButyleneParseException exception = assertThrows(ButyleneParseException.class, () -> fromString(data));

        assertEquals(1, exception.getLine(), exception.getMessage());
        assertEquals(15, exception.getColumn(), exception.getMessage());
    }

    @Test
    void invalidSurrogatePair() {
        String data = """
            value: "\\uD800\\uD800"
            """;

        ButyleneParseException exception = assertThrows(ButyleneParseException.class, () -> fromString(data));

        assertEquals(1, exception.getLine(), exception.getMessage());
        assertEquals(15, exception.getColumn(), exception.getMessage());
    }

    @Test
    void valueCases() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        ConfigElement reqs = assertDoesNotThrow(() -> fromInputStream(classloader.getResourceAsStream("reqs/reqs.butylene")));
        assertTrue(reqs.isNode());

        ConfigNode reqNode = reqs.asNode();

        for (int i = 1; i <= 12; i++) {
            String caseName = "reqs/case_" + i + ".butylene";

            InputStream caseStream = Objects.requireNonNull(classloader.getResourceAsStream(caseName), caseName);
            ConfigElement element = assertDoesNotThrow(() -> fromInputStream(caseStream), caseName);

            assertEquals(reqNode.at(String.valueOf(i)).asString(), element.toString(), caseName);
        }
    }

    @Test
    // test cases adapted from https://github.com/nst/JSONTestSuite/
    void nstJsonSuite() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        // all pass_*.json must validate
        for (int i = 1; i <= 95; i++) {
            String name = "nst_suite/pass_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> fromInputStream(is), name);
        }

        Set<Integer> failExcludes = Set.of(
            37, 58, 63, 69, // NaN and Infinity are valid values in Butylene
            9, 19, 89, 100, // Butylene accepts a single trailing comma
            95, 96, 97, // unquoted key is valid Butylene, even if the key appears to be a non-string literal
            99, // Butylene doesn't care about singlequotes appearing in a non-quoted key
            101, 103, // comments can appear even after the last closing bracket
            106, // non-quoted key is valid Butylene
            110, 142, 157, // entirely empty file is valid Butylene
            162 // multiline comments can go anywhere between tokens
        );

        for (int i = 1; i <= 188; i++) {
            if (failExcludes.contains(i)) continue;

            String name = "nst_suite/fail_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertThrows(IOException.class, () -> fromInputStream(is), name);
        }

        Set<Integer> optExcludes = Set.of(
            11, 12, 13, 17, 18, 19, 20, 21, 23, 25, // invalid surrogate pairs in strings aren't valid Butylene
            14, // replacement characters aren't valid whitespace
            32, 33, 35// null bytes aren't either
        );

        for (int i = 1; i <= 35; i++) {
            if (optExcludes.contains(i)) continue;

            String name = "nst_suite/opt_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> fromInputStream(is), name);
        }
    }

    @Test
    // uses the test files found at https://www.json.org/JSON_checker/, with some added cases
    void jsonNetSuite() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        for (int i = 1; i <= 3; i++) {
            String name = "json_net/pass" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> fromInputStream(is));
        }

        // we need to ignore a few of the "fail" tests, because although they contain invalid json, they actually
        // contain valid Butylene
        Set<Integer> failIgnores = Set.of(
            1, // top-level strings are valid Butylene
            3, // ignored because unquoted keys are valid Butylene
            4, 9, // ignored because exactly one trailing comma is valid Butylene
            18 // Butylene doesn't have a defined depth limit
        );

        for (int i = 1; i <= 34; i++) {
            if (failIgnores.contains(i)) continue;

            String name = "json_net/fail" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);
            assertThrows(IOException.class, () -> fromInputStream(is));
        }
    }
}