package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryManagerTest {
    @TempDir
    Path tempDir;

    private RepositoryManager manager;

    @BeforeEach
    void setUp() {
        manager = new RepositoryManager();
    }

    @Test
    void shouldAddRepositories() {
        Repository repo1 = new DirectoryRepository("Repo1", "src/test/resources/recipes");
        manager.addRepository(repo1);
        assertEquals(1, manager.getRepositoryCount());
    }

    @Test
    void shouldInitializeAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        assertTrue(resourceRepo.isInitialized());
        assertTrue(dirRepo.isInitialized());
    }

    @Test
    void shouldListRecipesFromAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        List<Recipe> recipes = manager.listRecipes();
        assertFalse(recipes.isEmpty());
    }

    @Test
    void shouldResolveRecipesFromMultipleRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        // Should find jdk-21 from directory repo
        Optional<Recipe> jdk = manager.resolveRecipe("jdk-21");
        assertTrue(jdk.isPresent());
        assertEquals("jdk-21", jdk.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownRecipe() {
        ResourceRepository resourceRepo = new ResourceRepository();
        manager.addRepository(resourceRepo);

        Optional<Recipe> recipe = manager.resolveRecipe("unknown-recipe");
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldDedupRecipesAcrossRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        List<Recipe> recipes = manager.listRecipes();

        // Count how many times each recipe name appears
        java.util.Set<String> uniqueNames = recipes.stream()
                .map(Recipe::getName)
                .collect(java.util.stream.Collectors.toSet());

        // All names should be unique
        assertEquals(uniqueNames.size(), recipes.size());
    }

    @Test
    void shouldDescribeAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        String description = manager.describe();
        assertTrue(description.contains("RepositoryManager"));
        assertTrue(description.contains("ResourceRepository"));
        assertTrue(description.contains("DirRepo"));
    }

    @Test
    void shouldGetRecipeYamlContent() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> yamlContent = manager.getRecipeYamlContent("jdk-21");
        assertTrue(yamlContent.isPresent());
    }

    @Test
    void shouldReturnEmptyYamlForNonExistent() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> yamlContent = manager.getRecipeYamlContent("nonexistent-recipe");
        assertTrue(yamlContent.isEmpty());
    }

    @Test
    void shouldHandleEmptyRepositoryList() {
        List<Recipe> recipes = manager.listRecipes();
        assertNotNull(recipes);
        assertTrue(recipes.isEmpty());
    }

    @Test
    void shouldResolveFromFirstMatchingRepository() {
        // Add multiple repositories - first match wins
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        Optional<Recipe> recipe = manager.resolveRecipe("jdk-21");
        assertTrue(recipe.isPresent());
    }

    @Test
    void shouldFindRepositoryForRecipe() {
        ResourceRepository resourceRepo = new ResourceRepository();
        MockRepository mockRepo = new MockRepository("mock", List.of(createRecipe("unique")));

        manager.addRepository(resourceRepo);
        manager.addRepository(mockRepo);

        Optional<Repository> fromJar = manager.findRepositoryForRecipe("jdk-21");
        Optional<Repository> fromMock = manager.findRepositoryForRecipe("unique");

        assertTrue(fromJar.isPresent());
        assertTrue(fromJar.get() instanceof ResourceRepository);
        assertTrue(fromMock.isPresent());
        assertTrue(fromMock.get() instanceof MockRepository);
    }

    @Test
    void shouldGetRecipeFileNameFromRepository() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> fileName = manager.getRecipeFileName("jdk-21");
        Optional<String> missing = manager.getRecipeFileName("missing");

        assertTrue(fileName.isPresent());
        assertEquals("jdk-21.levain.yaml", fileName.get());
        assertTrue(missing.isEmpty());
    }

    @Test
    void shouldExposeRegistryAndInstalledState() {
        Registry registry = new Registry(tempDir.toString());
        manager.addRepository(registry);

        Recipe recipe = createRecipe("installed");
        registry.store(recipe, "version: 1.0.0\n");

        assertTrue(manager.getRegistry().isPresent());
        assertTrue(manager.isInstalled("installed"));
        assertFalse(manager.isInstalled("missing"));
    }

    private Recipe createRecipe(String name) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setVersion("1.0.0");
        return recipe;
    }
}
