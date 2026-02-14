package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryRepositoryTest {
    private DirectoryRepository repository;

    @TempDir
    Path tempDir;

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

    @Test
    void shouldGetRecipeYamlContent() {
        repository.init();
        Optional<String> yamlContent = repository.getRecipeYamlContent("jdk-21");

        assertTrue(yamlContent.isPresent());
        assertTrue(yamlContent.get().contains("name:") || yamlContent.get().contains("version:"));
    }

    @Test
    void shouldReturnEmptyForNonExistentYaml() {
        repository.init();
        Optional<String> yamlContent = repository.getRecipeYamlContent("nonexistent-recipe");

        assertTrue(yamlContent.isEmpty());
    }

    @Test
    void shouldGetRecipeFileName() {
        repository.init();
        Optional<String> fileName = repository.getRecipeFileName("jdk-21");

        assertTrue(fileName.isPresent());
        assertEquals("jdk-21.levain.yaml", fileName.get());
    }

    @Test
    void shouldRejectInvalidRecipeName() {
        repository.init();
        Optional<String> fileName = repository.getRecipeFileName("test.levain.yaml");

        assertTrue(fileName.isEmpty());
    }

    @Test
    void shouldReturnEmptyFileNameWhenRecipeMissing() {
        repository.init();

        Optional<String> fileName = repository.getRecipeFileName("missing-recipe");

        assertTrue(fileName.isEmpty());
    }

    @Test
    void shouldFindYamlContentInSubdirectory() throws Exception {
        Path recipesDir = tempDir.resolve("recipes");
        Path nested = recipesDir.resolve("nested");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("demo.levain.yaml"), "version: 1.0.0\n", StandardCharsets.UTF_8);

        DirectoryRepository localRepo = new DirectoryRepository("TempRepo", recipesDir.toString());
        localRepo.init();

        Optional<String> content = localRepo.getRecipeYamlContent("demo");

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("version"));
    }

    @Test
    void shouldRejectMultipleLevainExtensions() throws Exception {
        Method method = DirectoryRepository.class.getDeclaredMethod("isValidLevainFilename", String.class);
        method.setAccessible(true);

        boolean valid = (boolean) method.invoke(repository, "bad.levain.yaml.levain.yaml");

        assertFalse(valid);
    }

    @Test
    void shouldRejectNonLevainFilename() throws Exception {
        Method method = DirectoryRepository.class.getDeclaredMethod("isValidLevainFilename", String.class);
        method.setAccessible(true);

        boolean valid = (boolean) method.invoke(repository, "notes.txt");

        assertFalse(valid);
    }

    @Test
    void shouldWarnOnRecipeNameWithExtension() throws Exception {
        DirectoryRepository localRepo = new DirectoryRepository("TempRepo", tempDir.toString());

        java.lang.reflect.Field field = DirectoryRepository.class.getDeclaredField("recipes");
        field.setAccessible(true);
        field.set(localRepo, java.util.Map.of("bad.levain.yaml", new Recipe()));

        Optional<String> result = localRepo.getRecipeFileName("bad.levain.yaml");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenFallbackFindsNoMatch() throws Exception {
        Path recipesDir = tempDir.resolve("recipes-missing");
        Files.createDirectories(recipesDir);
        Files.writeString(recipesDir.resolve("other.levain.yaml"), "version: 1.0.0\n", StandardCharsets.UTF_8);
        Files.writeString(recipesDir.resolve("bad.levain.yaml.levain.yaml"), "version: 2.0.0\n", StandardCharsets.UTF_8);

        DirectoryRepository localRepo = new DirectoryRepository("TempRepo", recipesDir.toString());
        java.lang.reflect.Field field = DirectoryRepository.class.getDeclaredField("recipes");
        field.setAccessible(true);
        field.set(localRepo, java.util.Map.of("demo", new Recipe()));

        Optional<String> content = localRepo.getRecipeYamlContent("demo");

        assertTrue(content.isEmpty());
    }

    @Test
    void shouldHandleInvalidDirectory() {
        DirectoryRepository invalidRepo = new DirectoryRepository("InvalidRepo", "nonexistent-dir-12345");
        invalidRepo.init();

        assertTrue(invalidRepo.isInitialized());
        List<Recipe> recipes = invalidRepo.listRecipes();
        assertTrue(recipes.isEmpty());
    }

    @Test
    void shouldReturnCorrectSize() {
        repository.init();
        int size = repository.size();
        assertTrue(size > 0);
        assertEquals(repository.listRecipes().size(), size);
    }
}
