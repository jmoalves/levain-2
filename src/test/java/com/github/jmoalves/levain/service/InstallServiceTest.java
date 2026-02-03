package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.model.Recipe;

/**
 * Unit tests for InstallService using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class InstallServiceTest {

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private InstallService installService;

    private Recipe mockRecipe;

    @BeforeEach
    void setUp() {
        mockRecipe = new Recipe();
        mockRecipe.setName("test-package");
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
