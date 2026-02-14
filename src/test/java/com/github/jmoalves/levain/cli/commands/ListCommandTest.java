package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.jmoalves.levain.service.RecipeService;
import com.github.jmoalves.levain.repository.RecipeMetadata;
import com.github.jmoalves.levain.repository.Repository;

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

    @Test
    void shouldShowSourceForInstalledRecipes() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);

        RecipeMetadata metadata = new RecipeMetadata();
        metadata.setSourceRepository("Registry");
        metadata.setSourceRepositoryUri("registry://test");
        when(recipeService.getInstalledMetadata("jdk-21")).thenReturn(java.util.Optional.of(metadata));

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).getInstalledMetadata("jdk-21");
    }

    @Test
    void shouldShowSourceForAvailableRecipes() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("git"));
        when(recipeService.isInstalled("git")).thenReturn(false);

        Repository repo = org.mockito.Mockito.mock(Repository.class);
        when(repo.getName()).thenReturn("repo1");
        when(repo.getUri()).thenReturn("https://example.com/repo1");
        when(recipeService.findSourceRepository("git")).thenReturn(java.util.Optional.of(repo));

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).findSourceRepository("git");
    }

    @Test
    void shouldHandleUnknownSourceGracefully() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("nodejs"));
        when(recipeService.isInstalled("nodejs")).thenReturn(false);
        when(recipeService.findSourceRepository("nodejs")).thenReturn(java.util.Optional.empty());

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).findSourceRepository("nodejs");
    }

    @Test
    void shouldHandleMissingInstalledMetadataSource() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);
        when(recipeService.getInstalledMetadata("jdk-21")).thenReturn(java.util.Optional.empty());

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).getInstalledMetadata("jdk-21");
        verify(recipeService, never()).findSourceRepository("jdk-21");
    }

    @Test
    void shouldHandleSourceWithBlankUri() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("git"));
        when(recipeService.isInstalled("git")).thenReturn(false);

        Repository repo = org.mockito.Mockito.mock(Repository.class);
        when(repo.getName()).thenReturn("repo1");
        when(repo.getUri()).thenReturn(" ");
        when(recipeService.findSourceRepository("git")).thenReturn(java.util.Optional.of(repo));

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).findSourceRepository("git");
    }

    @Test
    void shouldHandleInstalledSourceWithNullMetadataFields() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);

        RecipeMetadata metadata = new RecipeMetadata();
        when(recipeService.getInstalledMetadata("jdk-21")).thenReturn(java.util.Optional.of(metadata));

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).getInstalledMetadata("jdk-21");
    }

    @Test
    void shouldHandleAvailableSourceWithNullName() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("git"));
        when(recipeService.isInstalled("git")).thenReturn(false);

        Repository repo = org.mockito.Mockito.mock(Repository.class);
        when(repo.getName()).thenReturn(null);
        when(repo.getUri()).thenReturn("https://example.com/repo");
        when(recipeService.findSourceRepository("git")).thenReturn(java.util.Optional.of(repo));

        int exitCode = new CommandLine(command).execute("--source");

        assertEquals(0, exitCode);
        verify(recipeService).findSourceRepository("git");
    }

    @Test
    void shouldHandleNoInstalledResultsWithFilter() {
        when(recipeService.listRecipes("jdk")).thenReturn(List.of("jdk-21"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(false);

        int exitCode = new CommandLine(command).execute("jdk", "--installed");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes("jdk");
    }

    @Test
    void shouldHandleNoAvailableResultsWithoutFilter() {
        when(recipeService.listRecipes(null)).thenReturn(List.of("jdk-21"));
        when(recipeService.isInstalled("jdk-21")).thenReturn(true);

        int exitCode = new CommandLine(command).execute("--available");

        assertEquals(0, exitCode);
        verify(recipeService).listRecipes(null);
    }

    @Test
    void shouldReturnErrorWhenListingFails() {
        when(recipeService.listRecipes(null)).thenThrow(new RuntimeException("boom"));

        int exitCode = new CommandLine(command).execute();

        assertEquals(1, exitCode);
    }
}
