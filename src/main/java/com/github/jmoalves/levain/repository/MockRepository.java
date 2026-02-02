package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock repository for unit testing.
 * 
 * Allows tests to provide predefined recipes without requiring file system,
 * network access, or git operations. Useful for testing repository consumers
 * (like RepositoryManager, InstallService, etc.) in isolation.
 * 
 * Example:
 * 
 * <pre>
 * Recipe recipe1 = Recipe.builder().name("jdk-21").version("21.0.0").build();
 * Recipe recipe2 = Recipe.builder().name("git").version("2.45.0").build();
 * MockRepository mockRepo = new MockRepository("test-repo", List.of(recipe1, recipe2));
 * 
 * Optional<Recipe> result = mockRepo.resolveRecipe("jdk-21");
 * assert result.isPresent();
 * assert result.get().getName().equals("jdk-21");
 * </pre>
 */
public class MockRepository implements Repository {
    private static final Logger logger = LogManager.getLogger(MockRepository.class);
    private final String name;
    private final String uri;
    private final List<Recipe> recipes;
    private boolean initialized;

    /**
     * Create a mock repository with a name and list of recipes.
     * 
     * @param name    The repository name/description
     * @param recipes The list of recipes to provide
     */
    public MockRepository(String name, List<Recipe> recipes) {
        this.name = name;
        this.uri = "mock://" + name;
        this.recipes = new ArrayList<>(recipes != null ? recipes : new ArrayList<>());
        this.initialized = true; // Always initialized immediately
    }

    /**
     * Create an empty mock repository with a name.
     * 
     * @param name The repository name/description
     */
    public MockRepository(String name) {
        this(name, new ArrayList<>());
    }

    @Override
    public String describe() {
        return "MockRepository[" + name + "]";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void init() {
        // Already initialized in constructor
        logger.debug("Mock repository initialized: {}", name);
    }

    @Override
    public List<Recipe> listRecipes() {
        return new ArrayList<>(recipes); // Return a copy to prevent external modification
    }

    @Override
    public Optional<Recipe> resolveRecipe(String recipeName) {
        return recipes.stream()
                .filter(recipe -> recipe.getName().equals(recipeName))
                .findFirst();
    }

    @Override
    public Optional<String> getRecipeYamlContent(String recipeName) {
        // Mock repository doesn't have YAML content
        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        // Mock repository doesn't have file names
        return Optional.empty();
    }

    @Override
    public int size() {
        return recipes.size();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Add a recipe to this mock repository.
     * Useful for building up test data.
     * 
     * @param recipe The recipe to add
     */
    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
    }

    /**
     * Clear all recipes from this mock repository.
     */
    public void clearRecipes() {
        recipes.clear();
    }

    /**
     * Get the number of recipes in this mock repository.
     * 
     * @return The recipe count
     */
    public int getRecipeCount() {
        return recipes.size();
    }
}
