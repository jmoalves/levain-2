package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages multiple repositories in a chain.
 * Repositories are searched in order, with the first one to resolve a recipe
 * winning.
 */
@ApplicationScoped
public class RepositoryManager {
    private static final Logger logger = LogManager.getLogger(RepositoryManager.class);
    private final List<Repository> repositories = new ArrayList<>();

    /**
     * Add a repository to the chain.
     */
    public void addRepository(Repository repository) {
        repositories.add(repository);
        repository.init();
        logger.debug("Added repository: {}", repository.describe());
    }

    /**
     * Initialize all repositories.
     */
    public void init() {
        logger.debug("Initializing RepositoryManager with {} repositories", repositories.size());
        for (Repository repository : repositories) {
            try {
                repository.init();
            } catch (Exception e) {
                logger.warn("Failed to initialize repository {}: {}", repository.describe(), e.getMessage());
            }
        }
    }

    /**
     * List all recipes from all repositories.
     * Recipes are deduplicated by name (first found wins).
     */
    public List<Recipe> listRecipes() {
        List<Recipe> allRecipes = new ArrayList<>();
        List<String> seenNames = new ArrayList<>();

        for (Repository repository : repositories) {
            for (Recipe recipe : repository.listRecipes()) {
                if (!seenNames.contains(recipe.getName())) {
                    allRecipes.add(recipe);
                    seenNames.add(recipe.getName());
                }
            }
        }

        return allRecipes;
    }

    /**
     * Resolve a recipe by searching all repositories in order.
     */
    public Optional<Recipe> resolveRecipe(String recipeName) {
        for (Repository repository : repositories) {
            Optional<Recipe> recipe = repository.resolveRecipe(recipeName);
            if (recipe.isPresent()) {
                logger.debug("Resolved recipe '{}' from {}", recipeName, repository.describe());
                return recipe;
            }
        }
        logger.warn("Recipe '{}' not found in any repository", recipeName);
        return Optional.empty();
    }

    /**
     * Get the number of repositories.
     */
    public int getRepositoryCount() {
        return repositories.size();
    }

    /**
     * Describe all repositories.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("RepositoryManager:\n");
        for (Repository repository : repositories) {
            sb.append("  - ").append(repository.describe()).append("\n");
        }
        return sb.toString();
    }
}
