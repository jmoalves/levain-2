package com.github.jmoalves.levain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jmoalves.levain.model.Recipe;

class AbstractRepositoryTest {

    private TestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TestRepository();
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertEquals("TestRepository", repository.getName());
        assertEquals("test://source", repository.getUri());
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertNotNull(description);
        assertTrue(description.contains("TestRepository"));
    }

    @Test
    void shouldTrackInitializationState() {
        assertFalse(repository.isInitialized());
        repository.init();
        assertTrue(repository.isInitialized());
    }

    @Test
    void shouldReturnSizeAsListCount() {
        repository.init();
        assertEquals(0, repository.size());
    }

    // Simple test implementation of AbstractRepository
    private static class TestRepository extends AbstractRepository {
        public TestRepository() {
            super("TestRepository", "test://source");
        }

        @Override
        public void init() {
            setInitialized();
        }

        @Override
        public List<Recipe> listRecipes() {
            return List.of();
        }

        @Override
        public Optional<Recipe> resolveRecipe(String recipeName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getRecipeYamlContent(String recipeName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getRecipeFileName(String recipeName) {
            return Optional.empty();
        }
    }
}
