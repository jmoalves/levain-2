package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;

import java.nio.file.Path;

/**
 * Context for action execution.
 */
public class ActionContext {
    private final Config config;
    private final Recipe recipe;
    private final Path baseDir;
    private final Path recipeDir;

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
}
