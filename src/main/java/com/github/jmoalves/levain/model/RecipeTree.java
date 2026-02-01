package com.github.jmoalves.levain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages recipe dependencies and resolution in a dependency tree.
 * All recipes implicitly depend on the "levain" recipe.
 */
public class RecipeTree {
    private final Map<String, Recipe> recipeMap;
    private static final String LEVAIN_RECIPE = "levain";

    public RecipeTree(Map<String, Recipe> recipeMap) {
        this.recipeMap = recipeMap;
    }

    /**
     * Resolves a recipe and all its dependencies in dependency order.
     * Dependencies are resolved recursively, with levain always as the first
     * dependency.
     *
     * @param recipeName the recipe to resolve
     * @return a list of recipes in order (dependencies first, target last)
     * @throws IllegalArgumentException if recipe is not found or circular
     *                                  dependency detected
     */
    public List<Recipe> resolve(String recipeName) {
        Set<String> visited = new HashSet<>();
        List<Recipe> resolved = new ArrayList<>();
        resolveRecursive(recipeName, visited, resolved);
        return resolved;
    }

    /**
     * Resolves multiple recipes and their dependencies.
     *
     * @param recipeNames the recipes to resolve
     * @return a list of unique recipes in order (dependencies first, targets last)
     * @throws IllegalArgumentException if any recipe is not found or circular
     *                                  dependency detected
     */
    public List<Recipe> resolveAll(List<String> recipeNames) {
        Set<String> visited = new HashSet<>();
        List<Recipe> resolved = new ArrayList<>();
        Map<String, Recipe> deduped = new java.util.LinkedHashMap<>();

        for (String recipeName : recipeNames) {
            resolveRecursive(recipeName, visited, resolved);
        }

        // Deduplicate while preserving order
        for (Recipe recipe : resolved) {
            deduped.put(recipe.getName(), recipe);
        }

        return new ArrayList<>(deduped.values());
    }

    /**
     * Recursive helper to resolve recipe dependencies.
     * Implements depth-first traversal with circular dependency detection.
     *
     * @param recipeName the recipe to resolve
     * @param visiting   set of recipes currently being visited (for cycle
     *                   detection)
     * @param resolved   list to accumulate resolved recipes
     * @throws IllegalArgumentException if recipe not found or circular dependency
     *                                  detected
     */
    private void resolveRecursive(String recipeName, Set<String> visiting, List<Recipe> resolved) {
        if (resolved.stream().anyMatch(r -> r.getName().equals(recipeName))) {
            return; // Already resolved
        }

        if (visiting.contains(recipeName)) {
            throw new IllegalArgumentException("Circular dependency detected involving: " + recipeName);
        }

        Recipe recipe = recipeMap.get(recipeName);
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found: " + recipeName);
        }

        visiting.add(recipeName);

        // Levain is the implicit first dependency for all recipes except levain itself
        if (!LEVAIN_RECIPE.equals(recipeName)) {
            resolveRecursive(LEVAIN_RECIPE, visiting, resolved);
        }

        // Resolve explicit dependencies
        if (recipe.getDependencies() != null) {
            for (String dep : recipe.getDependencies()) {
                if (!LEVAIN_RECIPE.equals(dep)) { // Skip levain, already added
                    resolveRecursive(dep, visiting, resolved);
                }
            }
        }

        visiting.remove(recipeName);
        resolved.add(recipe);
    }

    /**
     * Gets the recipe by name.
     *
     * @param recipeName the recipe name
     * @return the recipe wrapped in Optional
     */
    public Optional<Recipe> getRecipe(String recipeName) {
        return Optional.ofNullable(recipeMap.get(recipeName));
    }

    /**
     * Checks if a recipe exists.
     *
     * @param recipeName the recipe name
     * @return true if the recipe exists
     */
    public boolean hasRecipe(String recipeName) {
        return recipeMap.containsKey(recipeName);
    }

    /**
     * Gets all available recipe names.
     *
     * @return a list of all recipe names
     */
    public List<String> getAvailableRecipeNames() {
        return new ArrayList<>(recipeMap.keySet());
    }

    /**
     * Gets all available recipes.
     *
     * @return a list of all recipes
     */
    public List<Recipe> getAvailableRecipes() {
        return new ArrayList<>(recipeMap.values());
    }

    /**
     * Filters recipes by name substring.
     *
     * @param filter the filter string
     * @return a list of matching recipes
     */
    public List<Recipe> filterRecipes(String filter) {
        if (filter == null || filter.isEmpty()) {
            return getAvailableRecipes();
        }
        return recipeMap.values().stream()
                .filter(r -> r.getName().contains(filter))
                .collect(Collectors.toList());
    }

    /**
     * Gets the direct dependencies of a recipe.
     *
     * @param recipeName the recipe name
     * @return a list of dependency recipe names
     * @throws IllegalArgumentException if recipe not found
     */
    public List<String> getDependencies(String recipeName) {
        Recipe recipe = recipeMap.get(recipeName);
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found: " + recipeName);
        }

        List<String> deps = new ArrayList<>();
        if (!LEVAIN_RECIPE.equals(recipeName)) {
            deps.add(LEVAIN_RECIPE);
        }

        if (recipe.getDependencies() != null) {
            deps.addAll(recipe.getDependencies());
        }

        return deps;
    }
}
