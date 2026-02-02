package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import java.util.List;
import java.util.Optional;

/**
 * Contract for recipe repositories.
 * Repositories can load recipes from various sources: directories, zip files,
 * git repositories, JAR resources, etc.
 */
public interface Repository {
    /**
     * Initialize the repository, making it ready to load recipes.
     */
    void init();

    /**
     * Check if the repository has been initialized.
     */
    boolean isInitialized();

    /**
     * Get the repository name.
     */
    String getName();

    /**
     * Get the repository URI (source location).
     */
    String getUri();

    /**
     * List all recipes in this repository.
     */
    List<Recipe> listRecipes();

    /**
     * Resolve a recipe by name.
     */
    Optional<Recipe> resolveRecipe(String recipeName);

    /**
     * Get the number of recipes in this repository.
     */
    int size();

    /**
     * Get a description of the repository.
     */
    String describe();
}
