package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.repository.Repository;
import com.github.jmoalves.levain.repository.RepositoryFactory;
import com.github.jmoalves.levain.repository.ResourceRepository;

/**
 * Unit tests for RecipeService using JUnit 5.
 */
class RecipeServiceTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;

    private RecipeService recipeService;
    private RecipeLoader recipeLoader;
    private ConfigService configService;
    private RepositoryFactory repositoryFactory;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        // Set test recipes directory for testing
        System.setProperty("levain.recipes.dir", "src/test/resources/recipes");
        recipeLoader = new RecipeLoader();
        configService = new ConfigService();
        repositoryFactory = new RepositoryFactory();
        recipeService = new RecipeService(recipeLoader, configService, repositoryFactory);
    }

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        } else {
            System.clearProperty("user.home");
        }
    }

    @Test
    void testListRecipesWithoutFilter() {
        List<String> recipes = recipeService.listRecipes(null);

        assertNotNull(recipes);
        assertFalse(recipes.isEmpty());
        assertTrue(recipes.contains("jdk-21"));
        assertTrue(recipes.contains("git"));
    }

    @Test
    void testListRecipesWithFilter() {
        List<String> recipes = recipeService.listRecipes("jdk");

        assertNotNull(recipes);
        assertFalse(recipes.isEmpty());
        assertTrue(recipes.contains("jdk-21"));
        assertFalse(recipes.contains("git"));
    }

    @Test
    void testListRecipesWithNoMatch() {
        List<String> recipes = recipeService.listRecipes("nonexistent");

        assertNotNull(recipes);
        assertTrue(recipes.isEmpty());
    }

    @Test
    void testLoadRecipe() {
        var recipe = recipeService.loadRecipe("jdk-21");

        assertNotNull(recipe);
        assertEquals("jdk-21", recipe.getName());
        assertEquals("21.0.5", recipe.getVersion());
    }

    @Test
    void testLoadNonExistentRecipe() {
        assertThrows(IllegalArgumentException.class,
                () -> recipeService.loadRecipe("nonexistent-recipe-12345"));
    }

    @Test
    void testGetRecipeYamlContent() {
        var yamlContent = recipeService.getRecipeYamlContent("jdk-21");

        assertNotNull(yamlContent);
        assertTrue(yamlContent.isPresent());
        assertTrue(yamlContent.get().contains("name:") || yamlContent.get().contains("version:"));
    }

    @Test
    void testGetRecipeYamlContentNonExistent() {
        var yamlContent = recipeService.getRecipeYamlContent("nonexistent-recipe-12345");

        assertNotNull(yamlContent);
        assertTrue(yamlContent.isEmpty());
    }

    @Test
    void testResolveRecipe() {
        List<Recipe> recipes = recipeService.resolveRecipe("jdk-21");

        assertNotNull(recipes);
        assertTrue(recipes.size() > 0);
        assertTrue(recipes.stream().anyMatch(r -> r.getName().equals("jdk-21")));
    }

    @Test
    void testResolveRecipesMultiple() {
        List<Recipe> recipes = recipeService.resolveRecipes(List.of("jdk-21", "git"));

        assertNotNull(recipes);
        assertTrue(recipes.stream().anyMatch(r -> "jdk-21".equals(r.getName())));
        assertTrue(recipes.stream().anyMatch(r -> "git".equals(r.getName())));
    }

    @Test
    void testGetRecipeFileName() {
        Optional<String> fileName = recipeService.getRecipeFileName("jdk-21");

        assertTrue(fileName.isPresent());
        assertEquals("jdk-21.levain.yaml", fileName.get());
    }

    @Test
    void testFindSourceRepository() {
        Optional<Repository> repository = recipeService.findSourceRepository("jdk-21");

        assertTrue(repository.isPresent());
        assertTrue(repository.get() instanceof ResourceRepository);
    }

    @Test
    void testInstalledMetadataDefaults() {
        String recipeName = "missing-" + UUID.randomUUID();
        assertFalse(recipeService.isInstalled(recipeName));
        assertTrue(recipeService.getInstalledMetadata(recipeName).isEmpty());
    }

    @Test
    void testListRecipesWithFilterCaseSensitive() {
        List<String> recipes = recipeService.listRecipes("JDK");

        assertNotNull(recipes);
        assertTrue(recipes.isEmpty());
    }

    @Test
    void testListRecipesWithEmptyFilter() {
        List<String> allRecipes = recipeService.listRecipes("");
        List<String> nullRecipes = recipeService.listRecipes(null);

        assertNotNull(allRecipes);
        assertNotNull(nullRecipes);
        assertEquals(allRecipes.size(), nullRecipes.size());
    }
}
