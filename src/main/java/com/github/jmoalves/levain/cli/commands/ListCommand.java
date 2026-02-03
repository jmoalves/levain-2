package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.RecipeService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to list available packages/recipes.
 * 
 * By default, shows all available recipes with an indicator for installed ones.
 * Use --installed to show only installed recipes.
 * Use --available to show only recipes that are not yet installed.
 */
@Command(name = "list", description = "List available packages/recipes", mixinStandardHelpOptions = true)
public class ListCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "0..1", description = "Optional filter pattern")
    private String filter;

    @Option(names = { "--installed" }, description = "Show only installed recipes")
    private boolean installedOnly;

    @Option(names = { "--available" }, description = "Show only recipes that are not installed")
    private boolean availableOnly;

    private final RecipeService recipeService;

    @Inject
    public ListCommand(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public Integer call() {
        // Validate mutually exclusive options
        if (installedOnly && availableOnly) {
            console.info("Error: --installed and --available are mutually exclusive");
            return 1;
        }

        logger.info("Listing recipes (filter: {}, installedOnly: {}, availableOnly: {})",
                filter, installedOnly, availableOnly);

        List<String> recipes = recipeService.listRecipes(filter);

        if (recipes.isEmpty()) {
            if (filter != null) {
                console.info("No recipes found matching '{}'", filter);
            } else {
                console.info("No recipes found");
            }
            return 0;
        }

        // Get installation status for each recipe
        List<RecipeStatus> recipeStatuses = recipes.stream()
                .map(name -> new RecipeStatus(name, recipeService.isInstalled(name)))
                .toList();

        // Apply filters based on options
        List<RecipeStatus> filteredRecipes = recipeStatuses;
        if (installedOnly) {
            filteredRecipes = recipeStatuses.stream()
                    .filter(RecipeStatus::isInstalled)
                    .toList();
        } else if (availableOnly) {
            filteredRecipes = recipeStatuses.stream()
                    .filter(status -> !status.isInstalled())
                    .toList();
        }

        // Display results
        if (filteredRecipes.isEmpty()) {
            if (installedOnly) {
                console.info("No installed recipes found" + (filter != null ? " matching '" + filter + "'" : ""));
            } else if (availableOnly) {
                console.info("No available (not installed) recipes found"
                        + (filter != null ? " matching '" + filter + "'" : ""));
            }
        } else {
            // Display header
            if (installedOnly) {
                console.info("Installed recipes:");
            } else if (availableOnly) {
                console.info("Available recipes (not installed):");
            } else {
                long installedCount = filteredRecipes.stream().filter(RecipeStatus::isInstalled).count();
                console.info("Available recipes ({} installed, {} total):", installedCount, filteredRecipes.size());
            }

            // Display recipes
            for (RecipeStatus status : filteredRecipes) {
                if (installedOnly || availableOnly) {
                    // No indicator needed when filtering by status
                    console.info("  - {}", status.name());
                } else {
                    // Show indicator when displaying all recipes
                    String indicator = status.isInstalled() ? "[installed]" : "";
                    console.info("  - {} {}", status.name(), indicator);
                }
            }
        }

        return 0;
    }

    /**
     * Record representing a recipe with its installation status.
     */
    private record RecipeStatus(String name, boolean isInstalled) {
    }
}
