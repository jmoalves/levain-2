package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.repository.Repository;
import com.github.jmoalves.levain.repository.RepositoryFactory;
import com.github.jmoalves.levain.repository.Registry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for installing packages.
 */
@ApplicationScoped
public class InstallService {
    private static final Logger logger = LoggerFactory.getLogger(InstallService.class);

    private final RecipeService recipeService;
    private final RepositoryFactory repositoryFactory;
    private Registry registry;

    @Inject
    public InstallService(RecipeService recipeService, RepositoryFactory repositoryFactory) {
        this.recipeService = recipeService;
        this.repositoryFactory = repositoryFactory;
        this.registry = null; // Lazy initialize in installRecipe
    }

    /**
     * Install a package by name using the default repositories.
     *
     * @param packageName Name of the package to install
     * @throws IllegalArgumentException if recipe is not found
     * @throws RuntimeException         if installation fails
     */
    public void install(String packageName) {
        install(packageName, false);
    }

    /**
     * Install a package by name using the default repositories.
     * If the package is already installed and force is false, it will be skipped.
     *
     * @param packageName Name of the package to install
     * @param force       If true, reinstall even if already installed
     * @throws IllegalArgumentException if recipe is not found
     * @throws RuntimeException         if installation fails
     */
    public void install(String packageName, boolean force) {
        logger.info("Installing package: {} (force={})", packageName, force);

        // Check if already installed first
        if (registry == null) {
            registry = new Registry();
            registry.init();
        }
        if (!force && registry.isInstalled(packageName)) {
            logger.info("Package already installed (skipping): {}", packageName);
            throw new AlreadyInstalledException(packageName + " is already installed (use --force to reinstall)");
        }

        // Load recipe from default repositories
        Recipe recipe = recipeService.loadRecipe(packageName);

        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found: " + packageName);
        }

        // Get original YAML content (recipes are stored as-is in registry)
        var yamlContent = recipeService.getRecipeYamlContent(packageName);

        if (yamlContent.isEmpty()) {
            logger.warn("Could not get YAML content for {}, using serialized version", packageName);
            try {
                installRecipe(recipe, serializeRecipeToYaml(recipe));
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize recipe: " + e.getMessage(), e);
            }
        } else {
            installRecipe(recipe, yamlContent.get());
        }
    }

    /**
     * Install a package from a specific repository.
     *
     * @param packageName   Name of the package to install
     * @param repositoryUri Repository URI (git, directory, zip, or http URL)
     * @throws IllegalArgumentException if recipe is not found
     * @throws RuntimeException         if installation fails
     */
    public void install(String packageName, String repositoryUri) {
        logger.info("Installing package: {} from repository: {}", packageName, repositoryUri);

        // Create repository from URI
        Repository repository = repositoryFactory.createRepository(repositoryUri);
        repository.init();

        // Load recipe from specified repository
        var recipe = repository.resolveRecipe(packageName);

        if (recipe.isEmpty()) {
            throw new IllegalArgumentException("Recipe not found in repository: " + packageName);
        }

        // Get original YAML content to preserve all fields
        var yamlContent = repository.getRecipeYamlContent(packageName);
        if (yamlContent.isEmpty()) {
            throw new IllegalArgumentException("Recipe YAML content not found for: " + packageName);
        }

        // Install with original YAML content (filename is always .levain.yaml)
        installRecipe(recipe.get(), yamlContent.get());
    }

    /**
     * Install a recipe (store in registry).
     * This first approach stores the recipe in the registry without executing it.
     * Recipes are always stored with .levain.yaml extension.
     *
     * @param recipe       The recipe to install
     * @param originalYaml The original YAML content to preserve
     */
    private void installRecipe(Recipe recipe, String originalYaml) {
        try {
            logger.info("Processing recipe: {}", recipe.getName());

            // Lazy initialize registry
            if (registry == null) {
                registry = new Registry();
                registry.init();
            }

            // TODO: In future phases:
            // 1. Download package if needed
            // 2. Extract/install to appropriate location
            // 3. Execute install commands from recipe
            // 4. Update environment

            // For now: Store recipe in registry with original YAML content
            // Registry stores all recipes as {name}.levain.yaml
            registry.store(recipe, originalYaml);

            logger.info("Recipe {} stored in registry", recipe.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to install recipe " + recipe.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Serialize recipe to YAML format.
     */
    private String serializeRecipeToYaml(Recipe recipe) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recipe);
    }

    /**
     * Get the registry for querying installed recipes.
     */
    public Registry getRegistry() {
        if (registry == null) {
            registry = new Registry();
            registry.init();
        }
        return registry;
    }

    /**
     * Check if a recipe is already installed in the registry.
     *
     * @param recipeName Name of the recipe
     * @return true if the recipe is installed, false otherwise
     */
    public boolean isInstalled(String recipeName) {
        return getRegistry().isInstalled(recipeName);
    }
}
