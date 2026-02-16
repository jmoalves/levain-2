package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("Variable Substitution Service Tests")
class VariableSubstitutionServiceTest {
    private VariableSubstitutionService service;

    @Mock
    private Config config;

    @Mock
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        service = new VariableSubstitutionService();
        service.config = config;
        service.recipeService = recipeService;

        // Setup mock config defaults with lenient stubbings for optional behavior
        lenient().when(config.getLevainHome()).thenReturn(Paths.get("/opt/levain"));
        lenient().when(config.getCacheDir()).thenReturn(Paths.get("/opt/levain/cache"));
        lenient().when(config.getRegistryDir()).thenReturn(Paths.get("/opt/levain/registry"));
        lenient().when(config.getShellPath()).thenReturn(null);
        lenient().when(config.getVariables()).thenReturn(new HashMap<>());
    }

    @Test
    @DisplayName("Should substitute simple variable from map")
    void shouldSubstituteSimpleVariable() {
        Map<String, String> variables = new HashMap<>();
        variables.put("version", "1.0.0");
        variables.put("name", "test-package");

        String result = service.substitute("Version: ${version}", variables);
        assertEquals("Version: 1.0.0", result);
    }

    @Test
    @DisplayName("Should substitute multiple variables in text")
    void shouldSubstituteMultipleVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "test");
        variables.put("version", "1.0");

        String result = service.substitute("Package ${name} version ${version}", variables);
        assertEquals("Package test version 1.0", result);
    }

    @Test
    @DisplayName("Should handle missing variables gracefully")
    void shouldHandleMissingVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "test");

        String result = service.substitute("Name: ${name}, Version: ${version}", variables);
        assertEquals("Name: test, Version: ${version}", result);
    }

    @Test
    @DisplayName("Should handle null text")
    void shouldHandleNullText() {
        Map<String, String> variables = new HashMap<>();
        assertNull(service.substitute(null, variables));
    }

    @Test
    @DisplayName("Should handle null variables")
    void shouldHandleNullVariables() {
        String result = service.substitute("Text ${var}", (Map<String, String>) null);
        assertEquals("Text ${var}", result);
    }

    @Test
    @DisplayName("Should substitute recipe attributes")
    void shouldSubstituteRecipeAttributes() {
        Recipe recipe = new Recipe();
        recipe.setName("jdk-21");
        recipe.setVersion("21.0.1");
        recipe.setDescription("Java Development Kit");

        Path baseDir = Paths.get("/opt/levain/jdk-21");

        String result = service.substitute(
                "Installing ${name} version ${version}",
                recipe,
                baseDir);
        assertEquals("Installing jdk-21 version 21.0.1", result);
    }

    @Test
    @DisplayName("Should substitute baseDir variable")
    void shouldSubstituteBaseDir() {
        Recipe recipe = new Recipe();
        Path baseDir = Paths.get("/home/user/.levain/jdk-21");

        String result = service.substitute(
                "Base directory: ${baseDir}",
                recipe,
                baseDir);
        assertEquals("Base directory: /home/user/.levain/jdk-21", result);
    }

    @Test
    @DisplayName("Should substitute built-in variables")
    void shouldSubstituteBuiltInVariables() {
        Recipe recipe = new Recipe();
        Path baseDir = Paths.get("/opt/jdk-21");

        String text = "Levain home: ${levainHome}, Cache: ${cacheDir}, Registry: ${registryDir}";
        String result = service.substitute(text, recipe, baseDir);

        assertEquals(
                "Levain home: /opt/levain, Cache: /opt/levain/cache, Registry: /opt/levain/registry",
                result);
    }

    @Test
    @DisplayName("Should substitute custom config variables")
    void shouldSubstituteCustomVariables() {
        Map<String, String> customVars = new HashMap<>();
        customVars.put("JAVA_HOME", "/usr/lib/jvm/java-21");
        customVars.put("MAVEN_HOME", "/opt/maven");

        when(config.getVariables()).thenReturn(customVars);

        Recipe recipe = new Recipe();
        String result = service.substitute(
                "Java: ${JAVA_HOME}, Maven: ${MAVEN_HOME}",
                recipe,
                Paths.get("/tmp"));

        assertEquals("Java: /usr/lib/jvm/java-21, Maven: /opt/maven", result);
    }

    @Test
    @DisplayName("Should handle escaped dollar signs")
    void shouldHandleComplexPatterns() {
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "test");

        String result = service.substitute("Name is ${name}, Cost: $100", variables);
        assertEquals("Name is test, Cost: $100", result);
    }

    @Test
    @DisplayName("Should substitute indirect variable references with pkg. prefix")
    void shouldSubstituteIndirectVariables() {
        Recipe mainRecipe = new Recipe();
        mainRecipe.setName("maven");

        Recipe referencedRecipe = new Recipe();
        referencedRecipe.setName("jdk-21");
        referencedRecipe.setVersion("21.0.1");

        when(recipeService.loadRecipe("jdk-21")).thenReturn(referencedRecipe);

        String result = service.substitute(
                "JDK version: ${pkg.jdk-21.version}",
                mainRecipe,
                Paths.get("/tmp"));

        assertEquals("JDK version: 21.0.1", result);
    }

    @Test
    @DisplayName("Should return null for undefined indirect variable")
    void shouldHandleUndefinedIndirectVariable() {
        Recipe recipe = new Recipe();

        when(recipeService.loadRecipe("nonexistent")).thenThrow(new IllegalArgumentException("Recipe not found"));

        String result = service.substitute(
                "Version: ${pkg.nonexistent.version}",
                recipe,
                Paths.get("/tmp"));

        assertEquals("Version: ${pkg.nonexistent.version}", result);
    }

    @Test
    @DisplayName("Should NOT treat variables with dots as indirect references without pkg. prefix")
    void shouldNotTreatDotsAsIndirectReferencesWithoutPkgPrefix() {
        Recipe recipe = new Recipe();
        recipe.setName("test");

        // Variables with dots but without pkg. prefix should be treated as regular
        // variables from config
        Map<String, String> configVars = new HashMap<>();
        configVars.put("levain.email", "user@example.com");
        configVars.put("maven.password", "secret123");
        when(config.getVariables()).thenReturn(configVars);

        String result = service.substitute(
                "Email: ${levain.email}, Password: ${maven.password}",
                recipe,
                Paths.get("/tmp"));

        assertEquals("Email: user@example.com, Password: secret123", result);

        // Verify recipeService.loadRecipe was never called (not treated as indirect
        // reference)
        verify(recipeService, never()).loadRecipe(anyString());
    }

    @Test
    @DisplayName("Should handle empty recipe")
    void shouldHandleEmptyRecipe() {
        Recipe recipe = new Recipe();
        Path baseDir = Paths.get("/opt/package");

        String result = service.substitute("Path: ${baseDir}", recipe, baseDir);
        assertEquals("Path: /opt/package", result);
    }

    @Test
    @DisplayName("Should preserve original variables if not found")
    void shouldPreserveUnresolvedVariables() {
        Map<String, String> variables = new HashMap<>();

        String result = service.substitute("Path: ${unknownVar}", variables);
        assertEquals("Path: ${unknownVar}", result);
    }

    @Test
    @DisplayName("Should substitute variables in recipe commands")
    void shouldSubstituteVariablesInRecipeCommands() {
        Recipe recipe = new Recipe();
        recipe.setName("test-package");
        recipe.setVersion("1.0.0");

        Map<String, List<String>> commands = new HashMap<>();
        commands.put("install", Arrays.asList(
                "echo Installing ${name} v${version}",
                "mkdir -p ${baseDir}/bin",
                "cp file.txt ${baseDir}/"));
        recipe.setCommands(commands);

        Path baseDir = Paths.get("/opt/levain/install");
        service.substituteRecipeCommands(recipe, baseDir);

        List<String> installedCommands = recipe.getCommands().get("install");
        assertEquals("echo Installing test-package v1.0.0", installedCommands.get(0));
        assertEquals("mkdir -p /opt/levain/install/bin", installedCommands.get(1));
        assertEquals("cp file.txt /opt/levain/install/", installedCommands.get(2));
    }

    @Test
    @DisplayName("Should handle null commands in recipe")
    void shouldHandleNullCommandsInRecipe() {
        Recipe recipe = new Recipe();
        recipe.setName("test-package");
        recipe.setCommands(null);

        Path baseDir = Paths.get("/opt/levain/install");
        // Should not throw exception
        service.substituteRecipeCommands(recipe, baseDir);
    }

    @Test
    @DisplayName("Should resolve environment variables")
    void shouldResolveEnvironmentVariables() {
        // Set a test environment variable (this will use actual system env vars)
        String homeDir = System.getenv("HOME");
        if (homeDir == null) {
            homeDir = System.getenv("USERPROFILE"); // Windows fallback
        }

        if (homeDir != null) {
            Recipe recipe = new Recipe();
            Path baseDir = Paths.get("/opt/install");

            String envVarName = homeDir != null && System.getenv("HOME") != null ? "HOME" : "USERPROFILE";
            String result = service.substitute("Home is ${" + envVarName + "}", recipe, baseDir);
            assertEquals("Home is " + homeDir, result);
        }
    }

    @Test
    @DisplayName("Should support pkg. prefix in indirect variable references")
    void shouldSupportPkgPrefixInIndirectReferences() throws Exception {
        Recipe jdkRecipe = new Recipe();
        jdkRecipe.setName("jdk-21");
        jdkRecipe.setVersion("21.0.1");
        jdkRecipe.setRecipesDir("/opt/jdk-21");

        lenient().when(recipeService.loadRecipe("jdk-21")).thenReturn(jdkRecipe);

        Recipe recipe = new Recipe();
        recipe.setName("maven");
        Path baseDir = Paths.get("/opt/maven");

        String result = service.substitute("Using JDK ${pkg.jdk-21.version}", recipe, baseDir);
        assertEquals("Using JDK 21.0.1", result);
    }

    @Test
    @DisplayName("Should handle complex pkg. references with hyphens")
    void shouldHandleComplexPkgReferences() throws Exception {
        Recipe runtimeRecipe = new Recipe();
        runtimeRecipe.setName("wlp-runtime-25.0.0.12");
        runtimeRecipe.setRecipesDir("/opt/wlp");

        lenient().when(recipeService.loadRecipe("wlp-runtime-25.0.0.12")).thenReturn(runtimeRecipe);

        Recipe recipe = new Recipe();
        Path baseDir = Paths.get("/opt/install");

        String result = service.substitute("WLP Home: ${pkg.wlp-runtime-25.0.0.12.recipesDir}", recipe, baseDir);
        assertEquals("WLP Home: /opt/wlp", result);
    }

    @Test
    @DisplayName("Should prioritize local variables over environment variables")
    void shouldPrioritizeLocalVariablesOverEnv() {
        Map<String, String> variables = new HashMap<>();
        variables.put("PATH", "/custom/path");

        String result = service.substitute("Custom path: ${PATH}", variables);
        assertEquals("Custom path: /custom/path", result);
    }

    @Test
    @DisplayName("Should handle mixed variable types in single string")
    void shouldHandleMixedVariableTypes() throws Exception {
        Recipe jdkRecipe = new Recipe();
        jdkRecipe.setName("jdk-21");
        jdkRecipe.setVersion("21.0.1");

        lenient().when(recipeService.loadRecipe("jdk-21")).thenReturn(jdkRecipe);

        Recipe recipe = new Recipe();
        recipe.setName("maven");
        recipe.setVersion("3.9.0");
        Path baseDir = Paths.get("/opt/maven");

        String result = service.substitute(
                "Install ${name} ${version} with JDK ${pkg.jdk-21.version} to ${baseDir}",
                recipe,
                baseDir);
        assertEquals("Install maven 3.9.0 with JDK 21.0.1 to /opt/maven", result);
    }

    @Test
    @DisplayName("Should include shellPath when configured")
    void shouldIncludeShellPathWhenConfigured() {
        when(config.getShellPath()).thenReturn("/bin/zsh");

        Recipe recipe = new Recipe();
        String result = service.substitute("Shell is ${shellPath}", recipe, Paths.get("/tmp"));

        assertEquals("Shell is /bin/zsh", result);
    }

    @Test
    @DisplayName("Should resolve numeric and boolean custom attributes")
    void shouldResolveNumericAndBooleanCustomAttributes() {
        Recipe recipe = new Recipe();
        recipe.getCustomAttributes().put("flag", true);
        recipe.getCustomAttributes().put("count", 5);
        recipe.getCustomAttributes().put("cmd.skip", "ignored");

        String result = service.substitute("Flag=${flag} Count=${count}", recipe, Paths.get("/tmp"));

        assertEquals("Flag=true Count=5", result);
    }

    @Test
    @DisplayName("Should substitute custom attribute values using baseDir")
    void shouldSubstituteCustomAttributesUsingBaseDir() {
        Recipe recipe = new Recipe();
        recipe.getCustomAttributes().put("toolHome", "${baseDir}/tool");

        String result = service.substitute("Home=${toolHome}", recipe, Paths.get("/opt/pkg"));

        assertEquals("Home=/opt/pkg/tool", result);
    }

    @Test
    @DisplayName("Should keep invalid pkg reference without variable name")
    void shouldKeepInvalidPkgReference() {
        Recipe recipe = new Recipe();

        String result = service.substitute("Value=${pkg.onlypackage}", recipe, Paths.get("/tmp"));

        assertEquals("Value=${pkg.onlypackage}", result);
    }
}
