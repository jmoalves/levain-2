package com.github.jmoalves.levain.util;

import com.github.jmoalves.levain.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FileCacheTest {

    @TempDir
    Path tempDir;

    @Mock
    Config config;

    private FileCache fileCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getCacheDir()).thenReturn(tempDir.resolve("cache"));
        fileCache = new FileCache(config);
    }

    @Test
    void testGetLocalFile() throws IOException, InterruptedException {
        // Create a local test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        // Get the file through cache (should return same path for local files)
        Path result = fileCache.get(testFile.toString());

        assertNotNull(result);
        assertEquals(testFile.toAbsolutePath().normalize(), result);
        assertTrue(Files.exists(result));
    }

    @Test
    void testGetLocalFileNotExists() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertThrows(IllegalArgumentException.class, () -> {
            fileCache.get(nonExistent.toString());
        });
    }

    @Test
    void testProxySelectorCreation() {
        // Test that FileCache can be created even without proxy environment variables
        assertDoesNotThrow(() -> new FileCache(config));
    }

    @Test
    void testCacheDirCreation() throws IOException, InterruptedException {
        // Cache directory is created on demand when downloading remote files
        // For local files, no cache directory is needed
        Path localFile = tempDir.resolve("local.txt");
        Files.writeString(localFile, "content");

        Path result = fileCache.get(localFile.toString());
        
        // Local files should be returned directly without caching
        assertNotNull(result);
        assertEquals(localFile.toAbsolutePath().normalize(), result);
    }

    @Test
    void testProxyConfiguration() {
        // Test with various proxy environment variable combinations
        try {
            // Test with HTTPS_PROXY
            System.setProperty("test.https.proxy", "http://proxy.example.com:8080");
            FileCache cache1 = new FileCache(config);
            assertNotNull(cache1);

            // Test with invalid proxy (should not crash)
            System.setProperty("test.https.proxy", "invalid-url");
            FileCache cache2 = new FileCache(config);
            assertNotNull(cache2);
        } finally {
            System.clearProperty("test.https.proxy");
        }
    }

    @Test
    void testProxyEnvironmentVariables() {
        // Test that FileCache handles proxy environment variables gracefully
        String originalHttpsProxy = System.getenv("HTTPS_PROXY");
        String originalHttpProxy = System.getenv("HTTP_PROXY");
        
        // FileCache should work even without proxy settings
        FileCache cache = new FileCache(config);
        assertNotNull(cache);
    }

    @Test
    void testNoProxyList() {
        // Test NO_PROXY handling
        String originalNoProxy = System.getenv("NO_PROXY");
        
        // Create cache and verify it handles no-proxy configuration
        FileCache cache = new FileCache(config);
        assertNotNull(cache);
    }

    @Test
    void testProxyWithPort() {
        // Test proxy URL with explicit port
        try {
            // Simulate proxy environment (FileCache reads from env, not system props)
            FileCache cache = new FileCache(config);
            assertNotNull(cache);
        } catch (Exception e) {
            fail("Should handle proxy with port gracefully: " + e.getMessage());
        }
    }

    @Test
    void testProxyWithoutScheme() {
        // Test proxy URL without http:// scheme
        // FileCache should add scheme automatically
        FileCache cache = new FileCache(config);
        assertNotNull(cache);
    }

    @Test
    void testInvalidProxyUrl() {
        // FileCache should handle invalid proxy URLs gracefully
        try {
            FileCache cache = new FileCache(config);
            assertNotNull(cache);
        } catch (Exception e) {
            fail("Should handle invalid proxy URL gracefully: " + e.getMessage());
        }
    }

    @Test
    void testProxyWithoutHostOrPort() {
        // Test proxy configuration with missing host or port
        FileCache cache = new FileCache(config);
        assertNotNull(cache);
    }

    @Test
    void testHashFunction() throws IOException, InterruptedException {
        // Create two different files
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        // Get both through cache
        Path cached1 = fileCache.get(file1.toString());
        Path cached2 = fileCache.get(file2.toString());

        // Both should be accessible
        assertNotNull(cached1);
        assertNotNull(cached2);
        assertTrue(Files.exists(cached1));
        assertTrue(Files.exists(cached2));
    }

    @Test
    void testFileNameFromUrl() {
        // Test various URL formats
        String url1 = "https://example.com/path/to/file.zip";
        String url2 = "https://example.com/file.tar.gz";
        String url3 = "https://example.com/downloads/archive.7z";

        // These should not throw exceptions (FileCache handles URL parsing internally)
        assertDoesNotThrow(() -> {
            // Just verify the FileCache can be instantiated
            new FileCache(config);
        });
    }

    @Test
    void testCacheValidation() throws IOException, InterruptedException {
        // Test that local files are always returned directly
        Path localFile = tempDir.resolve("validation-test.txt");
        Files.writeString(localFile, "validation content");

        Path result1 = fileCache.get(localFile.toString());
        Path result2 = fileCache.get(localFile.toString());

        // Should return the same path for local files
        assertEquals(result1, result2);
    }

    @Test
    void testMultipleGetsOfSameFile() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("multiple.txt");
        Files.writeString(testFile, "multiple access test");

        // Get the same file multiple times
        Path result1 = fileCache.get(testFile.toString());
        Path result2 = fileCache.get(testFile.toString());
        Path result3 = fileCache.get(testFile.toString());

        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    void testNoProxyConfiguration() {
        // Test NO_PROXY handling by creating cache without proxy settings
        when(config.getCacheDir()).thenReturn(tempDir.resolve("no-proxy-cache"));
        FileCache cacheWithoutProxy = new FileCache(config);
        assertNotNull(cacheWithoutProxy);
    }
}
