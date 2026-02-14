package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

class ZipRepositoryTest {
    private ZipRepository repository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a repository for a test ZIP (won't initialize in unit tests)
        repository = new ZipRepository("https://github.com/jmoalves/levain-pkgs/archive/refs/heads/main.zip");
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertTrue(repository.getName().contains("Zip"));
        assertTrue(repository.getUri().contains(".zip") || repository.getUri().contains("levain-pkgs"));
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertTrue(description.contains("ZipRepository"));
    }

    @Test
    void shouldBeInitializable() {
        assertFalse(repository.isInitialized());
        // Don't actually initialize in unit tests since it requires downloads
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
    void shouldReturnEmptyFileNameForInvalidName() {
        assertTrue(repository.getRecipeFileName("bad.levain.yaml").isEmpty());
    }

    @Test
    void shouldReturnStandardFileNameForValidName() {
        assertEquals("jdk-21.levain.yaml", repository.getRecipeFileName("jdk-21").orElse(""));
    }

    @Test
    void shouldReturnEmptyYamlContentWhenUninitialized() {
        assertTrue(repository.getRecipeYamlContent("jdk-21").isEmpty());
    }

    @Test
    void shouldInitializeWhenCacheAlreadyExtracted() throws Exception {
        Path zipFile = tempDir.resolve("recipes.zip");
        createZipWithRecipe(zipFile, "ignored.txt", "data");

        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache").toString());
        try {
            ZipRepository localRepo = new ZipRepository(zipFile.toString());
            Path cacheDir = Path.of(getLocalCachePath(localRepo));
            Files.createDirectories(cacheDir);
            Files.writeString(cacheDir.resolve("jdk-21.levain.yaml"), "name: jdk-21\nversion: 21.0.0\n");

            localRepo.init();

            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("jdk-21")));
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldDownloadRemoteZipFile() throws Exception {
        Path zipFile = tempDir.resolve("remote.zip");
        createZipWithRecipe(zipFile, "git.levain.yaml", "name: git\nversion: 2.0.0\n");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/archive.zip", exchange -> {
            byte[] content = Files.readAllBytes(zipFile);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        });
        server.start();

        String originalHome = System.getProperty("user.home");
        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("user.home", tempDir.toString());
        System.setProperty("levain.cache.dir", tempDir.resolve("cache-remote").toString());

        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/archive.zip";
            ZipRepository remoteRepo = new ZipRepository(url);
            remoteRepo.init();

            assertTrue(remoteRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("git")));
        } finally {
            server.stop(0);
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            } else {
                System.clearProperty("user.home");
            }
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldDownloadRemoteZipWithoutFilename() throws Exception {
        Path zipFile = tempDir.resolve("remote-no-name.zip");
        createZipWithRecipe(zipFile, "node.levain.yaml", "name: node\nversion: 20.0.0\n");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] content = Files.readAllBytes(zipFile);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        });
        server.start();

        String originalHome = System.getProperty("user.home");
        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("user.home", tempDir.toString());
        System.setProperty("levain.cache.dir", tempDir.resolve("cache-remote").toString());

        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/";
            ZipRepository remoteRepo = new ZipRepository(url);
            remoteRepo.init();

            assertTrue(remoteRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("node")));
        } finally {
            server.stop(0);
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            } else {
                System.clearProperty("user.home");
            }
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldExtractZipWithDirectoryEntry() throws Exception {
        Path zipFile = tempDir.resolve("dir.zip");
        Files.deleteIfExists(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry dir = new ZipEntry("recipes/");
            zos.putNextEntry(dir);
            zos.closeEntry();

            ZipEntry entry = new ZipEntry("recipes/demo.levain.yaml");
            zos.putNextEntry(entry);
            zos.write("name: demo\nversion: 1.0.0\n".getBytes());
            zos.closeEntry();
        }

        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache-dir").toString());
        try {
            ZipRepository localRepo = new ZipRepository(zipFile.toString());
            localRepo.init();

            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("demo")));
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldExtractWhenCacheDirExistsButEmpty() throws Exception {
        Path zipFile = tempDir.resolve("empty-cache.zip");
        createZipWithRecipe(zipFile, "python.levain.yaml", "name: python\nversion: 3.12.0\n");

        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache-empty").toString());
        try {
            ZipRepository localRepo = new ZipRepository(zipFile.toString());
            Path cacheDir = Path.of(getLocalCachePath(localRepo));
            Files.createDirectories(cacheDir);

            localRepo.init();

            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("python")));
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldHandleExtractZipEntryFailure() throws Exception {
        Path zipFile = tempDir.resolve("broken.zip");
        Files.deleteIfExists(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("file.txt");
            zos.putNextEntry(entry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        Path targetFile = tempDir.resolve("target-file");
        Files.writeString(targetFile, "not a dir");

        ZipRepository repo = new ZipRepository(zipFile.toString());
        java.lang.reflect.Method method = ZipRepository.class.getDeclaredMethod("extractZip", File.class, File.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(repo, zipFile.toFile(), targetFile.toFile()));
    }

    @Test
    void shouldReturnEmptyWhenLocalRepositoryMissing() throws Exception {
        ZipRepository repo = new ZipRepository("missing.zip");
        java.lang.reflect.Method method = ZipRepository.class.getDeclaredMethod("loadRecipesFromLocalDirectory");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Recipe> recipes = (java.util.Map<String, Recipe>) method.invoke(repo);

        assertTrue(recipes.isEmpty());
    }

    @Test
    void shouldInitializeFromLocalZip() throws Exception {
        Path zipFile = tempDir.resolve("recipes.zip");
        createZipWithRecipe(zipFile, "jdk-21.levain.yaml", "name: jdk-21\nversion: 21.0.0\n");

        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache").toString());
        try {
            ZipRepository localRepo = new ZipRepository(zipFile.toString());
            localRepo.init();

            assertTrue(localRepo.isInitialized());
            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("jdk-21")));
            assertTrue(localRepo.getRecipeYamlContent("jdk-21").isPresent());
            assertTrue(localRepo.describe().contains("->"));
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    @Test
    void shouldHandleMissingZipFile() {
        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache-missing").toString());
        try {
            ZipRepository missingRepo = new ZipRepository(tempDir.resolve("missing.zip").toString());
            missingRepo.init();
            assertTrue(missingRepo.isInitialized());
            assertTrue(missingRepo.listRecipes().isEmpty());
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    private static void createZipWithRecipe(Path zipPath, String entryName, String content) throws Exception {
        Files.deleteIfExists(zipPath);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
    }

    private static String getLocalCachePath(ZipRepository repository) throws Exception {
        Field field = ZipRepository.class.getDeclaredField("localCachePath");
        field.setAccessible(true);
        return (String) field.get(repository);
    }
}
