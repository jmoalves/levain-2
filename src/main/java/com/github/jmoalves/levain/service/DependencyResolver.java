package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

/**
 * Resolves recipe dependencies and produces a topologically sorted installation plan.
 * 
 * This service:
 * 1. Loads the full dependency graph for a recipe
 * 2. Detects circular dependencies
 * 3. Produces a topologically sorted list for installation
 */
@ApplicationScoped
public class DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);

    private final RecipeService recipeService;

    @Inject
    public DependencyResolver(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Resolve all dependencies for a recipe and return them in topological order.
     * 
     * @param recipeName Name of the recipe to resolve
     * @return List of recipes in installation order (dependencies first, requested recipe last)
     * @throws IllegalArgumentException if recipe not found or circular dependency detected
     */
    public List<Recipe> resolveAndSort(String recipeName) {
        logger.info("Resolving dependencies for: {}", recipeName);
        
        Map<String, Recipe> allRecipes = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<String> sortedNames = new ArrayList<>();

        // Load recipe
        Recipe recipe = recipeService.loadRecipe(recipeName);
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found: " + recipeName);
        }

        // Build full dependency graph
        dfs(recipeName, allRecipes, visited, visiting, sortedNames);

        logger.info("Resolved dependencies in order: {}", sortedNames);

        // Convert sorted names back to recipes
        List<Recipe> result = new ArrayList<>();
        for (String name : sortedNames) {
            result.add(allRecipes.get(name));
        }

        return result;
    }

    /**
     * Depth-first search to build dependency graph and topological sort.
     * Implements Kahn's algorithm variant.
     */
    private void dfs(String recipeName,
                     Map<String, Recipe> allRecipes,
                     Set<String> visited,
                     Set<String> visiting,
                     List<String> sortedNames) {

        if (visited.contains(recipeName)) {
            return; // Already processed
        }

        if (visiting.contains(recipeName)) {
            throw new IllegalArgumentException("Circular dependency detected involving: " + recipeName);
        }

        visiting.add(recipeName);

        // Load recipe if not already loaded
        if (!allRecipes.containsKey(recipeName)) {
            Recipe recipe = recipeService.loadRecipe(recipeName);
            if (recipe == null) {
                throw new IllegalArgumentException("Recipe not found: " + recipeName);
            }
            allRecipes.put(recipeName, recipe);
        }

        Recipe recipe = allRecipes.get(recipeName);

        // Process dependencies first
        if (recipe.getDependencies() != null && !recipe.getDependencies().isEmpty()) {
            for (String dependency : recipe.getDependencies()) {
                dfs(dependency, allRecipes, visited, visiting, sortedNames);
            }
        }

        visiting.remove(recipeName);
        visited.add(recipeName);
        sortedNames.add(recipeName);
    }

    /**
     * Get a human-readable description of the installation plan.
     */
    public String formatInstallationPlan(List<Recipe> plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Installation Plan:\n");
        
        for (int i = 0; i < plan.size(); i++) {
            Recipe recipe = plan.get(i);
            String indent = recipe.getDependencies() != null && !recipe.getDependencies().isEmpty() 
                ? "  → " 
                : "  ✓ ";
            sb.append(String.format("%d. %s%s\n", i + 1, indent, recipe.getName()));
        }
        
        return sb.toString();
    }
}
