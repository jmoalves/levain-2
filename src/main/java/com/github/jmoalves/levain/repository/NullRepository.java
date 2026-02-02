package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Null Object pattern implementation of Repository.
 * 
 * This repository always returns empty results. It's useful as a default
 * repository when no actual repositories are configured, or as a placeholder
 * in testing scenarios.
 * 
 * All operations are no-ops and return empty results.
 */
public class NullRepository implements Repository {
    public static final String NULL_REPOSITORY_NAME = "nullRepository";
    public static final String NULL_REPOSITORY_URI = "null://";

    @Override
    public String describe() {
        return NULL_REPOSITORY_NAME;
    }

    @Override
    public String getName() {
        return NULL_REPOSITORY_NAME;
    }

    @Override
    public String getUri() {
        return NULL_REPOSITORY_URI;
    }

    @Override
    public void init() {
        // No-op: nothing to initialize
    }

    @Override
    public List<Recipe> listRecipes() {
        return new ArrayList<>();
    }

    @Override
    public Optional<Recipe> resolveRecipe(String recipeName) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeYamlContent(String recipeName) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        return Optional.empty();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isInitialized() {
        return true; // Trivially initialized since it has nothing to do
    }
}
