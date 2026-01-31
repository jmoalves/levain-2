package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstallService using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class InstallServiceTest {

    @Mock
    private RecipeService recipeService;

    private InstallService installService;

    private Recipe mockRecipe;

    @BeforeEach
    void setUp() {
        installService = new InstallService(recipeService);
        mockRecipe = new Recipe();
        mockRecipe.setVersion("1.0.0");
        mockRecipe.setDescription("Test recipe");
    }

    @Test
    void testInstallPackageSuccessfully() throws Exception {
        // Arrange
        String packageName = "test-package";
        when(recipeService.loadRecipe(packageName)).thenReturn(mockRecipe);

        // Act
        installService.install(packageName);

        // Assert
        verify(recipeService, times(1)).loadRecipe(packageName);
    }

    @Test
    void testInstallPackageWithRecipeServiceException() {
        // Arrange
        String packageName = "failing-package";
        when(recipeService.loadRecipe(packageName))
            .thenThrow(new RuntimeException("Recipe not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            installService.install(packageName);
        });
        
        verify(recipeService, times(1)).loadRecipe(packageName);
    }
}
