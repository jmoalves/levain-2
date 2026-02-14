package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
}
