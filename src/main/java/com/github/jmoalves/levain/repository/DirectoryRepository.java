package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeLoader;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from a file system directory.
 * Only accepts recipes with .levain.yaml extension.
 */
@Dependent
public class DirectoryRepository extends AbstractRepository {
    private static final Logger logger = LogManager.getLogger(DirectoryRepository.class);
    private final String directoryPath;
    private final RecipeLoader recipeLoader;
    private Map<String, Recipe> recipes = Collections.emptyMap();

    public DirectoryRepository(String name, String directoryPath) {
        super(name, directoryPath);
        this.directoryPath = directoryPath;
        this.recipeLoader = new RecipeLoader();
    }

    @Override
    public void init() {
        logger.debug("Initializing DirectoryRepository: {}", directoryPath);
        try {
            this.recipes = recipeLoader.loadRecipesFromDirectory(directoryPath);
            setInitialized();
            logger.info("DirectoryRepository initialized with {} recipes from {}", recipes.size(), directoryPath);
        } catch (Exception e) {
            logger.error("Failed to initialize DirectoryRepository from {}: {}", directoryPath, e.getMessage(), e);
            setInitialized(); // Mark as initialized even if empty, to prevent retries
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
        if (!recipes.containsKey(recipeName)) {
            return Optional.empty();
        }

        // All recipe files use .levain.yaml extension
        Path recipePath = Paths.get(directoryPath, recipeName + ".levain.yaml");
        if (Files.exists(recipePath)) {
            try {
                return Optional.of(Files.readString(recipePath));
            } catch (IOException e) {
                logger.error("Failed to read recipe file {}: {}", recipePath, e.getMessage());
            }
        }

        // Fallback: search subdirectories for .levain.yaml files
        try {
            Path result = Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        return isValidLevainFilename(fileName);
                    })
                    .filter(p -> {
                        String baseName = p.getFileName().toString().replace(".levain.yaml", "");
                        return baseName.equals(recipeName);
                    })
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                return Optional.of(Files.readString(result));
            }
        } catch (IOException e) {
            logger.error("Failed to search for recipe file {}: {}", recipeName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        if (!recipes.containsKey(recipeName)) {
            return Optional.empty();
        }

        // All recipes use standardized .levain.yaml extension
        // Validate the recipe name doesn't contain .levain.yaml
        if (recipeName.contains(".levain.yaml")) {
            logger.warn("Recipe name contains .levain.yaml: {}", recipeName);
            return Optional.empty();
        }

        return Optional.of(recipeName + ".levain.yaml");
    }

    /**
     * Validate that a levain filename is properly formatted.
     * Must have exactly one occurrence of .levain.yaml extension.
     */
    private boolean isValidLevainFilename(String fileName) {
        if (!fileName.endsWith(".levain.yaml")) {
            return false;
        }
        // Count occurrences of .levain.yaml
        int count = fileName.split("\\.levain\\.yaml", -1).length - 1;
        if (count != 1) {
            logger.warn("Invalid levain filename (multiple .levain.yaml): {}", fileName);
            return false;
        }
        return true;
    }
}
