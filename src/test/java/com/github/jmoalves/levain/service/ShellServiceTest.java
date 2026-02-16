package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jmoalves.levain.action.ActionContext;
import com.github.jmoalves.levain.action.ActionExecutor;
import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;

import jakarta.enterprise.inject.Vetoed;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayName("ShellService Tests")
class ShellServiceTest {

    @Vetoed
    private static class TestShellService extends ShellService {
        private List<String> capturedCommand;
        private Map<String, String> capturedEnv;

        TestShellService(Config config, ActionExecutor actionExecutor, RecipeService recipeService) {
            try {
                Field configField = ShellService.class.getDeclaredField("config");
                configField.setAccessible(true);
                configField.set(this, config);

                Field executorField = ShellService.class.getDeclaredField("actionExecutor");
                executorField.setAccessible(true);
                executorField.set(this, actionExecutor);

                Field recipeField = ShellService.class.getDeclaredField("recipeService");
                recipeField.setAccessible(true);
                recipeField.set(this, recipeService);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject mocks", e);
            }
        }

        @Override
        protected void runProcess(List<String> command, Map<String, String> environment, Path workingDir) {
            this.capturedCommand = command;
            this.capturedEnv = environment;
        }
    }

    @Test
    @DisplayName("Should execute shell and env actions in order")
    void testOpenShellExecutesActions() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);
        RecipeService recipeService = Mockito.mock(RecipeService.class);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        Map<String, List<String>> commands = new HashMap<>();
        commands.put("shell", List.of("setVar foo bar"));
        commands.put("env", List.of("setEnv HELLO world"));
        recipe.setCommands(commands);

        when(recipeService.loadRecipe("tooling")).thenReturn(recipe);

        TestShellService service = new TestShellService(config, actionExecutor, recipeService);
        service.openShell(List.of("tooling"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> actionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(actionExecutor).executeCommands(actionsCaptor.capture(), any(ActionContext.class));
        assertEquals(List.of("setVar foo bar", "setEnv HELLO world"), actionsCaptor.getValue());
    }

    @Test
    @DisplayName("Should build environment with package names")
    void testOpenShellBuildsEnvironment() throws Exception {
        Config config = Mockito.mock(Config.class);
        Map<String, String> vars = new HashMap<>();
        vars.put("X", "1");
        when(config.getVariables()).thenReturn(vars);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);
        RecipeService recipeService = Mockito.mock(RecipeService.class);

        Recipe recipeA = new Recipe();
        recipeA.setName("pkg-a");
        Recipe recipeB = new Recipe();
        recipeB.setName("pkg-b");

        when(recipeService.loadRecipe("pkg-a")).thenReturn(recipeA);
        when(recipeService.loadRecipe("pkg-b")).thenReturn(recipeB);

        TestShellService service = new TestShellService(config, actionExecutor, recipeService);
        service.openShell(List.of("pkg-a", "pkg-b"));

        assertNotNull(service.capturedEnv);
        assertEquals("/tmp/levain", service.capturedEnv.get("levainHome"));
        assertEquals("1", service.capturedEnv.get("X"));
        assertEquals("pkg-a;pkg-b", service.capturedEnv.get("LEVAIN_PKG_NAMES"));
    }

    @Test
    @DisplayName("Should reject levainShell recursion")
    void testRejectLevainShellAction() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);
        RecipeService recipeService = Mockito.mock(RecipeService.class);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        recipe.setCommands(Map.of("env", List.of("levainShell echo nope")));

        when(recipeService.loadRecipe("tooling")).thenReturn(recipe);

        TestShellService service = new TestShellService(config, actionExecutor, recipeService);

        assertThrows(IllegalStateException.class, () -> service.openShell(List.of("tooling")));
        verify(actionExecutor, never()).executeCommands(any(), any(ActionContext.class));
    }

    @Test
    @DisplayName("Should ignore recipe load failures")
    void testRecipeLoadFailure() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);
        RecipeService recipeService = Mockito.mock(RecipeService.class);
        when(recipeService.loadRecipe("bad")).thenThrow(new RuntimeException("boom"));

        TestShellService service = new TestShellService(config, actionExecutor, recipeService);
        service.openShell(List.of("bad"));

        assertNotNull(service.capturedCommand);
    }

    @Test
    @DisplayName("Should build environment with valid package names only")
    void testBuildEnvironmentFiltersPackageNames() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getVariables()).thenReturn(Map.of("X", "1"));
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ShellService service = new ShellService();
        setField(service, "config", config);

        Recipe recipeA = new Recipe();
        recipeA.setName("pkg-a");
        Recipe recipeBlank = new Recipe();
        recipeBlank.setName(" ");

        var method = ShellService.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(service, List.of(recipeA, recipeBlank));

        assertEquals("/tmp/levain", env.get("levainHome"));
        assertEquals("1", env.get("X"));
        assertEquals("pkg-a", env.get("LEVAIN_PKG_NAMES"));
    }

    @Test
    @DisplayName("Should omit package names when no recipes provided")
    void testBuildEnvironmentWithoutRecipes() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getVariables()).thenReturn(Map.of());
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ShellService service = new ShellService();
        setField(service, "config", config);

        var method = ShellService.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(service, (Object) null);

        assertEquals("/tmp/levain", env.get("levainHome"));
        assertTrue(!env.containsKey("LEVAIN_PKG_NAMES"));
    }

    @Test
    @DisplayName("Should use default package list in shell command")
    void testBuildShellCommandDefaultsToDefaultPackage() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("buildShellCommand", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(service, (Object) null);

        assertEquals("/bin/bash", command.get(0));
        assertTrue(command.get(2).contains("default"));
    }

    @Test
    @DisplayName("Should build shell command with package list")
    void testBuildShellCommandWithPackages() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("buildShellCommand", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(service, List.of("jdk-21", "maven"));

        assertTrue(command.get(2).contains("jdk-21, maven"));
    }

    @Test
    @DisplayName("Should detect levainShell actions")
    void testIsLevainShellActionRecognizesPrefix() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("isLevainShellAction", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(service, "levainShell"));
        assertTrue((Boolean) method.invoke(service, "levainShell echo hi"));
        assertTrue((Boolean) method.invoke(service, "  levainShell echo hi"));
    }

    @Test
    @DisplayName("Should skip actions when recipe commands are missing")
    void testExecuteShellActionsSkipsMissingCommands() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);

        ShellService service = new ShellService();
        setField(service, "config", config);
        setField(service, "actionExecutor", actionExecutor);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        recipe.setCommands(null);

        var method = ShellService.class.getDeclaredMethod("executeShellActions", Recipe.class);
        method.setAccessible(true);
        method.invoke(service, recipe);

        verify(actionExecutor, never()).executeCommands(any(), any(ActionContext.class));
    }

    @Test
    @DisplayName("Should run process with empty env")
    void testRunProcessWithEmptyEnvironment() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("runProcess", List.class, Map.class, Path.class);
        method.setAccessible(true);
        method.invoke(service, List.of("/bin/true"), Map.of(), null);
    }

    @Test
    @DisplayName("Should handle recipe load failure gracefully")
    void testOpenShellHandlesRecipeLoadFailure() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);
        RecipeService recipeService = Mockito.mock(RecipeService.class);
        when(recipeService.loadRecipe("bad-package")).thenThrow(new RuntimeException("Recipe not found"));

        TestShellService service = new TestShellService(config, actionExecutor, recipeService);
        
        // Should not throw - it logs warning and continues
        service.openShell(List.of("bad-package"));
        
        assertNotNull(service.capturedCommand);
    }

    @Test
    @DisplayName("Should build environment with null recipes")
    void testBuildEnvironmentWithNullRecipes() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ShellService service = new ShellService();
        setField(service, "config", config);

        var method = ShellService.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(service, (Object) null);

        assertNotNull(env);
        assertEquals("/tmp/levain", env.get("levainHome"));
    }

    @Test
    @DisplayName("Should build environment with empty recipe list")
    void testBuildEnvironmentWithEmptyRecipeList() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ShellService service = new ShellService();
        setField(service, "config", config);

        var method = ShellService.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(service, List.of());

        assertNotNull(env);
        assertEquals("/tmp/levain", env.get("levainHome"));
    }

    @Test
    @DisplayName("Should filter null and blank recipe names in environment")
    void testBuildEnvironmentFiltersNullAndBlankNames() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));
        when(config.getVariables()).thenReturn(new HashMap<>());

        ShellService service = new ShellService();
        setField(service, "config", config);

        Recipe r1 = new Recipe();
        r1.setName("valid-pkg");
        Recipe r2 = new Recipe();
        r2.setName(null);
        Recipe r3 = new Recipe();
        r3.setName("  ");
        Recipe r4 = new Recipe();
        r4.setName("another-pkg");

        var method = ShellService.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(service, List.of(r1, r2, r3, r4));

        assertEquals("valid-pkg;another-pkg", env.get("LEVAIN_PKG_NAMES"));
    }

    @Test
    @DisplayName("Should build shell command with null packages")
    void testBuildShellCommandWithNullPackages() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("buildShellCommand", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(service, (Object) null);

        assertTrue(command.get(2).contains("default"));
    }

    @Test
    @DisplayName("Should build shell command with empty packages")
    void testBuildShellCommandWithEmptyPackages() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("buildShellCommand", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(service, List.of());

        assertTrue(command.get(2).contains("default"));
    }

    @Test
    @DisplayName("Should recognize levainShell action without arguments")
    void testIsLevainShellActionWithoutArgs() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("isLevainShellAction", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(service, "levainShell"));
    }

    @Test
    @DisplayName("Should execute shell actions when cmd.shell exists")
    void testExecuteShellActionsWithShellCommands() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);

        ShellService service = new ShellService();
        setField(service, "config", config);
        setField(service, "actionExecutor", actionExecutor);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        Map<String, List<String>> commands = new HashMap<>();
        commands.put("shell", List.of("setVar foo bar"));
        recipe.setCommands(commands);

        var method = ShellService.class.getDeclaredMethod("executeShellActions", Recipe.class);
        method.setAccessible(true);
        method.invoke(service, recipe);

        verify(actionExecutor).executeCommands(eq(List.of("setVar foo bar")), any(ActionContext.class));
    }

    @Test
    @DisplayName("Should execute env actions when cmd.env exists")
    void testExecuteShellActionsWithEnvCommands() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);

        ShellService service = new ShellService();
        setField(service, "config", config);
        setField(service, "actionExecutor", actionExecutor);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        Map<String, List<String>> commands = new HashMap<>();
        commands.put("env", List.of("setEnv PATH /opt/bin"));
        recipe.setCommands(commands);

        var method = ShellService.class.getDeclaredMethod("executeShellActions", Recipe.class);
        method.setAccessible(true);
        method.invoke(service, recipe);

        verify(actionExecutor).executeCommands(eq(List.of("setEnv PATH /opt/bin")), any(ActionContext.class));
    }

    @Test
    @DisplayName("Should reject levainShell actions in shell commands")
    void testExecuteShellActionsRejectsLevainShell() throws Exception {
        Config config = Mockito.mock(Config.class);
        when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        ActionExecutor actionExecutor = Mockito.mock(ActionExecutor.class);

        ShellService service = new ShellService();
        setField(service, "config", config);
        setField(service, "actionExecutor", actionExecutor);

        Recipe recipe = new Recipe();
        recipe.setName("tooling");
        Map<String, List<String>> commands = new HashMap<>();
        commands.put("shell", List.of("setVar foo bar", "levainShell echo test", "setVar baz qux"));
        recipe.setCommands(commands);

        var method = ShellService.class.getDeclaredMethod("executeShellActions", Recipe.class);
        method.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> method.invoke(service, recipe));
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertTrue(exception.getCause().getMessage().contains("levainShell action is not allowed"));
    }

    @Test
    @DisplayName("Should run process with null working directory")
    void testRunProcessWithNullWorkingDir() throws Exception {
        ShellService service = new ShellService();

        var method = ShellService.class.getDeclaredMethod("runProcess", List.class, Map.class, Path.class);
        method.setAccessible(true);
        method.invoke(service, List.of("/bin/true"), new HashMap<>(), null);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = ShellService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
