package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
