package com.github.jmoalves.levain.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.model.RecipeTree;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for managing and listing recipes.
 */
@ApplicationScoped
public class RecipeService {
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeLoader recipeLoader;
    private RecipeTree recipeTree;

    @Inject
    public RecipeService(RecipeLoader recipeLoader) {
        this.recipeLoader = recipeLoader;
    }

    private RecipeTree initializeRecipeTree() {
        if (recipeTree == null) {
            java.util.Map<String, Recipe> recipeMap = loadAllRecipes();
            recipeTree = new RecipeTree(recipeMap);
        }
        return recipeTree;
    }

    private java.util.Map<String, Recipe> loadAllRecipes() {
        String recipesDir = RecipeLoader.getDefaultRecipesDirectory();
        if (recipesDir == null) {
            logger.warn("No recipes directory configured. Set LEVAIN_RECIPES_DIR or levain.recipes.dir");
            logger.info("Recipes should be cloned from https://github.com/jmoalves/levain-pkgs");
            return new java.util.LinkedHashMap<>();
        }
        logger.info("Loading recipes from directory: {}", recipesDir);
        return recipeLoader.loadRecipesFromDirectory(recipesDir);
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
