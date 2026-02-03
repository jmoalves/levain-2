package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.repository.RepositoryFactory;

/**
 * Unit tests for InstallService using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class InstallServiceTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private RepositoryFactory repositoryFactory;

    private InstallService installService;
    private Recipe mockRecipe;

    @BeforeEach
    void setUp() {
        installService = new InstallService(recipeService, repositoryFactory);

        mockRecipe = new Recipe();
        mockRecipe.setName("test-package");
        mockRecipe.setVersion("1.0.0");
        mockRecipe.setDescription("Test recipe");
    }

    @Test
    void testInstallPackageNotFound() {
        String packageName = "nonexistent-package";

        when(recipeService.loadRecipe(packageName)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            installService.install(packageName);
        });

        verify(recipeService).loadRecipe(packageName);
    }

    @Test
    void testInstallPackageWithRecipeServiceException() {
        String packageName = "failing-package";

        when(recipeService.loadRecipe(packageName))
                .thenThrow(new RuntimeException("Recipe not found"));

        assertThrows(RuntimeException.class, () -> {
            installService.install(packageName);
        });

        verify(recipeService).loadRecipe(packageName);
    }

    @Test
    void testGetRegistry() {
        assertNotNull(installService.getRegistry());
    }

    @Test
    void testAlreadyInstalledException() {
        AlreadyInstalledException exception = new AlreadyInstalledException("test message");
        assertEquals("test message", exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testAlreadyInstalledExceptionWithRecipeName() {
        String recipeName = "my-recipe";
        AlreadyInstalledException exception = new AlreadyInstalledException(recipeName + " is already installed");
        assertTrue(exception.getMessage().contains(recipeName));
    }
}
