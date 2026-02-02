package com.github.jmoalves.levain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NullRepository.
 */
@DisplayName("NullRepository Tests")
class NullRepositoryTest {

    private NullRepository repository;

    @BeforeEach
    void setUp() {
        repository = new NullRepository();
    }

    @Test
    @DisplayName("Should describe itself")
    void shouldDescribe() {
        assertEquals(NullRepository.NULL_REPOSITORY_NAME, repository.describe());
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
        assertEquals(NullRepository.NULL_REPOSITORY_NAME, repository.getName());
    }

    @Test
    @DisplayName("Should return null URI")
    void shouldReturnNullUri() {
        assertEquals(NullRepository.NULL_REPOSITORY_URI, repository.getUri());
    }

    @Test
    @DisplayName("Should be immediately initialized")
    void shouldBeImmediatelyInitialized() {
        assertTrue(repository.isInitialized(), "NullRepository should always be initialized");
    }

    @Test
    @DisplayName("Should return empty list of recipes")
    void shouldReturnEmptyRecipeList() {
        var recipes = repository.listRecipes();
        assertTrue(recipes.isEmpty(), "NullRepository should return empty recipe list");
    }

    @Test
    @DisplayName("Should return zero size")
    void shouldReturnZeroSize() {
        assertEquals(0, repository.size(), "NullRepository should have zero recipes");
    }

    @Test
    @DisplayName("Should return empty Optional when resolving recipe")
    void shouldReturnEmptyOptionalWhenResolvingRecipe() {
        var result = repository.resolveRecipe("any-recipe");
        assertFalse(result.isPresent(), "NullRepository should return empty Optional");
    }

    @Test
    @DisplayName("Should return empty Optional for any recipe name")
    void shouldReturnEmptyOptionalForAnyRecipeName() {
        assertFalse(repository.resolveRecipe("jdk-21").isPresent());
        assertFalse(repository.resolveRecipe("git").isPresent());
        assertFalse(repository.resolveRecipe("maven").isPresent());
    }

    @Test
    @DisplayName("Should be idempotent on init")
    void shouldBeIdempotentOnInit() {
        repository.init();
        repository.init(); // Should not throw
        assertTrue(repository.isInitialized());
    }
}
