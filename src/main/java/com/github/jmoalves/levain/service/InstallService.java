package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.action.ActionContext;
import com.github.jmoalves.levain.action.ActionExecutor;
import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.repository.Repository;
import com.github.jmoalves.levain.repository.RepositoryFactory;
import com.github.jmoalves.levain.repository.Registry;
import com.github.jmoalves.levain.util.VersionNumber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

/**
 * Service for installing packages.
 */
@ApplicationScoped
public class InstallService {
    private static final Logger logger = LoggerFactory.getLogger(InstallService.class);
    private static final String LEVAIN_VERSION = "2.0.0";

    private final RecipeService recipeService;
    private final RepositoryFactory repositoryFactory;
    private final VariableSubstitutionService variableSubstitutionService;
    private final ActionExecutor actionExecutor;
    private final Config config;
    private final DependencyResolver dependencyResolver;
    private Registry registry;

    @Inject
    public InstallService(RecipeService recipeService,
            RepositoryFactory repositoryFactory,
            VariableSubstitutionService variableSubstitutionService,
            ActionExecutor actionExecutor,
            Config config,
            DependencyResolver dependencyResolver) {
        this.recipeService = recipeService;
        this.repositoryFactory = repositoryFactory;
        this.variableSubstitutionService = variableSubstitutionService;
        this.actionExecutor = actionExecutor;
        this.config = config;
        this.dependencyResolver = dependencyResolver;
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
        logger.info("Analyzing installation for: {} (force={})", packageName, force);

        List<Recipe> toInstall = buildInstallationPlan(List.of(packageName), force);
        if (toInstall.isEmpty()) {
            logger.info("All packages already installed");
            return;
        }

        System.out.println("\n" + dependencyResolver.formatInstallationPlan(toInstall));
        installPlan(toInstall);
    }

    public List<Recipe> buildInstallationPlan(List<String> packageNames, boolean force) {
        if (packageNames == null || packageNames.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, Recipe> ordered = new LinkedHashMap<>();
        for (String packageName : packageNames) {
            List<Recipe> installationPlan = dependencyResolver.resolveAndSort(packageName);
            for (Recipe recipe : installationPlan) {
                ordered.putIfAbsent(recipe.getName(), recipe);
            }
        }

        if (registry == null) {
            registry = new Registry();
            registry.init();
        }

        List<Recipe> toInstall = new ArrayList<>();
        for (Recipe recipe : ordered.values()) {
            if (force || !registry.isInstalled(recipe.getName())) {
                toInstall.add(recipe);
            } else {
                logger.info("Package already installed (skipping): {}", recipe.getName());
            }
        }

        return toInstall;
    }

    public void installPlan(List<Recipe> plan) {
        if (plan == null || plan.isEmpty()) {
            return;
        }
        for (Recipe recipe : plan) {
            installSingleRecipe(recipe.getName());
        }
    }

    public String formatInstallationPlan(List<Recipe> plan) {
        return dependencyResolver.formatInstallationPlan(plan);
    }

    /**
     * Install a single recipe (internal method).
     * Assumes all dependencies are already installed.
     */
    private void installSingleRecipe(String packageName) {
        logger.info("Installing package: {}", packageName);

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
                var sourceRepo = recipeService.findSourceRepository(packageName).orElse(null);
                installRecipe(recipe, serializeRecipeToYaml(recipe),
                        sourceRepo != null ? sourceRepo.getName() : null,
                        sourceRepo != null ? sourceRepo.getUri() : null);
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize recipe: " + e.getMessage(), e);
            }
        } else {
            var sourceRepo = recipeService.findSourceRepository(packageName).orElse(null);
            installRecipe(recipe, yamlContent.get(),
                    sourceRepo != null ? sourceRepo.getName() : null,
                    sourceRepo != null ? sourceRepo.getUri() : null);
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
        installRecipe(recipe.get(), yamlContent.get(), repository.getName(), repository.getUri());
    }

    /**
     * Install a recipe (store in registry).
     * This first approach stores the recipe in the registry without executing it.
     * Recipes are always stored with .levain.yaml extension.
     *
     * @param recipe       The recipe to install
     * @param originalYaml The original YAML content to preserve
     */
    private void installRecipe(Recipe recipe, String originalYaml, String sourceRepo, String sourceRepoUri) {
        try {
            logger.info("Processing recipe: {}", recipe.getName());

            // Check minVersion requirement (levain.minVersion attribute)
            String minVersionStr = recipe.getMinVersion();
            if (minVersionStr != null && !minVersionStr.isBlank()) {
                VersionNumber minVersion = new VersionNumber(minVersionStr);
                VersionNumber currentVersion = new VersionNumber(LEVAIN_VERSION);
                
                if (minVersion.isNewerThan(currentVersion)) {
                    String msg = String.format(
                        "Recipe '%s' requires Levain %s or newer (current: %s)",
                        recipe.getName(),
                        minVersion,
                        currentVersion
                    );
                    logger.warn(msg);
                    throw new RuntimeException(msg);
                }
            }

            // Lazy initialize registry
            if (registry == null) {
                registry = new Registry();
                registry.init();
            }

            // Create baseDir only if recipe doesn't skip it
            var baseDir = config.getLevainHome().resolve(recipe.getName());
            if (!recipe.shouldSkipInstallDir()) {
                Files.createDirectories(baseDir);
            }
            
            var recipeDir = recipe.getRecipesDir() != null ? Path.of(recipe.getRecipesDir()) : null;
            variableSubstitutionService.substituteRecipeCommands(recipe, baseDir);
            
            // Execute cmd.install and cmd.env actions during installation
            // Following original Levain pattern: install.ts appends cmd.env after cmd.install
            List<String> actions = new ArrayList<>();
            if (recipe.getCommands() != null) {
                List<String> installActions = recipe.getCommands().get("install");
                if (installActions != null) {
                    actions.addAll(installActions);
                }
                
                // cmd.env contains environment setup actions that run both during
                // installation (to configure the installed package) and during shell
                // execution (to provide session-scoped environment)
                List<String> envActions = recipe.getCommands().get("env");
                if (envActions != null) {
                    actions.addAll(envActions);
                }
            }
            
            actionExecutor.executeCommands(actions, new ActionContext(config, recipe, baseDir, recipeDir));

            // For now: Store recipe in registry with original YAML content
            // Registry stores all recipes as {name}.levain.yaml
            registry.store(recipe, originalYaml, sourceRepo, sourceRepoUri);

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
