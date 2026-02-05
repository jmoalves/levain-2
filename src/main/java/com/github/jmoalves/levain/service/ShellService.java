package com.github.jmoalves.levain.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.action.ActionContext;
import com.github.jmoalves.levain.action.ActionExecutor;
import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.EnvironmentUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for managing shell environments.
 */
@ApplicationScoped
public class ShellService {
    private static final Logger logger = LoggerFactory.getLogger(ShellService.class);

    @Inject
    private Config config;

    @Inject
    private ActionExecutor actionExecutor;

    @Inject
    private RecipeService recipeService;

    /**
     * Open a shell with specified packages in the environment.
     * Executes cmd.shell and cmd.env actions from all package dependencies before launching shell.
     *
     * @param packages List of packages to include in shell environment
     * @throws IOException if shell cannot be opened
     */
    public void openShell(List<String> packages) throws IOException {
        logger.info("Opening shell with packages: {}", packages);

        // Load recipes for all requested packages
        List<Recipe> recipes = new ArrayList<>();
        if (packages != null && !packages.isEmpty()) {
            for (String pkgName : packages) {
                try {
                    Recipe recipe = recipeService.loadRecipe(pkgName);
                    if (recipe != null) {
                        recipes.add(recipe);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load recipe {}: {}", pkgName, e.getMessage());
                }
            }
        }

        // Execute cmd.shell and cmd.env actions from all recipes
        // Following original Levain pattern: cmd.shell first, then cmd.env
        for (Recipe recipe : recipes) {
            executeShellActions(recipe);
        }

        // Build environment with all recipe configurations
        Map<String, String> environment = buildEnvironment(recipes);

        // Determine OS and appropriate shell command
        List<String> command = buildShellCommand(packages);

        try {
            runProcess(command, environment);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Shell process interrupted", e);
        }
    }

    private void executeShellActions(Recipe recipe) {
        if (recipe == null || recipe.getCommands() == null) {
            return;
        }

        List<String> actions = new ArrayList<>();
        // Original Levain order: cmd.shell first, then cmd.env appended
        List<String> shellActions = recipe.getCommands().get("shell");
        List<String> envActions = recipe.getCommands().get("env");

        if (shellActions != null) {
            actions.addAll(shellActions);
        }
        if (envActions != null) {
            actions.addAll(envActions);
        }

        if (actions.isEmpty()) {
            return;
        }

        // Verify no levainShell recursion
        for (String action : actions) {
            if (action != null && isLevainShellAction(action)) {
                throw new IllegalStateException(
                        "levainShell action is not allowed in cmd.env or cmd.shell. Recipe: " + recipe.getName());
            }
        }

        Path baseDir = config.getLevainHome().resolve(recipe.getName());
        Path recipeDir = recipe.getRecipesDir() != null ? Path.of(recipe.getRecipesDir()) : baseDir;
        ActionContext ctx = new ActionContext(config, recipe, baseDir, recipeDir);
        actionExecutor.executeCommands(actions, ctx);
    }

    private boolean isLevainShellAction(String action) {
        String trimmed = action.trim();
        return trimmed.equals("levainShell") || trimmed.startsWith("levainShell ");
    }

    private Map<String, String> buildEnvironment(List<Recipe> recipes) {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.putAll(config.getVariables());
        env.put("levainHome", config.getLevainHome().toString());

        if (recipes != null && !recipes.isEmpty()) {
            String names = recipes.stream()
                    .map(Recipe::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.joining(";"));
            if (!names.isBlank()) {
                env.put("LEVAIN_PKG_NAMES", names);
            }
        }

        return env;
    }

    private List<String> buildShellCommand(List<String> packages) {
        List<String> command = new ArrayList<>();
        String pkgList = packages != null && !packages.isEmpty() 
            ? String.join(", ", packages) 
            : "default";

        if (EnvironmentUtils.isWindows()) {
            command.add("cmd.exe");
            command.add("/k");
            command.add("echo Levain shell initialized with packages: " + pkgList);
        } else {
            command.add("/bin/bash");
            command.add("-c");
            command.add("echo 'Levain shell initialized with packages: " + pkgList + "' && bash");
        }

        return command;
    }

    /**
     * Execute the shell process with configured environment. Extracted for testability.
     */
    protected void runProcess(List<String> command, Map<String, String> environment) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        if (environment != null && !environment.isEmpty()) {
            pb.environment().putAll(environment);
        }
        Process process = pb.start();
        process.waitFor();
    }
}
