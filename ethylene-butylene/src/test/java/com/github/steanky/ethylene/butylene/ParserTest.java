package com.github.steanky.ethylene.butylene;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.HashConfigNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @Test
    void badOverrideType() throws IOException {
        String data = """
            node: {
              test: 10
            }
            
            bad_override: >node [ ]
            """;

        ConfigElement element = Parser.fromReader(new StringReader(data), HashConfigNode::new, ArrayConfigList::new);


    }

    @Test
    void override() {
        String data = """
            source: &source {
              key: 10
              other_key: 20
            }
            
            override: >source {
              key: 42
            }
            
            what_are_you_doing: &node {
              troll: "\\u0000"
            }
            
            list_source: &list_source [ 2, 3, 4 ]
            list_override: >list_source [ 0, 1, *node ]
            """;

        ConfigElement config =
            assertDoesNotThrow(() -> Parser.fromReader(new StringReader(data), HashConfigNode::new, ArrayConfigList::new));

        assertEquals(ConfigPrimitive.of(42.0), config.asNode().at("override/key"));
        assertEquals(ConfigPrimitive.of(20.0), config.asNode().at("override/other_key"));
    }

    @Test
    // test cases adapted from https://github.com/nst/JSONTestSuite/
    void nstJsonSuite() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        // all pass_*.json must validate
        for (int i = 1; i <= 95; i++) {
            String name = "nst_suite/pass_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> Parser.fromReader(new InputStreamReader(is), HashConfigNode::new, ArrayConfigList::new), name);
            is.close();
        }

        Set<Integer> failExcludes = Set.of(
            9, 19, 89, 100, // Butylene accepts a single trailing comma
            95, 96, 97, // unquoted key is valid Butylene, even if the key appears to be a non-string literal
            99, // Butylene doesn't care about singlequotes appearing in a non-quoted key
            101, 103, // comments can appear even after the last closing bracket
            106, // non-quoted key is valid Butylene
            110, 157, // entirely empty file is valid Butylene
            162 // multiline comments can go anywhere between tokens
        );

        for (int i = 1; i <= 188; i++) {
            if (failExcludes.contains(i)) continue;

            String name = "nst_suite/fail_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertThrows(IOException.class, () -> Parser.fromReader(new InputStreamReader(is), HashConfigNode::new, ArrayConfigList::new), name);
            is.close();
        }

        Set<Integer> optExcludes = Set.of(
            14, 32, 33, // Butylene is STRICTLY UTF-8
            35 // Butylene doesn't support a BOM
        );

        for (int i = 1; i <= 35; i++) {
            if (optExcludes.contains(i)) continue;

            String name = "nst_suite/opt_" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> Parser.fromReader(new InputStreamReader(is), HashConfigNode::new, ArrayConfigList::new), name);
            is.close();
        }
    }

    @Test
    // uses the test files found at https://www.json.org/JSON_checker/, with some added cases
    void jsonNetSuite() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        for (int i = 1; i <= 3; i++) {
            String name = "json_net/pass" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);

            assertDoesNotThrow(() -> Parser.fromReader(new InputStreamReader(is), HashConfigNode::new, ArrayConfigList::new), name);
            is.close();
        }

        // we need to ignore a few of the "fail" tests, because although they contain invalid json, they actually
        // contain valid Butylene
        Set<Integer> failIgnores = Set.of(
            1, // top-level strings are valid Butylene
            3, // ignored because unquoted keys are valid Butylene
            4, 9, // ignored because exactly one trailing comma is valid Butylene
            18 // Butylene has a larger depth limit
        );

        for (int i = 1; i <= 34; i++) {
            if (failIgnores.contains(i)) continue;

            String name = "json_net/fail" + i + ".json";
            InputStream is = Objects.requireNonNull(classloader.getResourceAsStream(name), name);
            assertThrows(IOException.class, () -> Parser.fromReader(new InputStreamReader(is), HashConfigNode::new, ArrayConfigList::new), name);
            is.close();
        }
    }
}