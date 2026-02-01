package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to list available packages/recipes.
 */
@Command(name = "list", description = "List available packages/recipes", mixinStandardHelpOptions = true)
public class ListCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "0..1", description = "Optional filter pattern")
    private String filter;

    private final RecipeService recipeService;

    public ListCommand() {
        this.recipeService = new RecipeService();
    }

    @Override
    public Integer call() {
        logger.info("Listing available recipes...");

        List<String> recipes = recipeService.listRecipes(filter);

        if (recipes.isEmpty()) {
            console.info("No recipes found" + (filter != null ? " matching '" + filter + "'" : ""));
        } else {
            console.info("Available recipes:");
            for (String recipe : recipes) {
                console.info("  - " + recipe);
            }
        }

        return 0;
    }
}
