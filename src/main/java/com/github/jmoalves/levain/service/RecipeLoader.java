package com.github.jmoalves.levain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.jmoalves.levain.model.Recipe;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Loads recipes from YAML files in a recipes directory.
 * Only accepts .levain.yaml extension. Files with other extensions are
 * rejected.
 */
@ApplicationScoped
public class RecipeLoader {
    private static final Logger logger = LoggerFactory.getLogger(RecipeLoader.class);
    private static final String RECIPE_EXTENSION = ".levain.yaml";
    private static final String[] REJECTED_EXTENSIONS = { ".levain.yml", ".levain", ".yml", ".yaml" };

    private final ObjectMapper yamlMapper;

    public RecipeLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Load all recipes from a directory.
     *
     * @param recipesDir the directory containing recipe files
     * @return a map of recipe name to Recipe object
     */
    public Map<String, Recipe> loadRecipesFromDirectory(String recipesDir) {
        Map<String, Recipe> recipes = new LinkedHashMap<>();

        File dir = new File(recipesDir);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Recipes directory does not exist: {}", recipesDir);
            return recipes;
        }

        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isRecipeFile)
                    .forEach(path -> {
                        try {
                            Recipe recipe = loadRecipe(path.toFile());
                            if (recipe != null) {
                                recipes.put(recipe.getName(), recipe);
                            }
                        } catch (IOException e) {
                            logger.error("Failed to load recipe from {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to read recipes directory {}: {}", recipesDir, e.getMessage());
        }

        logger.info("Loaded {} recipes from {}", recipes.size(), recipesDir);
        return recipes;
    }

    /**
     * Load a single recipe from a file.
     *
     * @param file the recipe file
     * @return the Recipe object
     * @throws IOException if reading or parsing fails
     */
    public Recipe loadRecipe(File file) throws IOException {
        logger.debug("Loading recipe from: {}", file.getAbsolutePath());
        String yamlContent = Files.readString(file.toPath());
        Recipe recipe = yamlMapper.readValue(yamlContent, Recipe.class);

        // Extract recipe name from filename
        String fileName = file.getName();
        String recipeName = extractRecipeName(fileName);
        recipe.setName(recipeName);

        // Normalize dependencies to ensure no nulls
        if (recipe.getDependencies() == null) {
            recipe.setDependencies(new ArrayList<>());
        }

        applyCommandsFromYaml(yamlContent, recipe);

        logger.debug("Loaded recipe: {} version {}", recipe.getName(), recipe.getVersion());
        return recipe;
    }

    /**
     * Check if a file is a valid recipe file based on extension.
     * Only .levain.yaml files are accepted.
     *
     * @param path the file path
     * @return true if it's a valid recipe file (.levain.yaml), false otherwise
     */
    private boolean isRecipeFile(Path path) {
        String fileName = path.getFileName().toString();

        // Accept only .levain.yaml
        if (fileName.endsWith(RECIPE_EXTENSION)) {
            // Validate no double extensions
            if (!fileName.contains(RECIPE_EXTENSION + RECIPE_EXTENSION)) {
                return true;
            } else {
                logger.debug("Rejected recipe file with invalid name (multiple .levain.yaml): {}", fileName);
                return false;
            }
        }

        // Reject files with other recipe-like extensions (debug level - this is
        // expected)
        for (String rejected : REJECTED_EXTENSIONS) {
            if (fileName.endsWith(rejected)) {
                logger.debug("Rejected recipe file with wrong extension (expected .levain.yaml, got {}): {}", rejected,
                        fileName);
                return false;
            }
        }

        return false;
    }

    /**
     * Extract recipe name from filename.
     * Removes the .levain.yaml extension.
     *
     * @param fileName the file name
     * @return the recipe name (without .levain.yaml)
     */
    private String extractRecipeName(String fileName) {
        if (fileName.endsWith(RECIPE_EXTENSION)) {
            return fileName.substring(0, fileName.length() - RECIPE_EXTENSION.length());
        }
        return fileName;
    }

    /**
     * Parse a recipe from YAML string.
     * Static method for use by remote repositories and other sources.
     *
     * @param yamlContent the YAML content as string
     * @param recipeName  the recipe name to assign
     * @return the Recipe object
     */
    public static Recipe parseRecipeYaml(String yamlContent, String recipeName) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Recipe recipe = mapper.readValue(yamlContent, Recipe.class);
            recipe.setName(recipeName);

            // Normalize dependencies
            if (recipe.getDependencies() == null) {
                recipe.setDependencies(new ArrayList<>());
            }

            applyCommandsFromYaml(mapper, yamlContent, recipe);

            return recipe;
        } catch (Exception e) {
            LogManager.getLogger(RecipeLoader.class)
                    .error("Failed to parse recipe YAML: {}", e.getMessage());
            throw new RuntimeException("Failed to parse recipe: " + e.getMessage(), e);
        }
    }

    /**
     * Get the default recipes directory location.
     * In production, recipes should come from external repositories (git, zip,
     * etc).
     * For example: https://github.com/jmoalves/levain-pkgs
     *
     * @return the recipes directory path, or null if not configured
     */
    public static String getDefaultRecipesDirectory() {
        // Check for system property or environment variable
        String recipesDir = System.getProperty("levain.recipes.dir");
        if (recipesDir == null) {
            recipesDir = System.getenv("LEVAIN_RECIPES_DIR");
        }

        // Convert to absolute path if set
        if (recipesDir != null) {
            File dir = new File(recipesDir);
            if (!dir.isAbsolute()) {
                dir = dir.getAbsoluteFile();
            }
            if (dir.exists() && dir.isDirectory()) {
                recipesDir = dir.getAbsolutePath();
            } else {
                recipesDir = null;
            }
        }

        // Check standard locations
        if (recipesDir == null) {
            String userHome = System.getProperty("user.home");
            String levainHome = userHome + File.separator + "levain";
            String standardRecipesDir = levainHome + File.separator + "levain-pkgs" + File.separator + "recipes";

            File dir = new File(standardRecipesDir);
            if (dir.exists() && dir.isDirectory()) {
                recipesDir = standardRecipesDir;
            }
        }

        return recipesDir;
    }

    private void applyCommandsFromYaml(String yamlContent, Recipe recipe) throws IOException {
        applyCommandsFromYaml(this.yamlMapper, yamlContent, recipe);
    }

    private static void applyCommandsFromYaml(ObjectMapper mapper, String yamlContent, Recipe recipe)
            throws IOException {
        if (yamlContent == null || yamlContent.isBlank() || recipe == null) {
            return;
        }

        Map<String, Object> raw = mapper.readValue(yamlContent, new TypeReference<Map<String, Object>>() {
        });
        if (raw == null || raw.isEmpty()) {
            return;
        }

        Map<String, List<String>> commands = recipe.getCommands() != null
                ? new LinkedHashMap<>(recipe.getCommands())
                : new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("cmd.")) {
                continue;
            }
            String commandName = key.substring("cmd.".length());
            List<String> commandList = toStringList(entry.getValue());
            if (commandList != null) {
                commands.put(commandName, commandList);
            }
        }

        if (!commands.isEmpty()) {
            recipe.setCommands(commands);
        }
    }

    private static List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        if (value instanceof String str) {
            return List.of(str);
        }
        return null;
    }
}
