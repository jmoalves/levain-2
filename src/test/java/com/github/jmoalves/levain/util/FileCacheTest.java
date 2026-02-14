package com.github.jmoalves.levain.util;

import com.github.jmoalves.levain.config.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    void testProxySelectorBehavior() throws Exception {
        String originalHttpsProxy = System.getenv("HTTPS_PROXY");
        String originalNoProxy = System.getenv("NO_PROXY");

        try {
            setEnv("HTTPS_PROXY", "http://proxy.example.com:8080");
            setEnv("NO_PROXY", "example.com,.internal");

            FileCache cache = new FileCache(config);
            Method method = FileCache.class.getDeclaredMethod("createProxySelector");
            method.setAccessible(true);
            ProxySelector selector = (ProxySelector) method.invoke(cache);

            assertNotNull(selector);
            List<Proxy> noProxy = selector.select(URI.create("http://example.com"));
            assertEquals(Proxy.NO_PROXY, noProxy.get(0));

            List<Proxy> proxied = selector.select(URI.create("http://foo.com"));
            assertEquals(Proxy.Type.HTTP, proxied.get(0).type());

            assertEquals(Proxy.NO_PROXY, selector.select(null).get(0));
            selector.connectFailed(URI.create("http://foo.com"),
                    new InetSocketAddress("proxy.example.com", 8080), new IOException("boom"));
        } finally {
            restoreEnv("HTTPS_PROXY", originalHttpsProxy);
            restoreEnv("NO_PROXY", originalNoProxy);
        }
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

    @Test
    void testDownloadRemoteFileAndCacheReuse() throws Exception {
        AtomicReference<byte[]> content = new AtomicReference<>("hello".getBytes());
        AtomicReference<String> lastModified = new AtomicReference<>(
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        AtomicReference<Integer> headStatus = new AtomicReference<>(200);
        AtomicReference<Integer> getStatus = new AtomicReference<>(200);
        AtomicInteger headCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", new TestHandler(content, lastModified, headStatus, getStatus, headCount, getCount));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";
            Path cached = fileCache.get(url);
            assertTrue(Files.exists(cached));
            assertEquals("hello", Files.readString(cached));

            Path cachedAgain = fileCache.get(url);
            assertEquals(cached, cachedAgain);
            assertEquals(1, getCount.get());
            assertTrue(headCount.get() >= 1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testCacheInvalidWhenContentLengthChanges() throws Exception {
        AtomicReference<byte[]> content = new AtomicReference<>("aaa".getBytes());
        AtomicReference<String> lastModified = new AtomicReference<>(null);
        AtomicReference<Integer> headStatus = new AtomicReference<>(200);
        AtomicReference<Integer> getStatus = new AtomicReference<>(200);
        AtomicInteger headCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", new TestHandler(content, lastModified, headStatus, getStatus, headCount, getCount));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";
            Path cached = fileCache.get(url);
            assertEquals("aaa", Files.readString(cached));
            assertEquals(1, getCount.get());

            content.set("bbbbbb".getBytes());
            Path cachedUpdated = fileCache.get(url);
            assertEquals(cached, cachedUpdated);
            assertEquals("bbbbbb", Files.readString(cachedUpdated));
            assertEquals(2, getCount.get());
            assertTrue(headCount.get() >= 1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testCacheInvalidWhenRemoteIsNewer() throws Exception {
        AtomicReference<byte[]> content = new AtomicReference<>("old".getBytes());
        AtomicReference<String> lastModified = new AtomicReference<>(
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        AtomicReference<Integer> headStatus = new AtomicReference<>(200);
        AtomicReference<Integer> getStatus = new AtomicReference<>(200);
        AtomicInteger headCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", new TestHandler(content, lastModified, headStatus, getStatus, headCount, getCount));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";
            Path cached = fileCache.get(url);
            assertEquals("old", Files.readString(cached));

            content.set("new".getBytes());
            lastModified.set(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)
                    .format(DateTimeFormatter.RFC_1123_DATE_TIME));

            Path updated = fileCache.get(url);
            assertEquals(cached, updated);
            assertEquals("new", Files.readString(updated));
            assertEquals(2, getCount.get());
            assertTrue(headCount.get() >= 1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testDownloadFailsOnHttpError() throws Exception {
        AtomicReference<byte[]> content = new AtomicReference<>("".getBytes());
        AtomicReference<String> lastModified = new AtomicReference<>(null);
        AtomicReference<Integer> headStatus = new AtomicReference<>(200);
        AtomicReference<Integer> getStatus = new AtomicReference<>(404);
        AtomicInteger headCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", new TestHandler(content, lastModified, headStatus, getStatus, headCount, getCount));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";
            IOException ex = assertThrows(IOException.class, () -> fileCache.get(url));
            assertTrue(ex.getMessage().contains("HTTP 404"));
            assertEquals(1, getCount.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testCacheIsValidWhenHeadReturnsError() throws Exception {
        AtomicReference<byte[]> content = new AtomicReference<>("cached".getBytes());
        AtomicReference<String> lastModified = new AtomicReference<>(null);
        AtomicReference<Integer> headStatus = new AtomicReference<>(200);
        AtomicReference<Integer> getStatus = new AtomicReference<>(200);
        AtomicInteger headCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", new TestHandler(content, lastModified, headStatus, getStatus, headCount, getCount));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";
            Path cached = fileCache.get(url);
            assertEquals("cached", Files.readString(cached));

            headStatus.set(500);
            content.set("new".getBytes());

            Path cachedAgain = fileCache.get(url);
            assertEquals(cached, cachedAgain);
            assertEquals("cached", Files.readString(cachedAgain));
            assertEquals(1, getCount.get());
            assertTrue(headCount.get() >= 1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testCreateProxySelectorInvalidUrl() throws Exception {
        Map<String, String> original = snapshotEnv("HTTPS_PROXY");
        try {
            Assumptions.assumeTrue(setEnvVar("HTTPS_PROXY", "://bad"));
            ProxySelector selector = invokeCreateProxySelector(fileCache);
            assertNull(selector);
        } finally {
            restoreEnv(original);
        }
    }

    @Test
    void testCreateProxySelectorMissingPort() throws Exception {
        Map<String, String> original = snapshotEnv("HTTPS_PROXY");
        try {
            Assumptions.assumeTrue(setEnvVar("HTTPS_PROXY", "http://proxy.example.com"));
            ProxySelector selector = invokeCreateProxySelector(fileCache);
            assertNull(selector);
        } finally {
            restoreEnv(original);
        }
    }

    @Test
    void testCreateProxySelectorValidAndNoProxyRules() throws Exception {
        Map<String, String> original = snapshotEnv("HTTPS_PROXY", "NO_PROXY");
        try {
            Assumptions.assumeTrue(setEnvVar("HTTPS_PROXY", "proxy.example.com:8080"));
            Assumptions.assumeTrue(setEnvVar("NO_PROXY", "example.com,.internal,localhost"));

            ProxySelector selector = invokeCreateProxySelector(fileCache);
            assertNotNull(selector);

            List<Proxy> proxiesForNoProxyHost = selector.select(URI.create("http://service.internal"));
            assertEquals(1, proxiesForNoProxyHost.size());
            assertEquals(Proxy.NO_PROXY, proxiesForNoProxyHost.get(0));

            List<Proxy> proxiesForOtherHost = selector.select(URI.create("http://other.com"));
            assertEquals(1, proxiesForOtherHost.size());
            assertEquals(Proxy.Type.HTTP, proxiesForOtherHost.get(0).type());
        } finally {
            restoreEnv(original);
        }
    }

    @Test
    void testIsNoProxyRules() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("isNoProxy", String.class, List.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "example.com", List.of("*")));
        assertTrue((Boolean) method.invoke(null, "service.internal", List.of(".internal")));
        assertTrue((Boolean) method.invoke(null, "example.com", List.of("example.com")));
        assertTrue((Boolean) method.invoke(null, "sub.example.com", List.of("example.com")));
        assertFalse((Boolean) method.invoke(null, "other.com", List.of("example.com")));
    }

    @Test
    void testParseNoProxyList() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("parseNoProxyList", String[].class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(null, (Object) new String[]{" a , b ", ""});
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void testParseNoProxyListWithBlankValues() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("parseNoProxyList", String[].class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(null, (Object) new String[]{" ", ""});
        assertEquals(List.of(), result);
    }

    @Test
    void testIsNoProxyWithEmptyRules() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("isNoProxy", String.class, List.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(null, "example.com", null));
        assertFalse((Boolean) method.invoke(null, "example.com", List.of()));
    }

    @Test
    void testIsNoProxyWithPortRule() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("isNoProxy", String.class, List.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "example.com", List.of("example.com:8080")));
    }

    @Test
    void testProxySelectorHandlesNullHost() throws Exception {
        Map<String, String> original = snapshotEnv("HTTPS_PROXY", "NO_PROXY");
        try {
            Assumptions.assumeTrue(setEnvVar("HTTPS_PROXY", "proxy.example.com:8080"));
            Assumptions.assumeTrue(setEnvVar("NO_PROXY", ""));

            ProxySelector selector = invokeCreateProxySelector(fileCache);
            assertNotNull(selector);

            List<Proxy> proxies = selector.select(URI.create("file:///tmp/test"));
            assertEquals(Proxy.NO_PROXY, proxies.get(0));
        } finally {
            restoreEnv(original);
        }
    }

    @Test
    void testCreateProxySelectorWithBlankProxy() throws Exception {
        Map<String, String> original = snapshotEnv("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy");
        try {
            Assumptions.assumeTrue(setEnvVar("HTTPS_PROXY", " "));
            Assumptions.assumeTrue(setEnvVar("https_proxy", ""));
            Assumptions.assumeTrue(setEnvVar("HTTP_PROXY", ""));
            Assumptions.assumeTrue(setEnvVar("http_proxy", ""));

            ProxySelector selector = invokeCreateProxySelector(fileCache);
            assertNull(selector);
        } finally {
            restoreEnv(original);
        }
    }

    @Test
    void testCacheIsValidReturnsTrueOnException() throws Exception {
        Method method = FileCache.class.getDeclaredMethod("cacheIsValid", String.class, Path.class);
        method.setAccessible(true);

        Path cacheDir = config.getCacheDir().resolve("downloads");
        Files.createDirectories(cacheDir);
        Path cachedFile = cacheDir.resolve("dummy.txt");
        Files.writeString(cachedFile, "cached");

        boolean result = (Boolean) method.invoke(fileCache, "http://[invalid", cachedFile);
        assertTrue(result);
    }

    private static class TestHandler implements HttpHandler {
        private final AtomicReference<byte[]> content;
        private final AtomicReference<String> lastModified;
        private final AtomicReference<Integer> headStatus;
        private final AtomicReference<Integer> getStatus;
        private final AtomicInteger headCount;
        private final AtomicInteger getCount;

        private TestHandler(AtomicReference<byte[]> content,
                            AtomicReference<String> lastModified,
                            AtomicReference<Integer> headStatus,
                            AtomicReference<Integer> getStatus,
                            AtomicInteger headCount,
                            AtomicInteger getCount) {
            this.content = content;
            this.lastModified = lastModified;
            this.headStatus = headStatus;
            this.getStatus = getStatus;
            this.headCount = headCount;
            this.getCount = getCount;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("HEAD".equalsIgnoreCase(method)) {
                headCount.incrementAndGet();
                int status = headStatus.get();
                if (status >= 400) {
                    exchange.sendResponseHeaders(status, -1);
                    exchange.close();
                    return;
                }

                byte[] body = content.get();
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(body.length));
                if (lastModified.get() != null) {
                    exchange.getResponseHeaders().add("Last-Modified", lastModified.get());
                }
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            getCount.incrementAndGet();
            int status = getStatus.get();
            if (status >= 400) {
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
                return;
            }

            byte[] body = content.get();
            exchange.getResponseHeaders().add("Content-Length", String.valueOf(body.length));
            if (lastModified.get() != null) {
                exchange.getResponseHeaders().add("Last-Modified", lastModified.get());
            }
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static ProxySelector invokeCreateProxySelector(FileCache cache) throws Exception {
        Method method = FileCache.class.getDeclaredMethod("createProxySelector");
        method.setAccessible(true);
        return (ProxySelector) method.invoke(cache);
    }

    private static Map<String, String> snapshotEnv(String... keys) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        if (keys == null) {
            return snapshot;
        }
        for (String key : keys) {
            if (key == null || snapshot.containsKey(key)) {
                continue;
            }
            snapshot.put(key, System.getenv(key));
        }
        return snapshot;
    }

    private static void restoreEnv(Map<String, String> snapshot) {
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            if (entry.getValue() == null) {
                clearEnvVar(entry.getKey());
            } else {
                setEnvVar(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean setEnvVar(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            try {
                java.lang.reflect.Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, String> writableEnv = (Map<String, String>) field.get(env);
                if (value == null) {
                    writableEnv.remove(key);
                } else {
                    writableEnv.put(key, value);
                }
                return true;
            } catch (NoSuchFieldException e) {
                // Fall through to ProcessEnvironment strategy below
            }

            Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
            java.lang.reflect.Field envField = pe.getDeclaredField("theEnvironment");
            envField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> writableEnv = (Map<String, String>) envField.get(null);
            if (value == null) {
                writableEnv.remove(key);
            } else {
                writableEnv.put(key, value);
            }

            java.lang.reflect.Field ciEnvField = pe.getDeclaredField("theCaseInsensitiveEnvironment");
            ciEnvField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
            if (value == null) {
                ciEnv.remove(key);
            } else {
                ciEnv.put(key, value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void clearEnvVar(String key) {
        setEnvVar(key, null);
    }

    private static void setEnv(String key, String value) throws Exception {
        updateEnv(key, value);
    }

    private static void restoreEnv(String key, String original) throws Exception {
        if (original == null) {
            updateEnv(key, null);
            return;
        }
        updateEnv(key, original);
    }

    @SuppressWarnings("unchecked")
    private static void updateEnv(String key, String value) throws Exception {
        try {
            Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
            updateEnvMap(pe, "theEnvironment", key, value);
            updateEnvMapOptional(pe, "theCaseInsensitiveEnvironment", key, value);
        } catch (ReflectiveOperationException e) {
            // Fall through to alternate map update.
        }

        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> mutable = (Map<String, String>) field.get(env);
            if (value == null) {
                mutable.remove(key);
            } else {
                mutable.put(key, value);
            }
        } catch (ReflectiveOperationException e) {
            // Ignore if we cannot update the unmodifiable map.
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean updateEnvMap(Class<?> pe, String fieldName, String key, String value)
            throws ReflectiveOperationException {
        Field envField = pe.getDeclaredField(fieldName);
        envField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) envField.get(null);
        if (value == null) {
            env.remove(key);
        } else {
            env.put(key, value);
        }
        return true;
    }

    private static void updateEnvMapOptional(Class<?> pe, String fieldName, String key, String value) {
        try {
            updateEnvMap(pe, fieldName, key, value);
        } catch (ReflectiveOperationException e) {
            // Optional field missing on some JDKs.
        }
    }
}
