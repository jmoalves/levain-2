package com.github.jmoalves.levain.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.model.RecipeTree;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for managing and listing recipes.
 */
@ApplicationScoped
public class RecipeService {
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    private RecipeTree recipeTree;

    private RecipeTree initializeRecipeTree() {
        if (recipeTree == null) {
            java.util.Map<String, Recipe> recipeMap = loadAllRecipes();
            recipeTree = new RecipeTree(recipeMap);
        }
        return recipeTree;
    }

    private java.util.Map<String, Recipe> loadAllRecipes() {
        // TODO: Implement recipe discovery from recipe repositories
        java.util.Map<String, Recipe> recipes = new java.util.LinkedHashMap<>();

        // Sample recipes
        Recipe levainRecipe = new Recipe();
        levainRecipe.setName("levain");
        levainRecipe.setVersion("1.0.0");
        levainRecipe.setDescription("Levain base runtime");
        recipes.put("levain", levainRecipe);

        Recipe jdkRecipe = new Recipe();
        jdkRecipe.setName("jdk-21");
        jdkRecipe.setVersion("21.0.0");
        jdkRecipe.setDescription("OpenJDK 21");
        jdkRecipe.setDependencies(new ArrayList<>());
        recipes.put("jdk-21", jdkRecipe);

        Recipe gitRecipe = new Recipe();
        gitRecipe.setName("git");
        gitRecipe.setVersion("2.40.0");
        gitRecipe.setDescription("Git version control");
        gitRecipe.setDependencies(new ArrayList<>());
        recipes.put("git", gitRecipe);

        Recipe mavenRecipe = new Recipe();
        mavenRecipe.setName("maven");
        mavenRecipe.setVersion("3.9.0");
        mavenRecipe.setDescription("Apache Maven");
        mavenRecipe.setDependencies(new ArrayList<>(java.util.List.of("jdk-21")));
        recipes.put("maven", mavenRecipe);

        Recipe gradleRecipe = new Recipe();
        gradleRecipe.setName("gradle");
        gradleRecipe.setVersion("8.0.0");
        gradleRecipe.setDescription("Gradle build tool");
        gradleRecipe.setDependencies(new ArrayList<>(java.util.List.of("jdk-21")));
        recipes.put("gradle", gradleRecipe);

        Recipe nodejsRecipe = new Recipe();
        nodejsRecipe.setName("nodejs");
        nodejsRecipe.setVersion("20.0.0");
        nodejsRecipe.setDescription("Node.js runtime");
        nodejsRecipe.setDependencies(new ArrayList<>());
        recipes.put("nodejs", nodejsRecipe);

        return recipes;
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
