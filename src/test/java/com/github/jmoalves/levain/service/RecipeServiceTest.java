package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.repository.RepositoryFactory;

/**
 * Unit tests for RecipeService using JUnit 5.
 */
class RecipeServiceTest {

    private RecipeService recipeService;
    private RecipeLoader recipeLoader;
    private ConfigService configService;
    private RepositoryFactory repositoryFactory;

    @BeforeEach
    void setUp() {
        // Set test recipes directory for testing
        System.setProperty("levain.recipes.dir", "src/test/resources/recipes");
        recipeLoader = new RecipeLoader();
        configService = new ConfigService();
        repositoryFactory = new RepositoryFactory();
        recipeService = new RecipeService(recipeLoader, configService, repositoryFactory);
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
