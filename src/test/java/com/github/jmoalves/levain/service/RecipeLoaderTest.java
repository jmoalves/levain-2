package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jmoalves.levain.model.Recipe;

class RecipeLoaderTest {
    private RecipeLoader recipeLoader;

    @BeforeEach
    void setUp() {
        recipeLoader = new RecipeLoader();
    }

    @Test
    void shouldLoadRecipesFromDirectory() {
        String recipesDir = "src/test/resources/recipes";
        Map<String, Recipe> recipes = recipeLoader.loadRecipesFromDirectory(recipesDir);

        assertNotNull(recipes);
        assertTrue(recipes.size() >= 5, "Should load at least 5 recipes");
        assertTrue(recipes.containsKey("levain"));
        assertTrue(recipes.containsKey("jdk-21"));
        assertTrue(recipes.containsKey("git"));
        assertTrue(recipes.containsKey("maven"));
        assertTrue(recipes.containsKey("gradle"));
    }

    @Test
    void shouldLoadSingleRecipe() throws IOException {
        File recipeFile = new File("src/test/resources/recipes/jdk-21.levain.yaml");
        Recipe recipe = recipeLoader.loadRecipe(recipeFile);

        assertNotNull(recipe);
        assertEquals("jdk-21", recipe.getName());
        assertEquals("21.0.5", recipe.getVersion());
        assertNotNull(recipe.getDescription());
    }

    @Test
    void shouldLoadRecipeWithDependencies() throws IOException {
        File recipeFile = new File("src/test/resources/recipes/maven.levain.yaml");
        Recipe recipe = recipeLoader.loadRecipe(recipeFile);

        assertNotNull(recipe);
        assertEquals("maven", recipe.getName());
        assertNotNull(recipe.getDependencies());
        assertTrue(recipe.getDependencies().contains("jdk-21"));
    }

    @Test
    void shouldExtractRecipeNameFromFileName() throws IOException {
        File yamlFile = new File("src/test/resources/recipes/gradle.levain.yaml");
        Recipe recipe = recipeLoader.loadRecipe(yamlFile);

        assertEquals("gradle", recipe.getName());
    }

    @Test
    void shouldHandleNonExistentDirectory() {
        String nonExistentDir = "non-existent-recipes-dir";
        Map<String, Recipe> recipes = recipeLoader.loadRecipesFromDirectory(nonExistentDir);

        assertNotNull(recipes);
        assertTrue(recipes.isEmpty(), "Should return empty map for non-existent directory");
    }

    @Test
    void shouldRejectWrongExtensions() throws IOException {
        // Test that files with wrong extensions are properly rejected
        File tempDir = new File("target/test-recipes-rejection");
        tempDir.mkdirs();

        // These should be rejected
        new File(tempDir, "test.levain.yml").createNewFile();
        new File(tempDir, "test.yml").createNewFile();
        new File(tempDir, "test.yaml").createNewFile();
        new File(tempDir, "test.levain").createNewFile();

        Map<String, Recipe> recipes = recipeLoader.loadRecipesFromDirectory(tempDir.getAbsolutePath());

        assertEquals(0, recipes.size(), "Should not load recipes with wrong extensions");
    }

    @Test
    void shouldLoadRecipeWithNullDependencies() throws IOException {
        File recipeFile = new File("src/test/resources/recipes/nodejs.levain.yaml");
        if (recipeFile.exists()) {
            Recipe recipe = recipeLoader.loadRecipe(recipeFile);

            assertNotNull(recipe);
            assertNotNull(recipe.getDependencies(), "Dependencies should be initialized even if null in file");
        }
    }

    @Test
    void shouldExtractRecipeNameCorrectly() throws IOException {
        File recipeFile = new File("src/test/resources/recipes/gradle.levain.yaml");
        Recipe recipe = recipeLoader.loadRecipe(recipeFile);

        assertEquals("gradle", recipe.getName());
        assertTrue(recipe.getName().length() > 0);
        assertFalse(recipe.getName().contains(".levain.yaml"), "Recipe name should not contain extension");
    }

    @Test
    void shouldGetDefaultRecipesDirectory() {
        String defaultDir = RecipeLoader.getDefaultRecipesDirectory();
        // Should return null or a valid directory path
        assertTrue(defaultDir == null || new File(defaultDir).isAbsolute());
    }
}
