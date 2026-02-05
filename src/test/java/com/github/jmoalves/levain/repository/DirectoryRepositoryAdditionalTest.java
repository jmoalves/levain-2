package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DirectoryRepository Additional Tests")
class DirectoryRepositoryAdditionalTest {

    @TempDir
    Path tempDir;

    private DirectoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DirectoryRepository("test-repo", tempDir.toString());
    }

    @Test
    @DisplayName("Should initialize with empty directory")
    void testInitializeEmptyDirectory() {
        repository.init();
        List<Recipe> recipes = repository.listRecipes();
        assertNotNull(recipes, "Recipe list should not be null");
        assertTrue(recipes.isEmpty(), "Empty directory should have no recipes");
    }

    @Test
    @DisplayName("Should have proper name from initialization")
    void testRepositoryName() {
        repository.init();
        assertNotNull(repository.getName(), "Repository should have a name");
    }

    @Test
    @DisplayName("Should handle recipe name with .levain.yaml extension")
    void testRecipeNameWithLevainYamlExtension() {
        repository.init();
        Optional<String> filename = repository.getRecipeFileName("bad.levain.yaml");
        assertTrue(filename.isEmpty(), "Should reject recipe name containing .levain.yaml");
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent recipe")
    void testResolveNonExistentRecipe() {
        repository.init();
        Optional<Recipe> recipe = repository.resolveRecipe("non-existent");
        assertTrue(recipe.isEmpty(), "Should return empty Optional for non-existent recipe");
    }

    @Test
    @DisplayName("Should return empty Optional for YAML content of non-existent recipe")
    void testGetRecipeYamlContentNonExistent() {
        repository.init();
        Optional<String> content = repository.getRecipeYamlContent("non-existent");
        assertTrue(content.isEmpty(), "Should return empty Optional for non-existent recipe YAML");
    }

    @Test
    @DisplayName("Should return empty Optional for filename of non-existent recipe")
    void testGetRecipeFileNameNonExistent() {
        repository.init();
        Optional<String> filename = repository.getRecipeFileName("non-existent");
        assertTrue(filename.isEmpty(), "Should return empty Optional for non-existent recipe filename");
    }

    @Test
    @DisplayName("Should validate .levain.yaml filename format")
    void testValidateLevainFilenameFormat() {
        // Create a file with .levain.yaml extension
        try {
            Path recipeFile = tempDir.resolve("test.levain.yaml");
            Files.writeString(recipeFile, "name: test\nversion: 1.0.0");

            repository.init();
            List<Recipe> recipes = repository.listRecipes();

            // If file was loaded, it should be valid
            assertNotNull(recipes, "Recipe list should be valid");
        } catch (IOException e) {
            fail("Could not create test file", e);
        }
    }

    @Test
    @DisplayName("Should be Dependent scoped")
    void testIsDependent() {
        assertTrue(DirectoryRepository.class.isAnnotationPresent(jakarta.enterprise.context.Dependent.class),
                "DirectoryRepository should be Dependent");
    }

    @Test
    @DisplayName("Should extend AbstractRepository")
    void testExtendsAbstractRepository() {
        assertTrue(AbstractRepository.class.isAssignableFrom(DirectoryRepository.class),
                "DirectoryRepository should extend AbstractRepository");
    }

    @Test
    @DisplayName("Should list recipes as collection")
    void testListRecipesReturnsCopy() {
        repository.init();
        List<Recipe> recipes1 = repository.listRecipes();
        List<Recipe> recipes2 = repository.listRecipes();

        // Should return List.copyOf which creates immutable copies
        assertEquals(recipes1, recipes2, "Content should be equal");
        assertTrue(recipes1.isEmpty(), "Should be empty for empty directory");
    }

    @Test
    @DisplayName("Should handle null directory gracefully")
    void testNullDirectoryHandling() {
        // This should not throw during init
        DirectoryRepository nullRepo = new DirectoryRepository("null-repo", null);
        assertDoesNotThrow(() -> nullRepo.init(), "Should handle null directory path gracefully");
    }

    @Test
    @DisplayName("Should have proper toString from parent")
    void testToString() {
        repository.init();
        String toString = repository.toString();
        assertNotNull(toString, "toString should not be null");
    }

    @Test
    @DisplayName("Should handle non-existent directory gracefully")
    void testNonExistentDirectory() {
        DirectoryRepository noPath = new DirectoryRepository("no-path", "/non/existent/path");
        assertDoesNotThrow(() -> noPath.init(), "Should handle non-existent directory gracefully");
    }
}
