package com.github.jmoalves.levain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.model.RecipeTree;
import com.github.jmoalves.levain.model.RepositoryConfig;
import com.github.jmoalves.levain.repository.RepositoryManager;
import com.github.jmoalves.levain.repository.RepositoryFactory;
import com.github.jmoalves.levain.repository.ResourceRepository;
import com.github.jmoalves.levain.repository.DirectoryRepository;
import com.github.jmoalves.levain.repository.Registry;
import com.github.jmoalves.levain.repository.Repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for managing and listing recipes.
 * Uses a RepositoryManager to load recipes from multiple sources:
 * - Built-in recipes from JAR resources
 * - External recipes from configured directories
 */
@ApplicationScoped
public class RecipeService {
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeLoader recipeLoader;
    private final RepositoryManager repositoryManager;
    private final ConfigService configService;
    private final RepositoryFactory repositoryFactory;
    private RecipeTree recipeTree;

    @Inject
    public RecipeService(RecipeLoader recipeLoader, ConfigService configService, RepositoryFactory repositoryFactory) {
        this.recipeLoader = recipeLoader;
        this.configService = configService;
        this.repositoryFactory = repositoryFactory;
        this.repositoryManager = new RepositoryManager();
        initializeRepositories();
    }

    /**
     * Initialize repositories in the manager.
     * Built-in recipes from ResourceRepository are loaded first,
     * then configured repositories from config.json,
     * then external recipes from configured DirectoryRepository,
     * then installed recipes from Registry.
     */
    private void initializeRepositories() {
        // Add built-in resource recipes
        ResourceRepository resourceRepo = new ResourceRepository();
        repositoryManager.addRepository(resourceRepo);

        // Add configured repositories from config.json
        for (RepositoryConfig repoConfig : configService.getRepositories()) {
            try {
                Repository repo = repositoryFactory.createRepository(repoConfig.getUri());
                repositoryManager.addRepository(repo);
                logger.info("Loaded configured repository: {} ({})", repoConfig.getName(), repoConfig.getUri());
            } catch (Exception e) {
                logger.warn("Failed to load configured repository {}: {}", repoConfig.getName(), e.getMessage());
            }
        }

        // Add external directory recipes if configured
        String recipesDir = RecipeLoader.getDefaultRecipesDirectory();
        if (recipesDir != null) {
            DirectoryRepository dirRepo = new DirectoryRepository("DirectoryRepository", recipesDir);
            repositoryManager.addRepository(dirRepo);
        } else {
            logger.debug("No external recipes directory configured.");
            logger.debug("To add external recipes, set LEVAIN_RECIPES_DIR or levain.recipes.dir");
            logger.debug("Or use 'levain config repo add <uri>' to add a repository");
        }

        // Add Registry (installed recipes) - searched last
        Registry registry = new Registry();
        repositoryManager.addRepository(registry);

        logger.debug(repositoryManager.describe());
    }

    private RecipeTree initializeRecipeTree() {
        if (recipeTree == null) {
            java.util.Map<String, Recipe> recipeMap = new java.util.LinkedHashMap<>();
            for (Recipe recipe : repositoryManager.listRecipes()) {
                recipeMap.put(recipe.getName(), recipe);
            }
            recipeTree = new RecipeTree(recipeMap);
        }
        return recipeTree;
    }

    /**
     * List available recipes, optionally filtered by a pattern.
     *
     * @param filter Optional filter pattern
     * @return List of recipe names
     */
    public List<String> listRecipes(String filter) {
        logger.debug("Listing recipes with filter: {}", filter);
        RecipeTree tree = initializeRecipeTree();
        return tree.filterRecipes(filter).stream()
                .map(Recipe::getName)
                .toList();
    }

    /**
     * Load a recipe by name.
     *
     * @param recipeName Name of the recipe
     * @return Recipe object
     */
    public Recipe loadRecipe(String recipeName) {
        logger.debug("Loading recipe: {}", recipeName);
        RecipeTree tree = initializeRecipeTree();
        return tree.getRecipe(recipeName)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + recipeName));
    }

    /**
     * Resolve a recipe and all its dependencies in order.
     *
     * @param recipeName Name of the recipe
     * @return List of resolved recipes in dependency order
     * @throws IllegalArgumentException if recipe not found or circular dependency
     *                                  detected
     */
    public List<Recipe> resolveRecipe(String recipeName) {
        logger.debug("Resolving recipe: {}", recipeName);
        RecipeTree tree = initializeRecipeTree();
        return tree.resolve(recipeName);
    }

    /**
     * Resolve multiple recipes and their dependencies.
     *
     * @param recipeNames Names of the recipes to resolve
     * @return List of unique resolved recipes in dependency order
     * @throws IllegalArgumentException if any recipe not found or circular
     *                                  dependency detected
     */
    public List<Recipe> resolveRecipes(List<String> recipeNames) {
        logger.debug("Resolving recipes: {}", recipeNames);
        RecipeTree tree = initializeRecipeTree();
        return tree.resolveAll(recipeNames);
    }

    /**
     * Get direct dependencies of a recipe.
     *
     * @param recipeName Name of the recipe
     * @return List of dependency names
     * @throws IllegalArgumentException if recipe not found
     */
    public List<String> getDependencies(String recipeName) {
        logger.debug("Getting dependencies for recipe: {}", recipeName);
        RecipeTree tree = initializeRecipeTree();
        return tree.getDependencies(recipeName);
    }

    /**
     * Get the original YAML content for a recipe.
     * This preserves all fields including custom ones.
     *
     * @param recipeName Name of the recipe
     * @return Optional containing the YAML content if found
     */
    public Optional<String> getRecipeYamlContent(String recipeName) {
        logger.debug("Getting YAML content for recipe: {}", recipeName);
        return repositoryManager.getRecipeYamlContent(recipeName);
    }

    /**
     * Get the original filename for a recipe.
     * This preserves the original extension (.levain.yaml, .levain.yml, etc.)
     *
     * @param recipeName Name of the recipe
     * @return Optional containing the filename if found
     */
    public Optional<String> getRecipeFileName(String recipeName) {
        logger.debug("Getting filename for recipe: {}", recipeName);
        return repositoryManager.getRecipeFileName(recipeName);
    }
}
