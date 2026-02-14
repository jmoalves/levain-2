package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.repository.Repository;
import com.github.jmoalves.levain.repository.RepositoryFactory;
import com.github.jmoalves.levain.repository.Registry;
import com.github.jmoalves.levain.service.DependencyResolver.ResolutionResult;

/**
 * Unit tests for InstallService using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class InstallServiceTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private RepositoryFactory repositoryFactory;

    @Mock
    private VariableSubstitutionService variableSubstitutionService;

    @Mock
    private com.github.jmoalves.levain.action.ActionExecutor actionExecutor;

    @Mock
    private com.github.jmoalves.levain.config.Config config;

    @Mock
    private DependencyResolver dependencyResolver;

    private InstallService installService;
    private Recipe mockRecipe;

    @BeforeEach
    void setUp() {
        installService = new InstallService(recipeService, repositoryFactory, variableSubstitutionService,
                actionExecutor, config, dependencyResolver);
        lenient().when(config.getLevainHome()).thenReturn(Path.of("/tmp/levain"));

        mockRecipe = new Recipe();
        mockRecipe.setName("test-package");
        mockRecipe.setVersion("1.0.0");
        mockRecipe.setDescription("Test recipe");
    }

    @Test
    void testInstallPackageNotFound() {
        String packageName = "nonexistent-package";

        when(dependencyResolver.resolveAndSortWithMissing(java.util.List.of(packageName)))
            .thenReturn(new ResolutionResult(java.util.List.of(), java.util.List.of(packageName)));

        assertThrows(IllegalArgumentException.class, () -> {
            installService.install(packageName);
        });

        verify(dependencyResolver).resolveAndSortWithMissing(java.util.List.of(packageName));
    }

    @Test
    void testInstallPackageWithRecipeServiceException() {
        String packageName = "failing-package";

        when(dependencyResolver.resolveAndSortWithMissing(java.util.List.of(packageName)))
                .thenThrow(new RuntimeException("Recipe not found"));

        assertThrows(RuntimeException.class, () -> {
            installService.install(packageName);
        });

        verify(dependencyResolver).resolveAndSortWithMissing(java.util.List.of(packageName));
    }

    @Test
    void testInstallSkipsWhenAlreadyInstalled() throws Exception {
        Registry registry = org.mockito.Mockito.mock(Registry.class);
        when(registry.isInstalled("test-package")).thenReturn(true);
        setRegistry(installService, registry);

        when(dependencyResolver.resolveAndSortWithMissing(java.util.List.of("test-package")))
            .thenReturn(new ResolutionResult(java.util.List.of(mockRecipe), java.util.List.of()));

        // When already installed and force=false, install completes without exception
        installService.install("test-package", false);
        
        verify(registry).isInstalled("test-package");
        verify(dependencyResolver).resolveAndSortWithMissing(java.util.List.of("test-package"));
    }

    @Test
    void testInstallForceInstallsWhenAlreadyInstalled() throws Exception {
        Registry registry = org.mockito.Mockito.mock(Registry.class);
        lenient().when(registry.isInstalled("test-package")).thenReturn(true);
        setRegistry(installService, registry);

        when(dependencyResolver.resolveAndSortWithMissing(java.util.List.of("test-package")))
            .thenReturn(new ResolutionResult(java.util.List.of(mockRecipe), java.util.List.of()));
        when(recipeService.loadRecipe("test-package")).thenReturn(mockRecipe);
        when(recipeService.getRecipeYamlContent("test-package"))
                .thenReturn(Optional.of("name: test-package\nversion: 1.0.0\n"));
        when(recipeService.findSourceRepository("test-package")).thenReturn(Optional.empty());

        installService.install("test-package", true);

        verify(registry).store(org.mockito.Mockito.eq(mockRecipe),
                org.mockito.Mockito.eq("name: test-package\nversion: 1.0.0\n"),
                isNull(), isNull());
    }

    @Test
    void testInstallUsesSerializedYamlWhenContentMissing() throws Exception {
        Registry registry = org.mockito.Mockito.mock(Registry.class);
        when(registry.isInstalled("test-package")).thenReturn(false);
        setRegistry(installService, registry);

        when(dependencyResolver.resolveAndSortWithMissing(java.util.List.of("test-package")))
            .thenReturn(new ResolutionResult(java.util.List.of(mockRecipe), java.util.List.of()));
        when(recipeService.loadRecipe("test-package")).thenReturn(mockRecipe);
        when(recipeService.getRecipeYamlContent("test-package")).thenReturn(Optional.empty());
        when(recipeService.findSourceRepository("test-package")).thenReturn(Optional.empty());

        installService.install("test-package", false);

        ArgumentCaptor<String> yamlCaptor = ArgumentCaptor.forClass(String.class);
        verify(registry).store(org.mockito.Mockito.eq(mockRecipe), yamlCaptor.capture(), isNull(), isNull());
        assertTrue(yamlCaptor.getValue().contains("test-package"));
    }

    @Test
    void testInstallFromSpecificRepositorySuccess() throws Exception {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryFactory.createRepository("dir:/tmp")).thenReturn(repository);
        when(repository.resolveRecipe("test-package")).thenReturn(Optional.of(mockRecipe));
        when(repository.getRecipeYamlContent("test-package"))
                .thenReturn(Optional.of("name: test-package\nversion: 1.0.0\n"));

        Registry registry = org.mockito.Mockito.mock(Registry.class);
        setRegistry(installService, registry);

        installService.install("test-package", "dir:/tmp");

        verify(repository).init();
        verify(registry).store(org.mockito.Mockito.eq(mockRecipe),
                org.mockito.Mockito.eq("name: test-package\nversion: 1.0.0\n"),
                isNull(), isNull());
    }

    @Test
    void testInstallFromSpecificRepositoryMissingYaml() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryFactory.createRepository("dir:/tmp")).thenReturn(repository);
        when(repository.resolveRecipe("test-package")).thenReturn(Optional.of(mockRecipe));
        when(repository.getRecipeYamlContent("test-package")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> installService.install("test-package", "dir:/tmp"));
    }

    @Test
    void testIsInstalledDelegatesToRegistry() throws Exception {
        Registry registry = org.mockito.Mockito.mock(Registry.class);
        when(registry.isInstalled("test-package")).thenReturn(true);
        setRegistry(installService, registry);

        assertTrue(installService.isInstalled("test-package"));
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

    private static void setRegistry(InstallService service, Registry registry) throws Exception {
        Field field = InstallService.class.getDeclaredField("registry");
        field.setAccessible(true);
        field.set(service, registry);
    }
}
