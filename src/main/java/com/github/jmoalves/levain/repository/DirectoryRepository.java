package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeLoader;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from a file system directory.
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
}
