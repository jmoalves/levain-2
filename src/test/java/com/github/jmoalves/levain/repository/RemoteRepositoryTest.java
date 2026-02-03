package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

class RemoteRepositoryTest {
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
}
