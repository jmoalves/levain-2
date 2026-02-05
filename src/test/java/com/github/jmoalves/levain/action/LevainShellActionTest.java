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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LevainShellActionTest {

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
}
