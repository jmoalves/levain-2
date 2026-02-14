package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        protected void runProcess(List<String> command, Map<String, String> environment) {
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
}
