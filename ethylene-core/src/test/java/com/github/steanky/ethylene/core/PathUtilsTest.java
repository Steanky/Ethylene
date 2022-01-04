package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUtilsTest {
    private static final Path SIMPLE_FILE_PATH = Path.of(URI.create("file:///C:/Users/User/test_file.txt"));
    private static final String SIMPLE_FILE_EXTENSION = "txt";
    private static final String SIMPLE_FILE_NAME = "test_file.txt";
    private static final String SIMPLE_FILE_NAME_WITHOUT_EXTENSION = "test_file";

    private static final Path SIMPLE_EXTENSIONLESS_FILE_PATH = Path.of(URI.create("file:///C:/Users/User/test_file"));
    private static final Path EMPTY_PATH = Path.of(URI.create("file:///"));

    @Test
    void simpleFileExtension() {
        assertEquals(SIMPLE_FILE_EXTENSION, PathUtils.getFileExtension(SIMPLE_FILE_PATH));
    }

    @Test
    void noExtension() {
        assertTrue(PathUtils.getFileExtension(SIMPLE_EXTENSIONLESS_FILE_PATH).isEmpty());
    }

    @Test
    void emptyPathExtension() {
        assertTrue(PathUtils.getFileExtension(EMPTY_PATH).isEmpty());
    }

    @Test
    void simpleFileName() {
        assertEquals(SIMPLE_FILE_NAME, PathUtils.getFileName(SIMPLE_FILE_PATH));
    }

    @Test
    void simpleFileNameWithoutExtension() {
        assertEquals(SIMPLE_FILE_NAME_WITHOUT_EXTENSION, PathUtils.getFileNameWithoutExtension(SIMPLE_FILE_PATH));
    }

    @Test
    void simpleFileNameWithoutExtensionOnExtensionlessPath() {
        assertEquals(SIMPLE_FILE_NAME_WITHOUT_EXTENSION,
                PathUtils.getFileNameWithoutExtension(SIMPLE_EXTENSIONLESS_FILE_PATH));
    }
}