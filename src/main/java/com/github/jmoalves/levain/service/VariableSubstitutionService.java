package com.github.jmoalves.levain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for handling variable substitution in recipes.
 * 
 * Variables can be referenced using the syntax ${variableName}.
 * 
 * Supported variable sources:
 * 1. Recipe attributes (all single-valued attributes become variables)
 * 2. Built-in variables (baseDir, levainHome, cacheDir, registryDir, etc.)
 * 3. Custom variables from config
 * 4. Indirect variables from other recipes (pkg.package.variable - pkg. prefix
 * required)
 * 5. Environment variables (system environment variables)
 * 6. Recipe custom attributes (any additional YAML properties)
 */
@ApplicationScoped
public class VariableSubstitutionService {
    private static final Logger logger = LogManager.getLogger(VariableSubstitutionService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Inject
    Config config;

    @Inject
    RecipeService recipeService;

    /**
     * Substitute variables in a string using the provided variable context.
     * 
     * @param text    The text containing variable references
     * @param recipe  The recipe providing context (for baseDir and recipe
     *                variables)
     * @param baseDir The base directory for the recipe (used for ${baseDir})
     * @return The text with variables substituted
     */
    public String substitute(String text, Recipe recipe, Path baseDir) {
        if (text == null) {
            return null;
        }

        // Build variable context
        Map<String, String> variables = buildVariableContext(recipe, baseDir);

        // Perform substitution
        return substitute(text, variables);
    }

    /**
     * Substitute variables using an action execution context.
     * This allows recipe-scoped variables (setVar/checkChainDirExists) to take effect
     * for subsequent commands.
     *
     * @param text    The text containing variable references
     * @param context The action execution context
     * @return The text with variables substituted
     */
    public String substitute(String text, com.github.jmoalves.levain.action.ActionContext context) {
        if (text == null || context == null) {
            return text;
        }

        Map<String, String> variables = buildVariableContext(context);
        return substitute(text, variables);
    }

    /**
     * Substitute variables in a string using the provided variable map.
     * 
     * @param text      The text containing variable references
     * @param variables Map of variable names to values
     * @return The text with variables substituted
     */
    public String substitute(String text, Map<String, String> variables) {
        if (text == null || variables == null) {
            return text;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(text);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = resolveVariable(variableName, variables);

            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
                logger.debug("Substituted ${} with '{}'", variableName, value);
            } else {
                // Keep original if variable not found
                logger.warn("Variable not found: {}", variableName);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Build a variable context from recipe attributes and built-in variables.
     * 
     * @param recipe  The recipe providing context
     * @param baseDir The base directory for the recipe
     * @return Map of variable names to values
     */
    private Map<String, String> buildVariableContext(Recipe recipe, Path baseDir) {
        Map<String, String> variables = new HashMap<>();

        // Add built-in variables
        variables.put("baseDir", baseDir != null ? baseDir.toString() : "");
        variables.put("levainHome", config.getLevainHome().toString());
        variables.put("cacheDir", config.getCacheDir().toString());
        variables.put("registryDir", config.getRegistryDir().toString());

        // Add shell path if configured
        String shellPath = config.getShellPath();
        if (shellPath != null) {
            variables.put("shellPath", shellPath);
        }

        // Add custom config variables
        variables.putAll(config.getVariables());

        // Add recipe attributes as variables (single-valued attributes)
        if (recipe != null) {
            if (recipe.getName() != null) {
                variables.put("name", recipe.getName());
            }
            if (recipe.getVersion() != null) {
                variables.put("version", recipe.getVersion());
            }
            if (recipe.getDescription() != null) {
                variables.put("description", recipe.getDescription());
            }
            if (recipe.getRecipesDir() != null) {
                variables.put("recipesDir", recipe.getRecipesDir());
            }
            if (recipe.getCustomAttributes() != null) {
                for (Map.Entry<String, Object> entry : recipe.getCustomAttributes().entrySet()) {
                    String key = entry.getKey();
                    if (key == null || key.startsWith("cmd.")) {
                        continue;
                    }
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        // Substitute variables in custom attribute values (e.g., gitHome: ${baseDir})
                        String stringValue = (String) value;
                        String substitutedValue = substitute(stringValue, variables);
                        variables.put(key, substitutedValue);
                    } else if (value instanceof Number || value instanceof Boolean) {
                        variables.put(key, value.toString());
                    }
                }
            }
        }

        return variables;
    }

    private Map<String, String> buildVariableContext(com.github.jmoalves.levain.action.ActionContext context) {
        Recipe recipe = context.getRecipe();
        Path baseDir = context.getBaseDir();
        Map<String, String> variables = buildVariableContext(recipe, baseDir);

        Map<String, String> recipeVariables = context.getRecipeVariables();
        if (recipeVariables != null && !recipeVariables.isEmpty()) {
            variables.putAll(recipeVariables);
        }

        return variables;
    }

    /**
     * Resolve a variable reference, including indirect references
     * (package.variable or pkg.package.variable) and environment variables.
     * 
     * @param variableName The variable name, potentially with package prefix
     * @param variables    The local variable context
     * @return The variable value, or null if not found
     */
    private String resolveVariable(String variableName, Map<String, String> variables) {
        // Check local variables first (includes config and recipe attributes)
        String localValue = variables.get(variableName);
        if (localValue != null) {
            return localValue;
        }

        // Check for indirect variable reference (pkg.packageName.variableName or
        // packageName.variableName)
        if (variableName.contains(".")) {
            String indirectValue = resolveIndirectVariable(variableName);
            if (indirectValue != null) {
                return indirectValue;
            }
        }

        // Check environment variables as fallback
        String envValue = System.getenv(variableName);
        if (envValue != null) {
            logger.debug("Resolved {} from environment variable", variableName);
            return envValue;
        }

        return null;
    }

    /**
     * Resolve an indirect variable reference from another recipe.
     * Only supports the explicit pkg. prefix format:
     * - pkg.packageName.variableName (e.g., pkg.wlp-runtime-25.0.0.12.wlpHome)
     * 
     * Package names can contain dots. We use lastIndexOf to find the variable name
     * in the last segment after the final dot.
     * 
     * Variables with dots but without pkg. prefix (e.g., levain.email,
     * maven.password)
     * are NOT treated as indirect references - they're regular variables.
     * 
     * @param indirectRef The indirect variable reference
     * @return The variable value, or null if not an indirect reference or not found
     */
    private String resolveIndirectVariable(String indirectRef) {
        // Only support pkg. prefix format for indirect references
        if (!indirectRef.startsWith("pkg.")) {
            // Not an indirect reference - variables with dots but without pkg. prefix
            // are treated as regular variables (e.g., levain.email, maven.password)
            return null;
        }

        String remaining = indirectRef.substring(4); // Remove "pkg." prefix

        // Find the last dot to separate package name from variable name
        // This handles packages like: pkg.wlp-runtime-25.0.0.12.recipesDir
        // Package: wlp-runtime-25.0.0.12
        // Variable: recipesDir
        int lastDot = remaining.lastIndexOf('.');
        if (lastDot == -1) {
            logger.warn("Invalid pkg reference format: {}", indirectRef);
            return null;
        }

        String packageName = remaining.substring(0, lastDot);
        String variableName = remaining.substring(lastDot + 1);

        try {
            // Try to find and load the recipe
            Recipe refRecipe = recipeService.loadRecipe(packageName);
            Map<String, String> refVariables = new HashMap<>();

            // Build variable context for referenced recipe
            if (refRecipe.getName() != null) {
                refVariables.put("name", refRecipe.getName());
            }
            if (refRecipe.getVersion() != null) {
                refVariables.put("version", refRecipe.getVersion());
            }
            if (refRecipe.getDescription() != null) {
                refVariables.put("description", refRecipe.getDescription());
            }
            if (refRecipe.getRecipesDir() != null) {
                refVariables.put("recipesDir", refRecipe.getRecipesDir());
            }

            String value = refVariables.get(variableName);
            if (value != null) {
                logger.debug("Resolved indirect variable {}.{}", packageName, variableName);
            }
            return value;
        } catch (Exception e) {
            logger.warn("Failed to resolve indirect variable {}: {}", indirectRef, e.getMessage());
            return null;
        }
    }

    /**
     * Apply variable substitution to all commands in a recipe.
     * This substitutes variables in all command strings within the recipe.
     * 
     * @param recipe  The recipe to process
     * @param baseDir The base directory for the recipe
     */
    public void substituteRecipeCommands(Recipe recipe, Path baseDir) {
        if (recipe == null || recipe.getCommands() == null) {
            return;
        }

        Map<String, String> variables = buildVariableContext(recipe, baseDir);

        // Iterate through each command category and its command list
        for (Map.Entry<String, List<String>> entry : recipe.getCommands().entrySet()) {
            List<String> commands = entry.getValue();
            if (commands != null) {
                for (int i = 0; i < commands.size(); i++) {
                    String originalCommand = commands.get(i);
                    String substitutedCommand = substitute(originalCommand, variables);
                    if (!originalCommand.equals(substitutedCommand)) {
                        logger.debug("Substituted command in {}: {} -> {}", entry.getKey(), originalCommand,
                                substitutedCommand);
                        commands.set(i, substitutedCommand);
                    }
                }
            }
        }

        logger.debug("Variable substitution completed for recipe: {}", recipe.getName());
    }
}
