package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRepositoryTest {
    @TempDir
    Path tempDir;

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

    @Test
    void shouldIdentifyRecipeFilesAndExtractNames() throws Exception {
        assertTrue(invokeIsRecipeFile("sample.levain.yaml"));
        assertTrue(invokeIsRecipeFile("sample.levain.yml"));
        assertTrue(invokeIsRecipeFile("sample.levain"));
        assertFalse(invokeIsRecipeFile("sample.yaml"));

        String name = invokeExtractRecipeName("sample.levain.yaml");
        assertEquals("sample", name);
    }

    @Test
    void shouldListRecipesFromDirectoryResource() throws Exception {
        Path recipeFile = tempDir.resolve("demo.levain.yaml");
        Files.writeString(recipeFile, "version: 1.0.0\n");

        List<java.net.URL> recipeUrls = invokeListRecipesFromDirectory(recipeFile.getParent());
        assertEquals(1, recipeUrls.size());
        assertTrue(recipeUrls.get(0).toString().contains("demo.levain.yaml"));
    }

    @Test
    void shouldLoadRecipeFromResourceFile() throws Exception {
        Path recipeFile = tempDir.resolve("loaded.levain.yaml");
        Files.writeString(recipeFile, "version: 1.2.3\n");

        Optional<Recipe> recipe = invokeLoadRecipeFromResource(recipeFile);
        assertTrue(recipe.isPresent());
        assertEquals("loaded", recipe.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenRecipeYamlInvalid() throws Exception {
        Path recipeFile = tempDir.resolve("broken.levain.yaml");
        Files.writeString(recipeFile, "version: [", StandardCharsets.UTF_8);

        Optional<Recipe> recipe = invokeLoadRecipeFromResource(recipeFile);

        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldSkipNonRecipeResourceFiles() throws Exception {
        Path recipeFile = tempDir.resolve("skip.txt");
        Files.writeString(recipeFile, "version: 1.2.3\n");

        Optional<Recipe> recipe = invokeLoadRecipeFromResource(recipeFile);
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldListRecipesFromJarResource() throws Exception {
        Path jarPath = tempDir.resolve("recipes.jar");
        createJarWithRecipes(jarPath, List.of("recipes/first.levain.yaml", "recipes/second.levain"));

        java.net.URL jarUrl = new java.net.URL("jar:file:" + jarPath.toAbsolutePath() + "!/recipes");
        List<java.net.URL> recipes = invokeListRecipesFromJar(jarUrl);

        assertEquals(2, recipes.size());
    }

    @Test
    void shouldHandleMissingDirectoryResource() throws Exception {
        Path missing = tempDir.resolve("missing-dir");

        List<java.net.URL> recipes = invokeListRecipesFromDirectory(missing);

        assertTrue(recipes.isEmpty());
    }

    private boolean invokeIsRecipeFile(String filename) throws Exception {
        Method method = ResourceRepository.class.getDeclaredMethod("isRecipeFile", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(repository, filename);
    }

    private String invokeExtractRecipeName(String filename) throws Exception {
        Method method = ResourceRepository.class.getDeclaredMethod("extractRecipeName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, filename);
    }

    private List<java.net.URL> invokeListRecipesFromDirectory(Path directory) throws Exception {
        Method method = ResourceRepository.class.getDeclaredMethod("listRecipesFromDirectory", java.net.URL.class);
        method.setAccessible(true);
        return castUrlList(method.invoke(repository, directory.toUri().toURL()));
    }

    private List<java.net.URL> invokeListRecipesFromJar(java.net.URL jarUrl) throws Exception {
        Method method = ResourceRepository.class.getDeclaredMethod("listRecipesFromJar", java.net.URL.class);
        method.setAccessible(true);
        return castUrlList(method.invoke(repository, jarUrl));
    }

    private Optional<Recipe> invokeLoadRecipeFromResource(Path recipeFile) throws Exception {
        Method method = ResourceRepository.class.getDeclaredMethod("loadRecipeFromResource", java.net.URL.class);
        method.setAccessible(true);
        return castRecipeOptional(method.invoke(repository, recipeFile.toUri().toURL()));
    }

    @SuppressWarnings("unchecked")
    private List<java.net.URL> castUrlList(Object value) {
        return (List<java.net.URL>) value;
    }

    @SuppressWarnings("unchecked")
    private Optional<Recipe> castRecipeOptional(Object value) {
        return (Optional<Recipe>) value;
    }

    private void createJarWithRecipes(Path jarPath, List<String> entries) throws IOException {
        List<String> normalized = new ArrayList<>(entries);
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (String entryName : normalized) {
                JarEntry entry = new JarEntry(entryName);
                jarOutputStream.putNextEntry(entry);
                jarOutputStream.write("version: 1.0.0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                jarOutputStream.closeEntry();
            }
        }
    }
}
