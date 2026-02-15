package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.action.ActionExecutor;
import com.github.jmoalves.levain.config.Config;
import jakarta.enterprise.inject.Vetoed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ShellService Additional Tests")
class ShellServiceAdditionalTest {
    private String originalOsName;

    @Vetoed
    private static class TestShellService extends ShellService {
        private List<String> capturedCommand;
        private boolean throwInterrupted;

        TestShellService(Config config, ActionExecutor actionExecutor, RecipeService recipeService) {
            // Manually set the injected fields using reflection
            try {
                var configField = ShellService.class.getDeclaredField("config");
                configField.setAccessible(true);
                configField.set(this, config);

                var actionExecutorField = ShellService.class.getDeclaredField("actionExecutor");
                actionExecutorField.setAccessible(true);
                actionExecutorField.set(this, actionExecutor);

                var recipeServiceField = ShellService.class.getDeclaredField("recipeService");
                recipeServiceField.setAccessible(true);
                recipeServiceField.set(this, recipeService);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject mock dependencies", e);
            }
        }

        void setThrowInterrupted(boolean throwInterrupted) {
            this.throwInterrupted = throwInterrupted;
        }

        List<String> getCapturedCommand() {
            return capturedCommand;
        }

        @Override
        protected void runProcess(List<String> command, java.util.Map<String, String> environment, Path workingDir)
                throws IOException, InterruptedException {
            this.capturedCommand = command;
            if (throwInterrupted) {
                throw new InterruptedException("Interrupted for test");
            }
        }
    }

    @BeforeEach
    void setUp() {
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void tearDown() {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
    }

    @Test
    @DisplayName("Should be ApplicationScoped")
    void testIsApplicationScoped() {
        assertTrue(ShellService.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class),
                "ShellService should be ApplicationScoped");
    }

    @Test
    @DisplayName("Should build Windows shell command")
    void testOpenShellWindowsCommand() throws IOException {
        System.setProperty("os.name", "Windows 11");

        Config mockConfig = createMockConfig();
        ActionExecutor mockActionExecutor = mock(ActionExecutor.class);
        RecipeService mockRecipeService = mock(RecipeService.class);

        TestShellService service = new TestShellService(mockConfig, mockActionExecutor, mockRecipeService);
        service.openShell(List.of("jdk-21", "maven"));

        List<String> command = service.getCapturedCommand();
        assertNotNull(command, "Command should be captured");
        assertEquals(List.of(
                "cmd.exe",
                "/k",
                "echo Levain shell initialized with packages: jdk-21, maven"), command);
    }

    @Test
    @DisplayName("Should build Unix shell command")
    void testOpenShellUnixCommand() throws IOException {
        System.setProperty("os.name", "Linux");

        Config mockConfig = createMockConfig();
        ActionExecutor mockActionExecutor = mock(ActionExecutor.class);
        RecipeService mockRecipeService = mock(RecipeService.class);

        TestShellService service = new TestShellService(mockConfig, mockActionExecutor, mockRecipeService);
        service.openShell(List.of("jdk-21", "maven"));

        List<String> command = service.getCapturedCommand();
        assertNotNull(command, "Command should be captured");
        assertEquals(List.of(
                "/bin/bash",
                "-c",
                "echo 'Levain shell initialized with packages: jdk-21, maven' && bash"), command);
    }

    @Test
    @DisplayName("Should convert InterruptedException to IOException")
    void testOpenShellInterrupted() {
        System.setProperty("os.name", "Linux");

        Config mockConfig = createMockConfig();
        ActionExecutor mockActionExecutor = mock(ActionExecutor.class);
        RecipeService mockRecipeService = mock(RecipeService.class);

        TestShellService service = new TestShellService(mockConfig, mockActionExecutor, mockRecipeService);
        service.setThrowInterrupted(true);

        IOException ex = assertThrows(IOException.class,
                () -> service.openShell(List.of("jdk-21")),
                "Should throw IOException when interrupted");
        assertTrue(ex.getMessage().contains("interrupted"));
    }

    private Config createMockConfig() {
        Config mockConfig = mock(Config.class);
        when(mockConfig.getVariables()).thenReturn(new HashMap<>());
        when(mockConfig.getLevainHome()).thenReturn(Path.of("/test/levain"));
        return mockConfig;
    }
}
