package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing and listing recipes.
 */
public class RecipeService {
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    /**
     * List available recipes, optionally filtered by a pattern.
     *
     * @param filter Optional filter pattern
     * @return List of recipe names
     */
    public List<String> listRecipes(String filter) {
        logger.debug("Listing recipes with filter: {}", filter);
        
        // TODO: Implement recipe discovery from recipe repositories
        // For now, return a sample list
        List<String> recipes = new ArrayList<>();
        recipes.add("jdk-21");
        recipes.add("git");
        recipes.add("maven");
        recipes.add("gradle");
        recipes.add("nodejs");
        
        if (filter != null && !filter.isEmpty()) {
            return recipes.stream()
                    .filter(r -> r.toLowerCase().contains(filter.toLowerCase()))
                    .toList();
        }
        
        return recipes;
    }

    /**
     * Load a recipe by name.
     *
     * @param recipeName Name of the recipe
     * @return Recipe object
     */
    public Recipe loadRecipe(String recipeName) {
        logger.debug("Loading recipe: {}", recipeName);
        // TODO: Implement recipe loading from YAML files
        Recipe recipe = new Recipe();
        recipe.setVersion("1.0.0");
        recipe.setDescription("Recipe for " + recipeName);
        return recipe;
    }
}
