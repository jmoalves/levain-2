package com.github.jmoalves.levain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for MockRepository.
 */
@DisplayName("MockRepository Tests")
class MockRepositoryTest {

    private MockRepository mockRepository;
    private Recipe recipe1;
    private Recipe recipe2;

    @BeforeEach
    void setUp() {
        recipe1 = new Recipe();
        recipe1.setName("jdk-21");
        recipe1.setVersion("21.0.0");
        recipe1.setDescription("Java Development Kit 21");

        recipe2 = new Recipe();
        recipe2.setName("git");
        recipe2.setVersion("2.45.0");
        recipe2.setDescription("Git version control");

        mockRepository = new MockRepository("test-repo", List.of(recipe1, recipe2));
    }

    @Test
    @DisplayName("Should describe itself")
    void shouldDescribeSelf() {
        assertEquals("MockRepository[test-repo]", mockRepository.describe());
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
        assertEquals("test-repo", mockRepository.getName());
    }

    @Test
    @DisplayName("Should return mock URI")
    void shouldReturnMockUri() {
        assertEquals("mock://test-repo", mockRepository.getUri());
    }

    @Test
    @DisplayName("Should be immediately initialized")
    void shouldBeImmediatelyInitialized() {
        assertTrue(mockRepository.isInitialized());
    }

    @Test
    @DisplayName("Should list all recipes")
    void shouldListAllRecipes() {
        var recipes = mockRepository.listRecipes();
        assertEquals(2, recipes.size());
        assertEquals("jdk-21", recipes.get(0).getName());
        assertEquals("git", recipes.get(1).getName());
    }

    @Test
    @DisplayName("Should return correct size")
    void shouldReturnCorrectSize() {
        assertEquals(2, mockRepository.size());
    }

    @Test
    @DisplayName("Should resolve recipe by name")
    void shouldResolveRecipeByName() {
        var result = mockRepository.resolveRecipe("jdk-21");
        assertTrue(result.isPresent());
        assertEquals("jdk-21", result.get().getName());
        assertEquals("21.0.0", result.get().getVersion());
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent recipe")
    void shouldReturnEmptyOptionalForNonExistentRecipe() {
        var result = mockRepository.resolveRecipe("non-existent");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should add recipe dynamically")
    void shouldAddRecipeDynamically() {
        var recipe3 = new Recipe();
        recipe3.setName("maven");
        recipe3.setVersion("3.9.0");

        mockRepository.addRecipe(recipe3);

        assertEquals(3, mockRepository.size());
        assertTrue(mockRepository.resolveRecipe("maven").isPresent());
    }

    @Test
    @DisplayName("Should clear all recipes")
    void shouldClearAllRecipes() {
        mockRepository.clearRecipes();
        assertEquals(0, mockRepository.size());
        assertTrue(mockRepository.listRecipes().isEmpty());
    }

    @Test
    @DisplayName("Should support empty mock repository")
    void shouldSupportEmptyMockRepository() {
        var emptyRepo = new MockRepository("empty-repo");
        assertEquals(0, emptyRepo.size());
        assertTrue(emptyRepo.listRecipes().isEmpty());
    }

    @Test
    @DisplayName("Should return copy of recipes to prevent external modification")
    void shouldReturnCopyOfRecipesToPreventModification() {
        var recipes = mockRepository.listRecipes();
        recipes.clear();

        // Original repository should still have 2 recipes
        assertEquals(2, mockRepository.size());
        assertEquals(2, mockRepository.listRecipes().size());
    }

    @Test
    @DisplayName("Should be idempotent on init")
    void shouldBeIdempotentOnInit() {
        mockRepository.init();
        mockRepository.init(); // Should not throw
        assertEquals(2, mockRepository.size());
    }

    @Test
    @DisplayName("Should return correct recipe count")
    void shouldReturnCorrectRecipeCount() {
        assertEquals(2, mockRepository.getRecipeCount());
        mockRepository.addRecipe(new Recipe());
        assertEquals(3, mockRepository.getRecipeCount());
    }
}
