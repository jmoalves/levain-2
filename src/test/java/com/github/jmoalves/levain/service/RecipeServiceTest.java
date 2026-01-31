package com.github.jmoalves.levain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecipeService using JUnit 5.
 */
class RecipeServiceTest {

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService();
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
        assertEquals("1.0.0", recipe.getVersion());
        assertTrue(recipe.getDescription().contains("jdk-21"));
    }
}
