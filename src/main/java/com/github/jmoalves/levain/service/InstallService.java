package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for installing packages.
 */
@ApplicationScoped
public class InstallService {
    private static final Logger logger = LoggerFactory.getLogger(InstallService.class);

    @Inject
    private RecipeService recipeService;

    /**
     * Install a package by name.
     *
     * @param packageName Name of the package to install
     * @throws Exception if installation fails
     */
    public void install(String packageName) throws Exception {
        logger.info("Installing package: {}", packageName);

        // Load recipe
        Recipe recipe = recipeService.loadRecipe(packageName);

        // TODO: Implement actual installation logic
        // 1. Download package if needed
        // 2. Extract/install to appropriate location
        // 3. Execute install commands from recipe
        // 4. Update environment

        logger.info("Package {} installed successfully", packageName);
    }
}
