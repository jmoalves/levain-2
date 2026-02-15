package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeService;
import com.github.jmoalves.levain.util.EnvironmentUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Execute OS commands with the Levain environment applied.
 *
 * Usage:
 *   - levainShell echo "Hello"
 *   - levainShell --saveVar out --stripCRLF cmd /c "echo hi"
 *   - levainShell --ignoreErrors ls /nonexistent
 *
 * Notes:
 * - This action is not allowed inside cmd.env or cmd.shell blocks.
 * - It assembles environment variables from config (setEnv/addPath actions).
 */
@ApplicationScoped
public class LevainShellAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(LevainShellAction.class);

    private final ActionExecutor actionExecutor;
    private final RecipeService recipeService;
    private final Config config;

    @Inject
    public LevainShellAction(ActionExecutor actionExecutor, RecipeService recipeService, Config config) {
        this.actionExecutor = actionExecutor;
        this.recipeService = recipeService;
        this.config = config;
    }

    @Override
    public String name() {
        return "levainShell";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null || context.getRecipe() == null) {
            throw new IllegalArgumentException("levainShell requires a recipe context");
        }

        ParsedArgs parsed = parseArgs(args);
        if (parsed.commandArgs.isEmpty()) {
            throw new IllegalArgumentException("levainShell requires a command to execute");
        }

        List<Recipe> shellRecipes = resolveShellRecipes(context.getRecipe());
        for (Recipe recipe : shellRecipes) {
            runShellActions(recipe);
        }

        Map<String, String> env = buildEnvironment(shellRecipes);
        List<String> command = buildCommand(parsed.commandArgs);

        logger.debug("levainShell command: {}", command);
        ProcessResult result = runCommand(command, env, parsed.saveVar != null);

        if (!parsed.ignoreErrors && result.exitCode != 0) {
            throw new RuntimeException("levainShell command failed with code " + result.exitCode);
        }

        if (parsed.saveVar != null) {
            String output = result.output != null ? result.output : "";
            if (parsed.stripCRLF) {
                output = stripLineEnding(output);
            }
            context.setRecipeVariable(parsed.saveVar, output);
        }
    }

    private List<Recipe> resolveShellRecipes(Recipe recipe) {
        if (recipe == null || recipe.getName() == null || recipeService == null) {
            return List.of(recipe);
        }
        try {
            return recipeService.resolveRecipe(recipe.getName());
        } catch (Exception e) {
            logger.debug("Failed to resolve dependencies for levainShell: {}", e.getMessage());
            return List.of(recipe);
        }
    }

    private void runShellActions(Recipe recipe) {
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

    private List<String> buildCommand(List<String> args) {
        if (EnvironmentUtils.isWindows()) {
            List<String> command = new ArrayList<>();
            command.add("cmd.exe");
            command.add("/u");
            command.add("/c");
            command.addAll(args);
            return command;
        }

        String shell = resolveUnixShell();
        String commandString = toShellCommand(args);
        List<String> command = new ArrayList<>();
        command.add(shell);
        command.add("-lc");
        command.add(commandString);
        return command;
    }

    private String resolveUnixShell() {
        String configured = config.getShellPath();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        Path bash = Path.of("/bin/bash");
        if (Files.exists(bash)) {
            return bash.toString();
        }
        return "/bin/sh";
    }

    private String toShellCommand(List<String> args) {
        return args.stream().map(this::quoteForShell).collect(Collectors.joining(" "));
    }

    private String quoteForShell(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "''";
        }
        boolean needsQuote = arg.chars().anyMatch(ch -> Character.isWhitespace(ch) || ch == '"' || ch == '\'' || ch == '\\');
        if (!needsQuote) {
            return arg;
        }
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    private String stripLineEnding(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\r\\n$", "")
                .replaceAll("\\r$", "")
                .replaceAll("\\n$", "");
    }

    protected ProcessResult runCommand(List<String> command, Map<String, String> env, boolean captureOutput)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(env);

        if (!captureOutput) {
            builder.inheritIO();
        } else {
            builder.redirectErrorStream(true);
        }

        Process process = builder.start();
        String output = null;
        if (captureOutput) {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, output);
    }

    private ParsedArgs parseArgs(List<String> args) {
        ParsedArgs parsed = new ParsedArgs();
        if (args == null || args.isEmpty()) {
            return parsed;
        }

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--saveVar".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--saveVar requires a variable name");
                }
                parsed.saveVar = args.get(++i);
                continue;
            }
            if (arg.startsWith("--saveVar=")) {
                String value = arg.substring("--saveVar=".length());
                if (value.isBlank()) {
                    throw new IllegalArgumentException("--saveVar requires a variable name");
                }
                parsed.saveVar = value;
                continue;
            }
            if ("--stripCRLF".equals(arg)) {
                parsed.stripCRLF = true;
                continue;
            }
            if ("--ignoreErrors".equals(arg)) {
                parsed.ignoreErrors = true;
                continue;
            }
            parsed.commandArgs.add(arg);
        }

        return parsed;
    }

    protected static class ProcessResult {
        final int exitCode;
        final String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private static class ParsedArgs {
        String saveVar;
        boolean stripCRLF;
        boolean ignoreErrors;
        List<String> commandArgs = new ArrayList<>();
    }
}
