package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryManagerTest {
    @TempDir
    Path tempDir;

    private RepositoryManager manager;

    @BeforeEach
    void setUp() {
        manager = new RepositoryManager();
    }

    @Test
    void shouldAddRepositories() {
        Repository repo1 = new DirectoryRepository("Repo1", "src/test/resources/recipes");
        manager.addRepository(repo1);
        assertEquals(1, manager.getRepositoryCount());
    }

    @Test
    void shouldInitializeAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        assertTrue(resourceRepo.isInitialized());
        assertTrue(dirRepo.isInitialized());
    }

    @Test
    void shouldListRecipesFromAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        List<Recipe> recipes = manager.listRecipes();
        assertFalse(recipes.isEmpty());
    }

    @Test
    void shouldResolveRecipesFromMultipleRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        // Should find jdk-21 from directory repo
        Optional<Recipe> jdk = manager.resolveRecipe("jdk-21");
        assertTrue(jdk.isPresent());
        assertEquals("jdk-21", jdk.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownRecipe() {
        ResourceRepository resourceRepo = new ResourceRepository();
        manager.addRepository(resourceRepo);

        Optional<Recipe> recipe = manager.resolveRecipe("unknown-recipe");
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldDedupRecipesAcrossRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        List<Recipe> recipes = manager.listRecipes();

        // Count how many times each recipe name appears
        java.util.Set<String> uniqueNames = recipes.stream()
                .map(Recipe::getName)
                .collect(java.util.stream.Collectors.toSet());

        // All names should be unique
        assertEquals(uniqueNames.size(), recipes.size());
    }

    @Test
    void shouldDescribeAllRepositories() {
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        String description = manager.describe();
        assertTrue(description.contains("RepositoryManager"));
        assertTrue(description.contains("ResourceRepository"));
        assertTrue(description.contains("DirRepo"));
    }

    @Test
    void shouldGetRecipeYamlContent() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> yamlContent = manager.getRecipeYamlContent("jdk-21");
        assertTrue(yamlContent.isPresent());
    }

    @Test
    void shouldReturnEmptyYamlForNonExistent() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> yamlContent = manager.getRecipeYamlContent("nonexistent-recipe");
        assertTrue(yamlContent.isEmpty());
    }

    @Test
    void shouldHandleEmptyRepositoryList() {
        List<Recipe> recipes = manager.listRecipes();
        assertNotNull(recipes);
        assertTrue(recipes.isEmpty());
    }

    @Test
    void shouldListRecipesWithoutResourceRepository() {
        MockRepository mockRepo = new MockRepository("mock", List.of(createRecipe("alpha"), createRecipe("beta")));
        manager.addRepository(mockRepo);

        List<Recipe> recipes = manager.listRecipes();

        assertEquals(2, recipes.size());
    }

    @Test
    void shouldResolveFromFirstMatchingRepository() {
        // Add multiple repositories - first match wins
        ResourceRepository resourceRepo = new ResourceRepository();
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");

        manager.addRepository(resourceRepo);
        manager.addRepository(dirRepo);

        Optional<Recipe> recipe = manager.resolveRecipe("jdk-21");
        assertTrue(recipe.isPresent());
    }

    @Test
    void shouldResolveFromNonJarWhenJarMissing() {
        ResourceRepository resourceRepo = new ResourceRepository();
        MockRepository mockRepo = new MockRepository("mock", List.of(createRecipe("unique")));

        manager.addRepository(resourceRepo);
        manager.addRepository(mockRepo);

        Optional<Recipe> recipe = manager.resolveRecipe("unique");

        assertTrue(recipe.isPresent());
        assertEquals("unique", recipe.get().getName());
    }

    @Test
    void shouldFindRepositoryForRecipe() {
        ResourceRepository resourceRepo = new ResourceRepository();
        MockRepository mockRepo = new MockRepository("mock", List.of(createRecipe("unique")));

        manager.addRepository(resourceRepo);
        manager.addRepository(mockRepo);

        Optional<Repository> fromJar = manager.findRepositoryForRecipe("jdk-21");
        Optional<Repository> fromMock = manager.findRepositoryForRecipe("unique");

        assertTrue(fromJar.isPresent());
        assertTrue(fromJar.get() instanceof ResourceRepository);
        assertTrue(fromMock.isPresent());
        assertTrue(fromMock.get() instanceof MockRepository);
    }

    @Test
    void shouldReturnEmptyWhenRepositoryNotFound() {
        MockRepository mockRepo = new MockRepository("mock");
        manager.addRepository(mockRepo);

        Optional<Repository> repo = manager.findRepositoryForRecipe("missing");

        assertTrue(repo.isEmpty());
    }

    @Test
    void shouldReturnYamlFromFirstRepositoryWithContent() {
        Repository emptyRepo = new InMemoryRepository(Optional.empty(), Optional.empty());
        Repository yamlRepo = new InMemoryRepository(Optional.of("name: demo\n"), Optional.of("demo.levain.yaml"));

        manager.addRepository(emptyRepo);
        manager.addRepository(yamlRepo);

        Optional<String> yaml = manager.getRecipeYamlContent("demo");
        Optional<String> fileName = manager.getRecipeFileName("demo");

        assertTrue(yaml.isPresent());
        assertTrue(fileName.isPresent());
    }

    @Test
    void shouldGetRecipeFileNameFromRepository() {
        DirectoryRepository dirRepo = new DirectoryRepository("DirRepo", "src/test/resources/recipes");
        manager.addRepository(dirRepo);

        Optional<String> fileName = manager.getRecipeFileName("jdk-21");
        Optional<String> missing = manager.getRecipeFileName("missing");

        assertTrue(fileName.isPresent());
        assertEquals("jdk-21.levain.yaml", fileName.get());
        assertTrue(missing.isEmpty());
    }

    @Test
    void shouldExposeRegistryAndInstalledState() {
        Registry registry = new Registry(tempDir.toString());
        manager.addRepository(registry);

        Recipe recipe = createRecipe("installed");
        registry.store(recipe, "version: 1.0.0\n");

        assertTrue(manager.getRegistry().isPresent());
        assertTrue(manager.isInstalled("installed"));
        assertFalse(manager.isInstalled("missing"));
    }

    @Test
    void shouldReturnFalseWhenRegistryMissing() {
        assertFalse(manager.isInstalled("missing"));
    }

    @Test
    void shouldContinueInitWhenRepositoryThrows() throws Exception {
        Repository throwingRepo = new ThrowingRepository();
        Repository okRepo = new InMemoryRepository(Optional.empty(), Optional.empty());

        addRepositoryWithoutInit(throwingRepo);
        addRepositoryWithoutInit(okRepo);

        manager.init();

        assertEquals(2, manager.getRepositoryCount());
    }

    private Recipe createRecipe(String name) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setVersion("1.0.0");
        return recipe;
    }

    private void addRepositoryWithoutInit(Repository repository) throws Exception {
        java.lang.reflect.Field field = RepositoryManager.class.getDeclaredField("repositories");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Repository> repositories = (List<Repository>) field.get(manager);
        repositories.add(repository);
    }

    private static class ThrowingRepository implements Repository {
        @Override
        public void init() {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean isInitialized() {
            return false;
        }

        @Override
        public String getName() {
            return "throwing";
        }

        @Override
        public String getUri() {
            return "throw://";
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

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String describe() {
            return "ThrowingRepository";
        }
    }

    private static class InMemoryRepository implements Repository {
        private final Optional<String> yamlContent;
        private final Optional<String> fileName;

        private InMemoryRepository(Optional<String> yamlContent, Optional<String> fileName) {
            this.yamlContent = yamlContent;
            this.fileName = fileName;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public String getName() {
            return "in-memory";
        }

        @Override
        public String getUri() {
            return "mem://";
        }

        @Override
        public List<Recipe> listRecipes() {
            return new ArrayList<>();
        }

        @Override
        public Optional<Recipe> resolveRecipe(String recipeName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getRecipeYamlContent(String recipeName) {
            return yamlContent;
        }

        @Override
        public Optional<String> getRecipeFileName(String recipeName) {
            return fileName;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String describe() {
            return "InMemoryRepository";
        }
    }
}
