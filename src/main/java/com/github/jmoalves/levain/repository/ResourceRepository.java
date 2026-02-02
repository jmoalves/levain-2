package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from JAR resources.
 * This is useful for built-in recipes packaged with the Levain application.
 */
@Dependent
public class ResourceRepository extends AbstractRepository {
    private static final Logger logger = LogManager.getLogger(ResourceRepository.class);
    private static final String RECIPES_RESOURCE_PATH = "/recipes/";
    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Recipe> recipes = Collections.emptyMap();

    public ResourceRepository() {
        super("ResourceRepository", "jar://recipes");
    }

    @Override
    public void init() {
        logger.debug("Initializing ResourceRepository from JAR");
        try {
            Map<String, Recipe> loadedRecipes = loadRecipesFromResources();
            this.recipes = loadedRecipes;
            setInitialized();
            logger.info("ResourceRepository initialized with {} recipes", recipes.size());
        } catch (Exception e) {
            logger.warn("Failed to initialize ResourceRepository: {}", e.getMessage());
            setInitialized(); // Mark as initialized even if empty
        }
    }

    @Override
    public List<Recipe> listRecipes() {
        return List.copyOf(recipes.values());
    }

    @Override
    public Optional<Recipe> resolveRecipe(String recipeName) {
        return Optional.ofNullable(recipes.get(recipeName));
    }

    @Override
    public Optional<String> getRecipeYamlContent(String recipeName) {
        // For resource repository, we'd need to read from JAR resources
        // For now, return empty as this is mainly for built-in recipes
        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        // For resource repository, return empty for now
        return Optional.empty();
    }

    /**
     * Load recipes from JAR resources.
     * Looks for .levain, .levain.yaml, .levain.yml files in the resources
     * classpath.
     */
    private Map<String, Recipe> loadRecipesFromResources() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("recipes");
        Map<String, Recipe> loadedRecipes = Collections.emptyMap();

        while (resources.hasMoreElements()) {
            URL resourceURL = resources.nextElement();
            logger.debug("Found recipes resource: {}", resourceURL);

            try {
                List<URL> recipeFiles = listRecipeFilesInResource(resourceURL);
                loadedRecipes = recipeFiles.stream()
                        .map(this::loadRecipeFromResource)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toMap(Recipe::getName, r -> r));
            } catch (Exception e) {
                logger.warn("Error loading recipes from resource {}: {}", resourceURL, e.getMessage());
            }
        }

        return loadedRecipes;
    }

    /**
     * List recipe files in the resources directory.
     */
    private List<URL> listRecipeFilesInResource(URL resourceURL) {
        // For JAR resources, we need to handle this carefully
        // This is a simplified approach that works with standard JAR layouts
        ClassLoader classLoader = getClass().getClassLoader();
        List<URL> recipeFiles = Collections.emptyList();

        try {
            Enumeration<URL> allResources = classLoader.getResources("recipes");
            recipeFiles = Collections.list(allResources);
        } catch (IOException e) {
            logger.warn("Failed to list recipe files in resources: {}", e.getMessage());
        }

        return recipeFiles;
    }

    /**
     * Load a single recipe from a resource.
     */
    private Optional<Recipe> loadRecipeFromResource(URL resourceURL) {
        try {
            String filename = new java.io.File(resourceURL.getFile()).getName();
            if (!isRecipeFile(filename)) {
                return Optional.empty();
            }

            try (InputStream inputStream = resourceURL.openStream()) {
                Recipe recipe = objectMapper.readValue(inputStream, Recipe.class);
                String recipeName = extractRecipeName(filename);
                recipe.setName(recipeName);
                logger.debug("Loaded recipe from resource: {}", recipeName);
                return Optional.of(recipe);
            }
        } catch (Exception e) {
            logger.warn("Failed to load recipe from resource {}: {}", resourceURL, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if a file is a recipe file based on extension.
     */
    private boolean isRecipeFile(String filename) {
        return filename.matches(".*\\.levain(\\.ya?ml)?$");
    }

    /**
     * Extract recipe name from filename by removing .levain extensions.
     */
    private String extractRecipeName(String filename) {
        return filename.replaceAll("\\.levain(\\.ya?ml)?$", "");
    }
}
