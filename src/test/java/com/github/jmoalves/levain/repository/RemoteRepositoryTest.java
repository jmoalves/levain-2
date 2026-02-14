package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

class RemoteRepositoryTest {
    @TempDir
    Path tempDir;

    private RemoteRepository repository;

    @BeforeEach
    void setUp() {
        // Create a repository for testing GitHub URLs (won't initialize in unit tests)
        repository = new RemoteRepository("https://github.com/jmoalves/levain-pkgs");
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertTrue(repository.getName().contains("Remote"));
        assertTrue(repository.getUri().contains("github"));
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertTrue(description.contains("RemoteRepository"));
    }

    @Test
    void shouldBeInitializable() {
        assertFalse(repository.isInitialized());
        // Don't actually initialize in unit tests since it requires network
    }

    @Test
    void shouldReturnEmptyListWhenNotInitialized() {
        List<Recipe> recipes = repository.listRecipes();
        assertNotNull(recipes);
    }

    @Test
    void shouldReturnEmptyForUnknownRecipe() {
        Optional<Recipe> recipe = repository.resolveRecipe("unknown-recipe");
        // Should be empty since we didn't initialize
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldSizeReturnZeroWhenNotInitialized() {
        assertEquals(0, repository.size());
    }

    @Test
    void shouldInitializeFromLocalHttpServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/recipes/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("jdk-21.levain.yaml")) {
                byte[] body = "name: jdk-21\nversion: 21.0.0\n".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            RemoteRepository localRepo = new RemoteRepository("http://localhost:" + port);
            localRepo.init();

            assertTrue(localRepo.isInitialized());
            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("jdk-21")));
            assertTrue(localRepo.getRecipeYamlContent("jdk-21").isEmpty());
            assertTrue(localRepo.getRecipeFileName("jdk-21").isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldNormalizeGithubUrlsToRawRecipes() throws Exception {
        String normalized = invokeNormalizeUrl("https://github.com/acme/repo/tree/main");

        assertEquals("https://raw.githubusercontent.com/acme/repo/main/recipes", normalized);
    }

    @Test
    void shouldNormalizeDirectoryUrls() throws Exception {
        String normalized = invokeNormalizeUrl("https://example.com/base");
        String normalizedWithSlash = invokeNormalizeUrl("https://example.com/base/");

        assertEquals("https://example.com/base/recipes", normalized);
        assertEquals("https://example.com/base/recipes/", normalizedWithSlash);
    }

    @Test
    void shouldExtractRecipeNameFromUrl() throws Exception {
        String name = invokeExtractRecipeName("https://example.com/jdk-21.levain.yaml");

        assertEquals("jdk-21", name);
    }

    @Test
    void shouldCreateDefaultCacheDirectory() throws Exception {
        String original = System.getProperty("levain.cache.dir");
        try {
            System.setProperty("levain.cache.dir", tempDir.toString());
            String cacheDir = invokeGetDefaultCacheDirectory("https://example.com/repo");

            assertTrue(cacheDir.startsWith(tempDir.toString()));
            assertTrue(java.nio.file.Files.exists(Path.of(cacheDir)));
        } finally {
            if (original == null) {
                System.clearProperty("levain.cache.dir");
            } else {
                System.setProperty("levain.cache.dir", original);
            }
        }
    }

    private String invokeNormalizeUrl(String url) throws Exception {
        Method method = RemoteRepository.class.getDeclaredMethod("normalizeUrl", String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, url);
    }

    private String invokeExtractRecipeName(String url) throws Exception {
        Method method = RemoteRepository.class.getDeclaredMethod("extractRecipeName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, url);
    }

    private String invokeGetDefaultCacheDirectory(String url) throws Exception {
        Method method = RemoteRepository.class.getDeclaredMethod("getDefaultCacheDirectory", String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, url);
    }
}
