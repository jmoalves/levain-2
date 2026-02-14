package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void shouldRejectMissingCommand() {
        Recipe recipe = new Recipe();
        recipe.setName("test-pkg");

        LevainShellAction action = new LevainShellAction(actionExecutor, recipeService, config);
        ActionContext context = new ActionContext(config, recipe, tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
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

        CapturingLevainShellAction(ActionExecutor actionExecutor, RecipeService recipeService, Config config) {
            super(actionExecutor, recipeService, config);
        }

        @Override
        protected ProcessResult runCommand(List<String> command, Map<String, String> env, boolean captureOutput) {
            this.capturedCommand = command;
            return new ProcessResult(0, "");
        }
    }
}
