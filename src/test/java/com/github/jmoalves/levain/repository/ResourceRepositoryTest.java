package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRepositoryTest {
    private ResourceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ResourceRepository();
    }

    @Test
    void shouldInitialize() {
        assertFalse(repository.isInitialized());
        repository.init();
        assertTrue(repository.isInitialized());
    }

    @Test
    void shouldListRecipes() {
        repository.init();
        List<Recipe> recipes = repository.listRecipes();
        // In test environment, resources may not be loaded from JAR
        // Just verify it returns a list
        assertNotNull(recipes);
    }

    @Test
    void shouldLoadRecipesFromResources() {
        repository.init();
        List<Recipe> recipes = repository.listRecipes();
        assertNotNull(recipes);
        assertFalse(recipes.isEmpty(), "Expected recipes to be loaded from test resources");
        assertTrue(recipes.stream().anyMatch(r -> "jdk-21".equals(r.getName())));
    }

    @Test
    void shouldResolveKnownRecipeFromResources() {
        repository.init();
        Optional<Recipe> recipe = repository.resolveRecipe("jdk-21");
        assertTrue(recipe.isPresent());
        assertEquals("jdk-21", recipe.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownRecipe() {
        repository.init();
        Optional<Recipe> recipe = repository.resolveRecipe("unknown-recipe");
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldReturnEmptyYamlAndFileName() {
        repository.init();
        assertTrue(repository.getRecipeYamlContent("jdk-21").isEmpty());
        assertTrue(repository.getRecipeFileName("jdk-21").isEmpty());
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertEquals("ResourceRepository", repository.getName());
        assertEquals("jar://recipes", repository.getUri());
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertTrue(description.contains("ResourceRepository"));
        assertTrue(description.contains("jar://recipes"));
    }
}
