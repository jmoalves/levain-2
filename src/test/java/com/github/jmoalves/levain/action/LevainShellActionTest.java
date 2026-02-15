package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

class LevainShellActionTest {

    private String originalOsName;

    @TempDir
    Path tempDir;

    @Mock
    ActionExecutor actionExecutor;

    @Mock
    RecipeService recipeService;

    private Config config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new Config();
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void tearDown() {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        } else {
            System.clearProperty("os.name");
        }
    }

    @Test
    void shouldRejectMissingContext() {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        assertThrows(IllegalArgumentException.class, () -> action.execute(null, List.of("echo")));
    }

    @Test
    void shouldRejectMissingRecipeInContext() {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, null, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("echo")));
    }

    @Test
    void shouldRejectMissingCommand() {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    @Test
    void shouldRejectNullArgs() {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, null));
    }

    @Test
    void shouldRejectSaveVarWithoutName() {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--saveVar")));
    }

    @Test
    void shouldAcceptSaveVarEqualsSyntax() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "value\n");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("--saveVar=out", "echo", "hello"));

        assertEquals("value\n", context.getRecipeVariable("out"));
    }

    @Test
    void shouldRejectSaveVarEqualsWithoutValue() {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--saveVar=")));
    }

    @Test
    void shouldRejectLevainShellInsideEnvOrShell() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setCommands(Map.of(
                "env", List.of("levainShell echo nope")
        ));

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalStateException.class, () -> action.execute(context, List.of("echo", "ok")));
        verify(actionExecutor, never()).executeCommands(any(), any());
    }

    @Test
    void shouldIgnoreNullShellActions() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setCommands(Map.of(
            "shell", java.util.Arrays.asList(null, "echo ok")
        ));

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        verify(actionExecutor).executeCommands(eq(java.util.Arrays.asList(null, "echo ok")), any());
    }

    @Test
    void shouldRejectLevainShellExactAction() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setCommands(Map.of(
                "shell", List.of("levainShell")
        ));

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalStateException.class, () -> action.execute(context, List.of("echo", "ok")));
        verify(actionExecutor, never()).executeCommands(any(), any());
    }

    @Test
    void shouldExecuteShellAndEnvActionsInOrder() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setCommands(Map.of(
                "shell", List.of("echo shell"),
                "env", List.of("echo env")
        ));

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        verify(actionExecutor).executeCommands(eq(List.of("echo shell", "echo env")), any());
    }

    @Test
    void shouldUseRecipeDirWhenProvided() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setRecipesDir(tempDir.resolve("recipes").toString());
        recipe.setCommands(Map.of(
                "shell", List.of("echo ok")
        ));

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        verify(actionExecutor).executeCommands(eq(List.of("echo ok")), argThat(ctx ->
                ctx.getRecipeDir().equals(Path.of(recipe.getRecipesDir()))));
    }

    @Test
    void shouldSaveVarWithStripCRLF() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "hello\r\n");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("--saveVar", "out", "--stripCRLF", "echo", "hello"));

        assertEquals("hello", context.getRecipeVariable("out"));
        verify(actionExecutor, times(0)).executeCommands(any(), any());
    }

    @Test
    void shouldSaveVarWithoutStrip() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "hello\n");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("--saveVar", "out", "echo", "hello"));

        assertEquals("hello\n", context.getRecipeVariable("out"));
    }

    @Test
    void shouldSaveEmptyStringWhenOutputNull() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, null);

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("--saveVar", "out", "echo", "hello"));

        assertEquals("", context.getRecipeVariable("out"));
    }

    @Test
    void shouldIgnoreErrorsWhenConfigured() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(2, "nope");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("--ignoreErrors", "echo", "ok"));
    }

    @Test
    void shouldThrowWhenCommandFails() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(2, "nope");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        assertThrows(RuntimeException.class, () -> action.execute(context, List.of("echo", "ok")));
    }

    @Test
    void shouldBuildWindowsCommand() throws Exception {
        System.setProperty("os.name", "Windows 11");

        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "hello"));

        assertEquals(List.of("cmd.exe", "/u", "/c", "echo", "hello"), action.capturedCommand);
    }

    @Test
    void shouldBuildUnixCommandWithCustomShell() throws Exception {
        System.setProperty("os.name", "Linux");
        config.setShellPath("/custom/sh");

        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "hello world", "a'b"));

        assertEquals("/custom/sh", action.capturedCommand.get(0));
        assertEquals("-lc", action.capturedCommand.get(1));
        assertTrue(action.capturedCommand.get(2).contains("'hello world'"));
    }

    @Test
    void shouldBuildUnixCommandWithDefaultShell() throws Exception {
        System.setProperty("os.name", "Linux");
        config.setShellPath(null);

        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("", "plain"));

        String expectedShell = Files.exists(Path.of("/bin/bash")) ? "/bin/bash" : "/bin/sh";
        assertEquals(expectedShell, action.capturedCommand.get(0));
        assertTrue(action.capturedCommand.get(2).contains("''"));
    }

    @Test
    void shouldTreatBlankShellPathAsUnset() throws Exception {
        System.setProperty("os.name", "Linux");
        config.setShellPath(" ");

        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "hello"));

        String expectedShell = Files.exists(Path.of("/bin/bash")) ? "/bin/bash" : "/bin/sh";
        assertEquals(expectedShell, action.capturedCommand.get(0));
    }

    @Test
    void shouldUseShWhenBashMissing() throws Exception {
        System.setProperty("os.name", "Linux");
        config.setShellPath(null);

        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe));

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.exists(Path.of("/bin/bash"))).thenReturn(false);

            CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
            ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
            action.execute(context, List.of("echo", "hello"));

            assertEquals("/bin/sh", action.capturedCommand.get(0));
        }
    }

    @Test
    void shouldUseRecipeWhenResolveFails() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");
        recipe.setCommands(new HashMap<>());

        when(recipeService.resolveRecipe("test-pkg")).thenThrow(new RuntimeException("boom"));

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));
    }

    @Test
    void shouldUseRecipeWhenNameMissing() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setCommands(new HashMap<>());

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, recipeService, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        verify(recipeService, never()).resolveRecipe(any());
    }

    @Test
    void shouldUseRecipeWhenServiceNull() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        TestLevainShellAction action = new TestLevainShellAction(actionExecutor, null, config);
        action.nextResult = new LevainShellAction.ProcessResult(0, "ok");

        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));
    }

    @Test
    void shouldResolveShellRecipesWhenRecipeNull() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);

        var method = LevainShellAction.class.getDeclaredMethod("resolveShellRecipes", Recipe.class);
        method.setAccessible(true);

        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(action, new Object[] { null }));
        assertEquals(NullPointerException.class, thrown.getCause().getClass());
    }

    @Test
    void shouldReturnWhenRunShellActionsRecipeNull() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);

        var method = LevainShellAction.class.getDeclaredMethod("runShellActions", Recipe.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(action, new Object[] { null }));
    }

    @Test
    void shouldReturnWhenRunShellActionsCommandsNull() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        var method = LevainShellAction.class.getDeclaredMethod("runShellActions", Recipe.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(action, new Object[] { recipe }));
    }

    @Test
    void shouldPopulatePackageNamesInEnvironment() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        Recipe dep = new Recipe();
        dep.setName("dep");

        Recipe blank = new Recipe();
        blank.setName(" ");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of(recipe, dep, blank));

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        assertEquals("test-pkg;dep", action.capturedEnv.get("LEVAIN_PKG_NAMES"));
    }

    @Test
    void shouldSkipPackageNamesWhenBlank() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName(" ");

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        assertTrue(action.capturedEnv.get("LEVAIN_PKG_NAMES") == null);
    }

    @Test
    void shouldSkipPackageNamesWhenEmptyRecipeList() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        when(recipeService.resolveRecipe("test-pkg")).thenReturn(List.of());

        CapturingLevainShellAction action = new CapturingLevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);
        action.execute(context, List.of("echo", "ok"));

        assertTrue(action.capturedEnv.get("LEVAIN_PKG_NAMES") == null);
    }

    @Test
    void shouldBuildEnvironmentWithNullRecipeList() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);

        var method = LevainShellAction.class.getDeclaredMethod("buildEnvironment", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) method.invoke(action, new Object[] { null });
        assertTrue(env.get("LEVAIN_PKG_NAMES") == null);
    }

    @Test
    void shouldHandleNullStripLineEnding() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);

        var method = LevainShellAction.class.getDeclaredMethod("stripLineEnding", String.class);
        method.setAccessible(true);

        Object result = method.invoke(action, new Object[] { null });
        assertNull(result);
    }

    @Test
    void shouldQuoteForShellWithAndWithoutSpecialChars() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);

        var method = LevainShellAction.class.getDeclaredMethod("quoteForShell", String.class);
        method.setAccessible(true);

        assertEquals("''", method.invoke(action, new Object[] { null }));
        assertEquals("plain", method.invoke(action, new Object[] { "plain" }));
        assertEquals("'hello world'", method.invoke(action, new Object[] { "hello world" }));
        assertEquals("'has\"quote'", method.invoke(action, new Object[] { "has\"quote" }));
        assertEquals("'has'\"'\"'quote'", method.invoke(action, new Object[] { "has'quote" }));
        assertEquals("'path\\dir'", method.invoke(action, new Object[] { "path\\dir" }));
    }

    @Test
    void shouldRunCommandWithAndWithoutCapture() throws Exception {
        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        var method = LevainShellAction.class.getDeclaredMethod("runCommand", List.class, Map.class, boolean.class);
        method.setAccessible(true);

        Map<String, String> env = new HashMap<>(System.getenv());
        @SuppressWarnings("unchecked")
        LevainShellAction.ProcessResult captured = (LevainShellAction.ProcessResult) method.invoke(
            action, List.of("echo", "hello"), env, true);
        assertEquals(0, captured.exitCode);
        assertTrue(captured.output.contains("hello"));

        @SuppressWarnings("unchecked")
        LevainShellAction.ProcessResult uncaptured = (LevainShellAction.ProcessResult) method.invoke(
            action, List.of("echo", "hello"), env, false);
        assertEquals(0, uncaptured.exitCode);
        assertNull(uncaptured.output);
    }

    static class TestLevainShellAction extends LevainShellAction {
        ProcessResult nextResult;

        TestLevainShellAction(ActionExecutor actionExecutor, RecipeService recipeService, Config config) {
            super(actionExecutor, recipeService, config);
        }

        @Override
        protected ProcessResult runCommand(List<String> command, Map<String, String> env, boolean captureOutput) {
            return nextResult != null ? nextResult : new ProcessResult(0, "");
        }
    }

    static class CapturingLevainShellAction extends LevainShellAction {
        List<String> capturedCommand;
        Map<String, String> capturedEnv;

        CapturingLevainShellAction(ActionExecutor actionExecutor, RecipeService recipeService, Config config) {
            super(actionExecutor, recipeService, config);
        }

        @Override
        protected ProcessResult runCommand(List<String> command, Map<String, String> env, boolean captureOutput) {
            this.capturedCommand = command;
            this.capturedEnv = env;
            return new ProcessResult(0, "");
        }
    }
}
