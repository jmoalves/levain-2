package com.github.jmoalves.levain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry of installed recipes.
 * 
 * The registry maintains an inventory of installed recipes by storing copies
 * of the recipe YAML files that were used during installation. This provides:
 * 
 * - Audit trail: Know what was installed and when
 * - Recovery: Can reinstall from registry if original source is unavailable
 * - Versioning: Can track which version of a recipe was installed
 * - Debugging: Check exact recipe configuration that was used
 * 
 * The registry is implemented as a directory repository and supports all
 * standard repository operations (list, resolve, etc.).
 * 
 * Default location: ~/.levain/registry/
 * Each recipe is stored as a YAML file: {recipeName}.yml
 */
public class Registry implements Repository {
    private static final Logger logger = LogManager.getLogger(Registry.class);
    private static final String DEFAULT_REGISTRY_DIR = System.getProperty("user.home") + "/.levain/registry";

    private final Path registryPath;
    private boolean initialized = false;

    /**
     * Create a registry with the default location (~/.levain/registry).
     */
    public Registry() {
        this(DEFAULT_REGISTRY_DIR);
    }

    /**
     * Create a registry at a custom location.
     * 
     * @param registryDir The directory to use for the registry
     */
    public Registry(String registryDir) {
        this.registryPath = Paths.get(registryDir);
        logger.debug("Registry initialized with path: {}", registryPath);
    }

    @Override
    public String describe() {
        return "Registry[" + registryPath.toAbsolutePath() + "]";
    }

    @Override
    public String getName() {
        return "registry";
    }

    @Override
    public String getUri() {
        return "registry://" + registryPath.toAbsolutePath();
    }

    @Override
    public void init() {
        try {
            // Ensure registry directory exists
            if (!Files.exists(registryPath)) {
                Files.createDirectories(registryPath);
                logger.info("Created registry directory: {}", registryPath.toAbsolutePath());
            }
            initialized = true;
            logger.debug("Registry initialized successfully at: {}", registryPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to initialize registry at {}: {}", registryPath, e.getMessage());
            throw new RuntimeException("Cannot initialize registry: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public List<Recipe> listRecipes() {
        ensureInitialized();
        List<Recipe> recipes = new ArrayList<>();

        try {
            File[] files = registryPath.toFile()
                    .listFiles((dir, name) -> name.endsWith(".levain.yaml"));

            if (files != null) {
                for (File file : files) {
                    try {
                        String yamlContent = Files.readString(file.toPath());
                        String recipeName = extractRecipeName(file.getName());
                        Recipe recipe = RecipeLoader.parseRecipeYaml(yamlContent, recipeName);
                        recipes.add(recipe);
                    } catch (Exception e) {
                        logger.warn("Failed to load recipe from registry file {}: {}",
                                file.getName(), e.getMessage());
                    }
                }
            }
            logger.debug("Listed {} recipes from registry", recipes.size());
        } catch (Exception e) {
            logger.error("Failed to list recipes from registry: {}", e.getMessage());
        }

        return recipes;
    }

    @Override
    public Optional<Recipe> resolveRecipe(String recipeName) {
        ensureInitialized();

        try {
            Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
            if (Files.exists(recipePath)) {
                String yamlContent = Files.readString(recipePath);
                Recipe recipe = RecipeLoader.parseRecipeYaml(yamlContent, recipeName);
                logger.debug("Resolved recipe '{}' from registry", recipeName);
                return Optional.of(recipe);
            }
            logger.debug("Recipe '{}' not found in registry", recipeName);
        } catch (Exception e) {
            logger.error("Failed to resolve recipe '{}' from registry: {}", recipeName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeYamlContent(String recipeName) {
        ensureInitialized();

        try {
            // Recipe files are always stored as .levain.yaml
            Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
            if (Files.exists(recipePath)) {
                return Optional.of(Files.readString(recipePath));
            }
        } catch (IOException e) {
            logger.error("Failed to read YAML content for '{}' from registry: {}", recipeName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        ensureInitialized();

        // All recipes in registry use .levain.yaml extension
        // Check for malformed names
        if (recipeName.contains(".levain.yaml")) {
            logger.warn("Recipe name contains .levain.yaml: {}", recipeName);
            return Optional.empty();
        }

        Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
        if (Files.exists(recipePath)) {
            return Optional.of(recipeName + ".levain.yaml");
        }

        return Optional.empty();
    }

    @Override
    public int size() {
        ensureInitialized();
        try {
            File[] files = registryPath.toFile()
                    .listFiles((dir, name) -> name.endsWith(".levain.yaml"));
            return files != null ? files.length : 0;
        } catch (Exception e) {
            logger.error("Failed to get registry size: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Store a recipe in the registry.
     * All recipes are stored with the .levain.yaml extension.
     * 
     * @param recipe        The recipe to store
     * @param yamlContent   The YAML content of the recipe
     * @param fileName      The original filename (ignored, always uses
     *                      .levain.yaml)
     * @param sourceRepo    The source repository name (optional)
     * @param sourceRepoUri The source repository URI (optional)
     */
    public void store(Recipe recipe, String yamlContent, String fileName, String sourceRepo, String sourceRepoUri) {
        ensureInitialized();

        // Validate recipe name doesn't already contain .levain.yaml
        String recipeName = recipe.getName();
        if (recipeName.contains(".levain.yaml")) {
            throw new IllegalArgumentException(
                    "Recipe name contains invalid extension: " + recipeName +
                            " (recipe names should not contain .levain.yaml)");
        }

        try {
            // All recipes stored with standardized .levain.yaml extension
            String standardizedFileName = recipeName + ".levain.yaml";
            Path recipePath = registryPath.resolve(standardizedFileName);

            Files.writeString(recipePath, yamlContent);
            logger.info("Stored recipe '{}' in registry: {}", recipeName, recipePath.toAbsolutePath());

            if (sourceRepo != null || sourceRepoUri != null) {
                storeMetadata(recipeName, sourceRepo, sourceRepoUri, recipe.getVersion());
            }
        } catch (IOException e) {
            logger.error("Failed to store recipe '{}' in registry: {}", recipeName, e.getMessage());
            throw new RuntimeException("Cannot store recipe in registry: " + e.getMessage(), e);
        }
    }

    /**
     * Store a recipe in the registry with standardized .levain.yaml extension.
     * 
     * @param recipe      The recipe to store
     * @param yamlContent The YAML content of the recipe
     */
    public void store(Recipe recipe, String yamlContent) {
        store(recipe, yamlContent, null, null, null);
    }

    /**
     * Store a recipe in the registry with standardized .levain.yaml extension.
     * 
     * @param recipe      The recipe to store
     * @param yamlContent The YAML content of the recipe
     * @param fileName    The original filename (ignored, always uses .levain.yaml)
     */
    public void store(Recipe recipe, String yamlContent, String fileName) {
        store(recipe, yamlContent, fileName, null, null);
    }

    /**
     * Store a recipe in the registry with metadata about its source.
     * 
     * @param recipe        The recipe to store
     * @param yamlContent   The YAML content of the recipe
     * @param sourceRepo    The source repository name (optional)
     * @param sourceRepoUri The source repository URI (optional)
     */
    public void store(Recipe recipe, String yamlContent, String sourceRepo, String sourceRepoUri) {
        store(recipe, yamlContent, null, sourceRepo, sourceRepoUri);
    }

    /**
     * Check if a recipe is installed (exists in the registry).
     * 
     * @param recipeName The name of the recipe
     * @return true if the recipe is in the registry, false otherwise
     */
    public boolean isInstalled(String recipeName) {
        ensureInitialized();

        Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
        return Files.exists(recipePath);
    }

    /**
     * Get the path to a recipe file in the registry.
     * 
     * @param recipeName The name of the recipe
     * @return The path if found, or empty Optional
     */
    public Optional<Path> getRecipePath(String recipeName) {
        ensureInitialized();

        Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
        if (Files.exists(recipePath)) {
            return Optional.of(recipePath);
        }
        return Optional.empty();
    }

    /**
     * Remove a recipe from the registry.
     * 
     * @param recipeName The name of the recipe to remove
     * @return true if removed, false if not found
     */
    public boolean remove(String recipeName) {
        ensureInitialized();

        Path recipePath = registryPath.resolve(recipeName + ".levain.yaml");
        Path metadataPath = registryPath.resolve(recipeName + ".levain.meta");
        try {
            if (Files.exists(recipePath)) {
                Files.delete(recipePath);
                if (Files.exists(metadataPath)) {
                    Files.delete(metadataPath);
                }
                logger.info("Removed recipe '{}' from registry", recipeName);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to remove recipe '{}' from registry: {}", recipeName, e.getMessage());
        }
        return false;
    }

    /**
     * Clear all recipes from the registry.
     * Use with caution!
     */
    public void clear() {
        ensureInitialized();

        try {
            File[] files = registryPath.toFile()
                    .listFiles((dir, name) -> name.endsWith(".levain.yaml") || name.endsWith(".levain.meta"));
            if (files != null) {
                for (File file : files) {
                    Files.delete(file.toPath());
                }
            }
            logger.info("Cleared all recipes from registry");
        } catch (IOException e) {
            logger.error("Failed to clear registry: {}", e.getMessage());
            throw new RuntimeException("Cannot clear registry: " + e.getMessage(), e);
        }
    }

    /**
     * Get the absolute path to the registry directory.
     * 
     * @return The registry directory path
     */
    public Path getRegistryPath() {
        return registryPath.toAbsolutePath();
    }

    /**
     * Get the default registry location.
     * 
     * @return The default registry directory path
     */
    public static String getDefaultRegistryPath() {
        return DEFAULT_REGISTRY_DIR;
    }

    private void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    private String extractRecipeName(String fileName) {
        return fileName.replaceAll("\\.levain\\.yaml$", "");
    }

    /**
     * Store metadata about an installed recipe.
     * 
     * @param recipeName    The name of the recipe
     * @param sourceRepo    The source repository name
     * @param sourceRepoUri The source repository URI
     * @param version       The recipe version
     */
    private void storeMetadata(String recipeName, String sourceRepo, String sourceRepoUri, String version) {
        try {
            RecipeMetadata metadata = new RecipeMetadata(recipeName, sourceRepo, sourceRepoUri);
            metadata.setInstalledVersion(version);

            Path metadataPath = registryPath.resolve(recipeName + ".levain.meta");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);

            logger.debug("Stored metadata for recipe '{}': source={}, uri={}",
                    recipeName, sourceRepo, sourceRepoUri);
        } catch (IOException e) {
            logger.warn("Failed to store metadata for recipe '{}': {}", recipeName, e.getMessage());
            // Don't fail the installation if metadata storage fails
        }
    }

    /**
     * Load metadata about an installed recipe.
     * 
     * @param recipeName The name of the recipe
     * @return Optional containing the metadata if found
     */
    public Optional<RecipeMetadata> getMetadata(String recipeName) {
        ensureInitialized();

        try {
            Path metadataPath = registryPath.resolve(recipeName + ".levain.meta");
            if (Files.exists(metadataPath)) {
                ObjectMapper mapper = new ObjectMapper();
                RecipeMetadata metadata = mapper.readValue(metadataPath.toFile(), RecipeMetadata.class);
                return Optional.of(metadata);
            }
        } catch (IOException e) {
            logger.warn("Failed to load metadata for recipe '{}': {}", recipeName, e.getMessage());
        }

        return Optional.empty();
    }
}
