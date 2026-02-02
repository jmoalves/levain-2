package com.github.jmoalves.levain.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.model.RecipeTree;
import com.github.jmoalves.levain.repository.RepositoryManager;
import com.github.jmoalves.levain.repository.ResourceRepository;
import com.github.jmoalves.levain.repository.DirectoryRepository;

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
    private RecipeTree recipeTree;

    @Inject
    public RecipeService(RecipeLoader recipeLoader) {
        this.recipeLoader = recipeLoader;
        this.repositoryManager = new RepositoryManager();
        initializeRepositories();
    }

    /**
     * Initialize repositories in the manager.
     * Built-in recipes from ResourceRepository are loaded first,
     * then external recipes from configured DirectoryRepository.
     */
    private void initializeRepositories() {
        // Add built-in resource recipes
        ResourceRepository resourceRepo = new ResourceRepository();
        repositoryManager.addRepository(resourceRepo);

        // Add external directory recipes if configured
        String recipesDir = RecipeLoader.getDefaultRecipesDirectory();
        if (recipesDir != null) {
            DirectoryRepository dirRepo = new DirectoryRepository("DirectoryRepository", recipesDir);
            repositoryManager.addRepository(dirRepo);
        } else {
            logger.info(
                    "No external recipes directory configured. Using only built-in recipes from ResourceRepository");
            logger.debug("To add external recipes, set LEVAIN_RECIPES_DIR or levain.recipes.dir");
            logger.debug("Or clone https://github.com/jmoalves/levain-pkgs to ~/levain/levain-pkgs/recipes");
        }

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
}
