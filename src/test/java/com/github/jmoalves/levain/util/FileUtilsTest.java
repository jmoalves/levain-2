package com.github.jmoalves.levain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void testResolveAbsolutePath() {
        String absolutePath = "/tmp/test.txt";
        Path result = FileUtils.resolve(null, absolutePath);
        
        assertNotNull(result);
        assertTrue(result.isAbsolute());
    }

    @Test
    void testResolveRelativePath() {
        Path parent = tempDir.resolve("parent");
        String relativePath = "subdir/file.txt";
        
        Path result = FileUtils.resolve(parent, relativePath);
        
        assertNotNull(result);
        assertTrue(result.toString().contains("parent"));
        assertTrue(result.toString().contains("subdir"));
    }

    @Test
    void testResolveHttpUrl() {
        String httpUrl = "http://example.com/file.zip";
        Path result = FileUtils.resolve(tempDir, httpUrl);
        
        assertNotNull(result);
        assertEquals(Paths.get(httpUrl), result);
    }

    @Test
    void testResolveHttpsUrl() {
        String httpsUrl = "https://example.com/file.tar.gz";
        Path result = FileUtils.resolve(tempDir, httpsUrl);
        
        assertNotNull(result);
        assertEquals(Paths.get(httpsUrl), result);
    }

    @Test
    void testIsFileSystemUrlWithNull() {
        assertFalse(FileUtils.isFileSystemUrl(null));
    }

    @Test
    void testIsFileSystemUrlWithHttp() {
        assertFalse(FileUtils.isFileSystemUrl("http://example.com/file"));
    }

    @Test
    void testIsFileSystemUrlWithHttps() {
        assertFalse(FileUtils.isFileSystemUrl("https://example.com/file"));
    }

    @Test
    void testIsFileSystemUrlWithHttpMixedCase() {
        assertFalse(FileUtils.isFileSystemUrl("HTTP://example.com/file"));
        assertFalse(FileUtils.isFileSystemUrl("HtTpS://example.com/file"));
    }

    @Test
    void testIsFileSystemUrlWithFilePath() {
        assertTrue(FileUtils.isFileSystemUrl("/tmp/file.txt"));
        assertTrue(FileUtils.isFileSystemUrl("relative/path/file.txt"));
        assertTrue(FileUtils.isFileSystemUrl("file.txt"));
    }

    @Test
    void testThrowIfNotExistsWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.throwIfNotExists(null);
        });
    }

    @Test
    void testThrowIfNotExistsWithNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");
        
        assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.throwIfNotExists(nonExistent);
        });
    }

    @Test
    void testThrowIfNotExistsWithExistingFile() throws IOException {
        Path existingFile = tempDir.resolve("existing.txt");
        Files.writeString(existingFile, "content");
        
        assertDoesNotThrow(() -> {
            FileUtils.throwIfNotExists(existingFile);
        });
    }

    @Test
    void testThrowIfNotExistsWithDirectory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Files.createDirectory(dir);
        
        assertDoesNotThrow(() -> {
            FileUtils.throwIfNotExists(dir);
        });
    }

    @Test
    void testGetFileNameFromUrlWithSimpleUrl() {
        String url = "https://example.com/downloads/file.zip";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("file.zip", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithComplexPath() {
        String url = "https://example.com/path/to/downloads/archive.tar.gz";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("archive.tar.gz", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithQueryString() {
        String url = "https://example.com/file.zip?version=1.0&user=test";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("file.zip", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithNoPath() {
        String url = "https://example.com";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("download", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithBlankPath() {
        String url = "https://example.com/";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("download", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithInvalidUrl() {
        String url = "not a valid url @@##";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        assertEquals("download", fileName);
    }

    @Test
    void testGetFileNameFromUrlWithTrailingSlash() {
        String url = "https://example.com/downloads/";
        String fileName = FileUtils.getFileNameFromUrl(url);
        
        // Returns the directory name when path ends with /
        assertEquals("downloads", fileName);
    }

    @Test
    void testResolveNormalizesPath() {
        Path parent = tempDir.resolve("parent");
        String pathWithDots = "subdir/../file.txt";
        
        Path result = FileUtils.resolve(parent, pathWithDots);
        
        assertNotNull(result);
        assertFalse(result.toString().contains(".."));
    }

    @Test
    void testResolveWithNullParent() {
        String path = "relative/file.txt";
        Path result = FileUtils.resolve(null, path);
        
        assertNotNull(result);
    }

    @Test
    void testIsFileSystemUrlWithEmptyString() {
        assertTrue(FileUtils.isFileSystemUrl(""));
    }

    @Test
    void testIsFileSystemUrlWithFileProtocol() {
        assertTrue(FileUtils.isFileSystemUrl("file:///tmp/file.txt"));
    }
}
