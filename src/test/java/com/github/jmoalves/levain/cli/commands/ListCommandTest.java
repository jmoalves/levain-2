package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.jmoalves.levain.service.RecipeService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {

    @Mock
    private RecipeService recipeService;

    private ListCommand command;

    @BeforeEach
    void setUp() {
        command = new ListCommand(recipeService);
    }

    @Test
    void shouldListRecipesWithNoFilterAndEmptyResult() {
        when(recipeService.listRecipes(null)).thenReturn(List.of());

        int exitCode = new CommandLine(command).execute();

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
    }

    @Test
    void shouldListRecipesWithFilterAndEmptyResult() {
        when(recipeService.listRecipes("jdk")).thenReturn(List.of());

        int exitCode = new CommandLine(command).execute("jdk");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes("jdk");
    }

    @Test
    void shouldListRecipesWithResults() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21", "git"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(false);
        when(recipeService.isInstalled("git")).thenReturn(false);

        int exitCode = new CommandLine(command).execute();

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
    }

    @Test
    void shouldListInstalledRecipesOnly() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21", "git", "maven"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);
        when(recipeService.isInstalled("git")).thenReturn(false);
        when(recipeService.isInstalled("maven")).thenReturn(true);

        int exitCode = new CommandLine(command).execute("--installed");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
    }

    @Test
    void shouldListAvailableRecipesOnly() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21", "git", "maven"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);
        when(recipeService.isInstalled("git")).thenReturn(false);
        when(recipeService.isInstalled("maven")).thenReturn(true);

        int exitCode = new CommandLine(command).execute("--available");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
    }

    @Test
    void shouldShowInstallationStatusIndicators() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21", "git"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);
        when(recipeService.isInstalled("git")).thenReturn(false);

        int exitCode = new CommandLine(command).execute();

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
        verify(recipeService).isInstalled("jdk-21");
        verify(recipeService).isInstalled("git");
    }

    @Test
    void shouldRejectMutuallyExclusiveOptions() {
        // No stubbing needed as command should return early with error

        int exitCode = new CommandLine(command).execute("--installed", "--available");

        assertEquals(1, exitCode);
    }

    @Test
    void shouldFilterInstalledRecipes() {
        when(recipeService.listRecipes("jdk")).thenReturn(List.of("jdk-21", "jdk-17"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);
        when(recipeService.isInstalled("jdk-17")).thenReturn(false);

        int exitCode = new CommandLine(command).execute("jdk", "--installed");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes("jdk");
    }
}
