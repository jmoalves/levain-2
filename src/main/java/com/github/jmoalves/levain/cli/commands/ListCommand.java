package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.RecipeService;
import com.github.jmoalves.levain.repository.RecipeMetadata;

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

    @Option(names = { "--source" }, description = "Show the source repository for each recipe")
    private boolean showSource;

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
                .map(this::buildRecipeStatus)
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
                if (filter != null) {
                    console.info("No installed recipes found matching '{}'", filter);
                } else {
                    console.info("No installed recipes found");
                }
            } else if (availableOnly) {
                if (filter != null) {
                    console.info("No available (not installed) recipes found matching '{}'", filter);
                } else {
                    console.info("No available (not installed) recipes found");
                }
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
                String indicator = (!installedOnly && !availableOnly && status.isInstalled()) ? "[installed]" : "";
                String sourceInfo = showSource ? formatSource(status) : "";
                String suffix = buildSuffix(indicator, sourceInfo);
                console.info("  - {}{}", status.name(), suffix);
            }
        }

        return 0;
    }

    private RecipeStatus buildRecipeStatus(String recipeName) {
        boolean installed = recipeService.isInstalled(recipeName);

        String sourceName = null;
        String sourceUri = null;
        if (showSource) {
            if (installed) {
                RecipeMetadata metadata = recipeService.getInstalledMetadata(recipeName).orElse(null);
                if (metadata != null) {
                    sourceName = metadata.getSourceRepository();
                    sourceUri = metadata.getSourceRepositoryUri();
                }
            } else {
                var repo = recipeService.findSourceRepository(recipeName).orElse(null);
                if (repo != null) {
                    sourceName = repo.getName();
                    sourceUri = repo.getUri();
                }
            }

            if (sourceName == null && sourceUri == null) {
                sourceName = "unknown";
            }
        }

        return new RecipeStatus(recipeName, installed, sourceName, sourceUri);
    }

    private String formatSource(RecipeStatus status) {
        if (status.sourceName() == null && status.sourceUri() == null) {
            return "";
        }

        if (status.sourceUri() == null || status.sourceUri().isBlank()) {
            return "(source: " + status.sourceName() + ")";
        }

        String sourceName = status.sourceName() != null ? status.sourceName() : "unknown";
        return "(source: " + sourceName + " | " + status.sourceUri() + ")";
    }

    private String buildSuffix(String indicator, String sourceInfo) {
        StringBuilder sb = new StringBuilder();
        if (indicator != null && !indicator.isBlank()) {
            sb.append(" ").append(indicator);
        }
        if (sourceInfo != null && !sourceInfo.isBlank()) {
            sb.append(" ").append(sourceInfo);
        }
        return sb.toString();
    }

    /**
     * Record representing a recipe with its installation status.
     */
    private record RecipeStatus(String name, boolean isInstalled, String sourceName, String sourceUri) {
    }
}
