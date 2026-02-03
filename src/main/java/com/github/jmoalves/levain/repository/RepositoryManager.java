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
     * JAR recipes (ResourceRepository) are always included and take priority.
     * Recipes from other sources are only included if not present in JAR.
     * Recipes are deduplicated by name (JAR recipes win).
     */
    public List<Recipe> listRecipes() {
        List<Recipe> allRecipes = new ArrayList<>();
        List<String> seenNames = new ArrayList<>();

        // First, collect all JAR recipes (from ResourceRepository) - these have
        // absolute priority
        Optional<ResourceRepository> jarRepo = repositories.stream()
                .filter(repo -> repo instanceof ResourceRepository)
                .map(repo -> (ResourceRepository) repo)
                .findFirst();

        if (jarRepo.isPresent()) {
            for (Recipe recipe : jarRepo.get().listRecipes()) {
                allRecipes.add(recipe);
                seenNames.add(recipe.getName());
                logger.debug("Added JAR recipe: {}", recipe.getName());
            }
        }

        // Then, add recipes from other repositories, but only if not already in JAR
        for (Repository repository : repositories) {
            // Skip ResourceRepository as we already processed it
            if (repository instanceof ResourceRepository) {
                continue;
            }

            for (Recipe recipe : repository.listRecipes()) {
                if (!seenNames.contains(recipe.getName())) {
                    allRecipes.add(recipe);
                    seenNames.add(recipe.getName());
                    logger.debug("Added recipe from {}: {}", repository.describe(), recipe.getName());
                } else {
                    logger.debug("Skipping recipe '{}' from {} - already exists in JAR",
                            recipe.getName(), repository.describe());
                }
            }
        }

        return allRecipes;
    }

    /**
     * Resolve a recipe by searching all repositories in order.
     * JAR recipes (ResourceRepository) are always checked first and have priority.
     */
    public Optional<Recipe> resolveRecipe(String recipeName) {
        // Always try JAR recipes first
        Optional<ResourceRepository> jarRepo = repositories.stream()
                .filter(repo -> repo instanceof ResourceRepository)
                .map(repo -> (ResourceRepository) repo)
                .findFirst();

        if (jarRepo.isPresent()) {
            Optional<Recipe> recipe = jarRepo.get().resolveRecipe(recipeName);
            if (recipe.isPresent()) {
                logger.debug("Resolved recipe '{}' from JAR (ResourceRepository)", recipeName);
                return recipe;
            }
        }

        // If not in JAR, search other repositories
        for (Repository repository : repositories) {
            // Skip ResourceRepository as we already checked it
            if (repository instanceof ResourceRepository) {
                continue;
            }

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
     * Find the repository that provides a given recipe.
     * JAR recipes (ResourceRepository) are always checked first and have priority.
     * 
     * @param recipeName The recipe name
     * @return Optional containing the repository if found
     */
    public Optional<Repository> findRepositoryForRecipe(String recipeName) {
        // Always try JAR recipes first
        Optional<ResourceRepository> jarRepo = repositories.stream()
                .filter(repo -> repo instanceof ResourceRepository)
                .map(repo -> (ResourceRepository) repo)
                .findFirst();

        if (jarRepo.isPresent()) {
            Optional<Recipe> recipe = jarRepo.get().resolveRecipe(recipeName);
            if (recipe.isPresent()) {
                return Optional.of(jarRepo.get());
            }
        }

        // If not in JAR, search other repositories
        for (Repository repository : repositories) {
            // Skip ResourceRepository as we already checked it
            if (repository instanceof ResourceRepository) {
                continue;
            }

            Optional<Recipe> recipe = repository.resolveRecipe(recipeName);
            if (recipe.isPresent()) {
                return Optional.of(repository);
            }
        }
        return Optional.empty();
    }

    /**
     * Get the original YAML content for a recipe by searching all repositories.
     */
    public Optional<String> getRecipeYamlContent(String recipeName) {
        for (Repository repository : repositories) {
            Optional<String> yaml = repository.getRecipeYamlContent(recipeName);
            if (yaml.isPresent()) {
                logger.debug("Found YAML content for '{}' from {}", recipeName, repository.describe());
                return yaml;
            }
        }
        logger.warn("YAML content for '{}' not found in any repository", recipeName);
        return Optional.empty();
    }

    /**
     * Get the original filename for a recipe by searching all repositories.
     */
    public Optional<String> getRecipeFileName(String recipeName) {
        for (Repository repository : repositories) {
            Optional<String> fileName = repository.getRecipeFileName(recipeName);
            if (fileName.isPresent()) {
                logger.debug("Found filename for '{}' from {}", recipeName, repository.describe());
                return fileName;
            }
        }
        logger.warn("Filename for '{}' not found in any repository", recipeName);
        return Optional.empty();
    }

    /**
     * Get the number of repositories.
     */
    public int getRepositoryCount() {
        return repositories.size();
    }

    /**
     * Get the Registry repository if it exists.
     * 
     * @return Optional containing the Registry if found
     */
    public Optional<Registry> getRegistry() {
        return repositories.stream()
                .filter(repo -> repo instanceof Registry)
                .map(repo -> (Registry) repo)
                .findFirst();
    }

    /**
     * Check if a recipe is installed (exists in the Registry).
     * 
     * @param recipeName The name of the recipe
     * @return true if the recipe is installed, false otherwise
     */
    public boolean isInstalled(String recipeName) {
        return getRegistry()
                .map(registry -> registry.isInstalled(recipeName))
                .orElse(false);
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
