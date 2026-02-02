package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryRepositoryTest {
    private DirectoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DirectoryRepository("TestDirRepo", "src/test/resources/recipes");
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
        assertFalse(recipes.isEmpty());
        assertTrue(recipes.size() > 0);
    }

    @Test
    void shouldResolveRecipeByName() {
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
    void shouldHaveCorrectProperties() {
        assertEquals("TestDirRepo", repository.getName());
        assertEquals("src/test/resources/recipes", repository.getUri());
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertTrue(description.contains("TestDirRepo"));
        assertTrue(description.contains("src/test/resources/recipes"));
    }
}
