package com.github.jmoalves.levain.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jmoalves.levain.cli.commands.ListCommand;
import com.github.jmoalves.levain.service.RecipeService;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import picocli.CommandLine;

/**
 * Unit tests for CdiCommandFactory to ensure command classes are properly
 * instantiated with their dependencies resolved through CDI.
 */
@DisplayName("CdiCommandFactory Integration Tests")
class CdiCommandFactoryTest {

    private SeContainer container;
    private CdiCommandFactory factory;

    @BeforeEach
    void setUp() {
        // Initialize CDI container for testing
        container = SeContainerInitializer.newInstance().initialize();
        factory = new CdiCommandFactory();
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.close();
        }
    }

    @Test
    @DisplayName("Should create ListCommand with constructor-injected RecipeService")
    void shouldCreateListCommandWithDependencies() throws Exception {
        // Act
        ListCommand command = factory.create(ListCommand.class);

        // Assert
        assertNotNull(command, "ListCommand should be created");
        assertTrue(command instanceof ListCommand, "Created instance should be ListCommand");

        // Verify the command has its service injected (by calling a method that uses
        // it)
        Integer result = command.call();
        assertNotNull(result, "Command should execute successfully with injected service");
    }

    @Test
    @DisplayName("Should create RecipeService from CDI")
    void shouldCreateRecipeService() throws Exception {
        // Act
        RecipeService service = factory.create(RecipeService.class);

        // Assert
        assertNotNull(service, "RecipeService should be created");
        assertNotNull(service.listRecipes(null), "RecipeService should be functional");
    }

    @Test
    @DisplayName("Should fail gracefully for unknown classes")
    void shouldFailGracefullyForUnknownClasses() {
        // Arrange
        class UnknownClass {
        }

        // Use factory that suppresses error logging for this expected failure
        CdiCommandFactory testFactory = new CdiCommandFactory(false);

        // Act & Assert - expect an exception to be thrown
        assertThrows(Exception.class, () -> testFactory.create(UnknownClass.class),
                "Factory should throw an exception for unknown CDI beans");
    }

    @Test
    @DisplayName("Should throw execution exception when CDI unavailable")
    void shouldFailWhenCdiUnavailable() {
        container.close();

        CdiCommandFactory testFactory = new CdiCommandFactory(false);

        assertThrows(CommandLine.ExecutionException.class,
                () -> testFactory.create(ListCommand.class));
    }
}
