package com.github.jmoalves.levain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for Registry.
 */
@DisplayName("Registry Tests")
class RegistryTest {

    @TempDir
    Path tempDir;

    private Registry registry;

    @BeforeEach
    void setUp() {
        registry = new Registry(tempDir.toString());
        registry.init();
    }

    @Test
    @DisplayName("Should initialize registry successfully")
    void shouldInitializeRegistry() {
        assertTrue(registry.isInitialized(), "Registry should be initialized");
        assertTrue(Files.exists(tempDir), "Registry directory should exist");
    }

    @Test
    @DisplayName("Should describe itself")
    void shouldDescribeSelf() {
        assertNotNull(registry.describe());
        assertTrue(registry.describe().contains("Registry"));
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
        assertEquals("registry", registry.getName());
    }

    @Test
    @DisplayName("Should return registry URI")
    void shouldReturnRegistryUri() {
        assertTrue(registry.getUri().startsWith("registry://"));
    }

    @Test
    @DisplayName("Should have zero recipes initially")
    void shouldHaveZeroRecipesInitially() {
        assertEquals(0, registry.size());
        assertTrue(registry.listRecipes().isEmpty());
    }

    @Test
    @DisplayName("Should store recipe in registry")
    void shouldStoreRecipeInRegistry() {
        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        String yamlContent = """
                name: jdk-21
                version: 21.0.0
                description: Java Development Kit 21
                commands:
                  install:
                    - echo "Installing JDK 21"
                """;

        registry.store(recipe, yamlContent);

        assertTrue(registry.isInstalled("jdk-21"), "Recipe should be installed");
        assertTrue(Files.exists(tempDir.resolve("jdk-21.levain.yaml")), "Recipe file should exist");
    }

    @Test
    @DisplayName("Migration: legacy install without metadata should still work")
    void migrationLegacyInstallWithoutMetadataShouldWork() throws Exception {
        String yamlContent = """
                name: legacy
                version: 1.0.0
                """;
        Files.writeString(tempDir.resolve("legacy.levain.yaml"), yamlContent);

        assertTrue(registry.isInstalled("legacy"));
        assertTrue(registry.getMetadata("legacy").isEmpty());
        assertTrue(registry.resolveRecipe("legacy").isPresent());
    }

    @Test
    @DisplayName("Migration: should read metadata when present")
    void migrationShouldReadMetadataWhenPresent() throws Exception {
        String yamlContent = """
                name: git
                version: 2.45.0
                """;
        Files.writeString(tempDir.resolve("git.levain.yaml"), yamlContent);

        String metadataJson = """
                {
                  "recipeName": "git",
                  "sourceRepository": "GitRepository",
                  "sourceRepositoryUri": "https://example.com/levain-pkgs.git",
                  "installedAt": "2026-02-03T00:00:00Z",
                  "installedVersion": "2.45.0"
                }
                """;
        Files.writeString(tempDir.resolve("git.levain.meta"), metadataJson);

        var metadata = registry.getMetadata("git");
        assertTrue(metadata.isPresent());
        assertEquals("GitRepository", metadata.get().getSourceRepository());
        assertEquals("https://example.com/levain-pkgs.git", metadata.get().getSourceRepositoryUri());
        assertEquals("2.45.0", metadata.get().getInstalledVersion());
    }

    @Test
    @DisplayName("Should list stored recipes")
    void shouldListStoredRecipes() {
        storeTestRecipes();

        var recipes = registry.listRecipes();
        assertEquals(2, recipes.size());
        assertTrue(recipes.stream().anyMatch(r -> r.getName().equals("jdk-21")));
        assertTrue(recipes.stream().anyMatch(r -> r.getName().equals("git")));
    }

    @Test
    @DisplayName("Should resolve recipe from registry")
    void shouldResolveRecipeFromRegistry() {
        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        String yamlContent = "name: jdk-21\nversion: 21.0.0\n";

        registry.store(recipe, yamlContent);

        var result = registry.resolveRecipe("jdk-21");
        assertTrue(result.isPresent());
        assertEquals("jdk-21", result.get().getName());
    }

    @Test
    @DisplayName("Should return empty Optional for missing recipe")
    void shouldReturnEmptyOptionalForMissingRecipe() {
        var result = registry.resolveRecipe("non-existent");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should check if recipe is installed")
    void shouldCheckIfRecipeIsInstalled() {
        assertFalse(registry.isInstalled("jdk-21"), "Recipe should not be installed initially");

        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe, "name: jdk-21\nversion: 21.0.0\n");

        assertTrue(registry.isInstalled("jdk-21"), "Recipe should be installed after store");
    }

    @Test
    @DisplayName("Should get recipe path from registry")
    void shouldGetRecipePathFromRegistry() {
        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe, "name: jdk-21\nversion: 21.0.0\n");

        var path = registry.getRecipePath("jdk-21");
        assertTrue(path.isPresent());
        assertTrue(Files.exists(path.get()));
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent recipe path")
    void shouldReturnEmptyOptionalForNonExistentRecipePath() {
        var path = registry.getRecipePath("non-existent");
        assertFalse(path.isPresent());
    }

    @Test
    @DisplayName("Should remove recipe from registry")
    void shouldRemoveRecipeFromRegistry() {
        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe, "name: jdk-21\nversion: 21.0.0\n");

        assertTrue(registry.isInstalled("jdk-21"));

        boolean removed = registry.remove("jdk-21");
        assertTrue(removed, "Recipe should be removed");
        assertFalse(registry.isInstalled("jdk-21"));
    }

    @Test
    @DisplayName("Should return false when removing non-existent recipe")
    void shouldReturnFalseWhenRemovingNonExistent() {
        boolean removed = registry.remove("non-existent");
        assertFalse(removed);
    }

    @Test
    @DisplayName("Should clear all recipes from registry")
    void shouldClearAllRecipesFromRegistry() {
        storeTestRecipes();
        assertEquals(2, registry.size());

        registry.clear();
        assertEquals(0, registry.size());
        assertTrue(registry.listRecipes().isEmpty());
    }

    @Test
    @DisplayName("Should return registry path")
    void shouldReturnRegistryPath() {
        assertNotNull(registry.getRegistryPath());
        assertEquals(tempDir.toAbsolutePath(), registry.getRegistryPath());
    }

    @Test
    @DisplayName("Should support .levain.yaml extension")
    void shouldSupportYamlExtension() {
        Recipe recipe = createRecipe("maven", "3.9.0");
        String yamlContent = "name: maven\nversion: 3.9.0\n";

        // Manually create .levain.yaml file
        try {
            Files.writeString(tempDir.resolve("maven.levain.yaml"), yamlContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertTrue(registry.isInstalled("maven"));
        var result = registry.resolveRecipe("maven");
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should return YAML content for stored recipe")
    void shouldReturnYamlContentForStoredRecipe() {
        Recipe recipe = createRecipe("gradle", "8.5");
        String yamlContent = "name: gradle\nversion: 8.5\n";

        registry.store(recipe, yamlContent);

        var content = registry.getRecipeYamlContent("gradle");
        assertTrue(content.isPresent());
        assertTrue(content.get().contains("name: gradle"));
    }

    @Test
    @DisplayName("Should return empty YAML content for missing recipe")
    void shouldReturnEmptyYamlContentForMissingRecipe() {
        var content = registry.getRecipeYamlContent("missing-recipe");
        assertTrue(content.isEmpty());
    }

    @Test
    @DisplayName("Should return empty filename when recipe name includes extension")
    void shouldReturnEmptyFileNameWhenNameIncludesExtension() {
        var fileName = registry.getRecipeFileName("bad.levain.yaml");
        assertTrue(fileName.isEmpty());
    }

    @Test
    @DisplayName("Should return empty metadata when JSON is invalid")
    void shouldReturnEmptyMetadataWhenJsonInvalid() throws Exception {
        Files.writeString(tempDir.resolve("bad.levain.meta"), "{bad json");

        var metadata = registry.getMetadata("bad");
        assertTrue(metadata.isEmpty());
    }

    @Test
    @DisplayName("Should store with standardized extension even when filename provided")
    void shouldStoreWithStandardExtensionWhenFilenameProvided() {
        Recipe recipe = createRecipe("node", "20.11.0");
        String yamlContent = "name: node\nversion: 20.11.0\n";

        registry.store(recipe, yamlContent, "node.yaml");

        assertTrue(Files.exists(tempDir.resolve("node.levain.yaml")));
    }

    @Test
    @DisplayName("Should reject recipe name containing .levain.yaml")
    void shouldRejectRecipeNameWithExtension() {
        Recipe recipe = createRecipe("bad.levain.yaml", "1.0.0");
        String yamlContent = "name: bad\nversion: 1.0.0\n";

        assertThrows(IllegalArgumentException.class, () -> registry.store(recipe, yamlContent));
    }

    @Test
    @DisplayName("Should skip invalid YAML when listing recipes")
    void shouldSkipInvalidYamlWhenListingRecipes() throws Exception {
        Recipe recipe = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe, "name: jdk-21\nversion: 21.0.0\n");

        Files.writeString(tempDir.resolve("broken.levain.yaml"), "invalid: [");

        var recipes = registry.listRecipes();
        assertEquals(1, recipes.size());
        assertTrue(recipes.stream().anyMatch(r -> r.getName().equals("jdk-21")));
    }

    @Test
    @DisplayName("Should return zero size when registry path is a file")
    void shouldReturnZeroSizeWhenRegistryPathIsFile() throws Exception {
        Path filePath = tempDir.resolve("not-a-dir");
        Files.writeString(filePath, "content");

        Registry fileRegistry = new Registry(filePath.toString());
        assertEquals(0, fileRegistry.size());
    }

    @Test
    @DisplayName("Should expose default registry path")
    void shouldExposeDefaultRegistryPath() {
        String defaultPath = Registry.getDefaultRegistryPath();
        assertNotNull(defaultPath);
        assertTrue(defaultPath.endsWith("/.levain/registry"));
    }

    @Test
    @DisplayName("Should count recipes correctly")
    void shouldCountRecipesCorrectly() {
        assertEquals(0, registry.size());

        Recipe recipe1 = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe1, "name: jdk-21\nversion: 21.0.0\n");
        assertEquals(1, registry.size());

        Recipe recipe2 = createRecipe("git", "2.45.0");
        registry.store(recipe2, "name: git\nversion: 2.45.0\n");
        assertEquals(2, registry.size());
    }

    @Test
    @DisplayName("Should initialize automatically on first access")
    void shouldInitializeAutomaticallyOnFirstAccess() {
        Registry newRegistry = new Registry(tempDir.toString());
        assertFalse(newRegistry.isInitialized());

        // Accessing recipes should trigger auto-initialization
        int size = newRegistry.size();
        assertEquals(0, size);
    }

    private void storeTestRecipes() {
        Recipe recipe1 = createRecipe("jdk-21", "21.0.0");
        registry.store(recipe1, "name: jdk-21\nversion: 21.0.0\n");

        Recipe recipe2 = createRecipe("git", "2.45.0");
        registry.store(recipe2, "name: git\nversion: 2.45.0\n");
    }

    private Recipe createRecipe(String name, String version) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setVersion(version);
        recipe.setDescription("Test recipe for " + name);
        return recipe;
    }
}
