package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for action execution.
 * 
 * Maintains recipe-level state including:
 * - Configuration and recipe metadata
 * - Base and recipe directories
 * - Recipe-scoped variables (set via setVar action)
 */
public class ActionContext {
    private final Config config;
    private final Recipe recipe;
    private final Path baseDir;
    private final Path recipeDir;
    private final Map<String, String> recipeVariables = new HashMap<>();

    public ActionContext(Config config, Recipe recipe, Path baseDir, Path recipeDir) {
        this.config = config;
        this.recipe = recipe;
        this.baseDir = baseDir;
        this.recipeDir = recipeDir;
    }

    public Config getConfig() {
        return config;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getRecipeDir() {
        return recipeDir;
    }

    /**
     * Get all recipe-scoped variables set during this recipe's execution.
     * @return map of recipe variables
     */
    public Map<String, String> getRecipeVariables() {
        return recipeVariables;
    }

    /**
     * Set a recipe-scoped variable.
     * @param name variable name
     * @param value variable value
     */
    public void setRecipeVariable(String name, String value) {
        recipeVariables.put(name, value);
    }

    /**
     * Get a recipe-scoped variable.
     * @param name variable name
     * @return variable value, or null if not set
     */
    public String getRecipeVariable(String name) {
        return recipeVariables.get(name);
    }
}

