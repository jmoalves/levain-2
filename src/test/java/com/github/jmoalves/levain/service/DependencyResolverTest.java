package com.github.jmoalves.levain.service;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Comprehensive tests for DependencyResolver.
 * 
 * This is a critical component that must handle:
 * - Simple dependencies (A -> B)
 * - Multi-level dependencies (A -> B -> C)
 * - Diamond dependencies (A -> B,C; B -> D; C -> D)
 * - Circular dependencies detection
 * - Missing recipes
 * - Recipes with no dependencies
 */
@ExtendWith(MockitoExtension.class)
class DependencyResolverTest {

    @Mock
    private RecipeService recipeService;

    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver(recipeService);
    }

    // ========== Basic Functionality Tests ==========

    @Test
    void testResolveNoDependencies() {
        Recipe standalone = createRecipe("standalone", "1.0.0");
        when(recipeService.loadRecipe("standalone")).thenReturn(standalone);

        List<Recipe> result = resolver.resolveAndSort("standalone");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("standalone", result.get(0).getName());
    }

    @Test
    void testResolveEmptyDependenciesList() {
        Recipe withEmptyList = createRecipe("package", "1.0.0", Collections.emptyList());
        when(recipeService.loadRecipe("package")).thenReturn(withEmptyList);

        List<Recipe> result = resolver.resolveAndSort("package");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("package", result.get(0).getName());
    }

    // ========== Linear Dependency Chain Tests ==========

    @Test
    void testResolveSimpleLinearDependency() {
        // A depends on B
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("B", result.get(0).getName(), "B should be installed first");
        assertEquals("A", result.get(1).getName(), "A should be installed last");
    }

    @Test
    void testResolveThreeLevelDependency() {
        // A -> B -> C (linear chain)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("C"));
        Recipe recipeC = createRecipe("C", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("C", result.get(0).getName(), "C should be installed first");
        assertEquals("B", result.get(1).getName(), "B should be installed second");
        assertEquals("A", result.get(2).getName(), "A should be installed last");
    }

    @Test
    void testResolveDeepDependencyChain() {
        // A -> B -> C -> D -> E (5-level chain)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("C"));
        Recipe recipeC = createRecipe("C", "1.0.0", Arrays.asList("D"));
        Recipe recipeD = createRecipe("D", "1.0.0", Arrays.asList("E"));
        Recipe recipeE = createRecipe("E", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);
        when(recipeService.loadRecipe("D")).thenReturn(recipeD);
        when(recipeService.loadRecipe("E")).thenReturn(recipeE);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("E", result.get(0).getName());
        assertEquals("D", result.get(1).getName());
        assertEquals("C", result.get(2).getName());
        assertEquals("B", result.get(3).getName());
        assertEquals("A", result.get(4).getName());
    }

    // ========== Multiple Dependencies Tests ==========

    @Test
    void testResolveMultipleDependencies() {
        // A depends on B and C
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B", "C"));
        Recipe recipeB = createRecipe("B", "1.0.0");
        Recipe recipeC = createRecipe("C", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(3, result.size());
        
        // B and C can be in any order, but both must come before A
        int indexA = findIndex(result, "A");
        int indexB = findIndex(result, "B");
        int indexC = findIndex(result, "C");
        
        assertTrue(indexB < indexA, "B must come before A");
        assertTrue(indexC < indexA, "C must come before A");
        assertEquals(2, indexA, "A should be last");
    }

    @Test
    void testResolveFanOutDependencies() {
        // A depends on B, C, D (3 direct dependencies)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B", "C", "D"));
        Recipe recipeB = createRecipe("B", "1.0.0");
        Recipe recipeC = createRecipe("C", "1.0.0");
        Recipe recipeD = createRecipe("D", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);
        when(recipeService.loadRecipe("D")).thenReturn(recipeD);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(4, result.size());
        
        int indexA = findIndex(result, "A");
        assertEquals(3, indexA, "A should be last");
    }

    // ========== Diamond Dependency Tests ==========

    @Test
    void testResolveDiamondDependency() {
        // A depends on B and C, both B and C depend on D
        //    A
        //   / \
        //  B   C
        //   \ /
        //    D
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B", "C"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("D"));
        Recipe recipeC = createRecipe("C", "1.0.0", Arrays.asList("D"));
        Recipe recipeD = createRecipe("D", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);
        when(recipeService.loadRecipe("D")).thenReturn(recipeD);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(4, result.size());
        
        // D must come first, B and C in middle (any order), A last
        assertEquals("D", result.get(0).getName(), "D should be installed first");
        assertEquals("A", result.get(3).getName(), "A should be installed last");
        
        int indexB = findIndex(result, "B");
        int indexC = findIndex(result, "C");
        assertTrue(indexB == 1 || indexB == 2, "B should be in middle");
        assertTrue(indexC == 1 || indexC == 2, "C should be in middle");
    }

    @Test
    void testResolveComplexDiamondWithExtraLevels() {
        // More complex diamond: A -> B,C -> D,E -> F
        //      A
        //     / \
        //    B   C
        //    |   |
        //    D   E
        //     \ /
        //      F
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B", "C"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("D"));
        Recipe recipeC = createRecipe("C", "1.0.0", Arrays.asList("E"));
        Recipe recipeD = createRecipe("D", "1.0.0", Arrays.asList("F"));
        Recipe recipeE = createRecipe("E", "1.0.0", Arrays.asList("F"));
        Recipe recipeF = createRecipe("F", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);
        when(recipeService.loadRecipe("D")).thenReturn(recipeD);
        when(recipeService.loadRecipe("E")).thenReturn(recipeE);
        when(recipeService.loadRecipe("F")).thenReturn(recipeF);

        List<Recipe> result = resolver.resolveAndSort("A");

        assertNotNull(result);
        assertEquals(6, result.size());
        
        // F must be first, A must be last
        assertEquals("F", result.get(0).getName());
        assertEquals("A", result.get(5).getName());
        
        // D and E must come after F but before B and C
        int indexF = 0;
        int indexD = findIndex(result, "D");
        int indexE = findIndex(result, "E");
        int indexB = findIndex(result, "B");
        int indexC = findIndex(result, "C");
        
        assertTrue(indexD > indexF && indexD < indexB);
        assertTrue(indexE > indexF && indexE < indexC);
    }

    @Test
    void testResolveAndSortWithMissingReturnsMissingList() {
        Recipe recipeA = createRecipe("A", "1.0.0", List.of("B"));
        Recipe recipeC = createRecipe("C", "1.0.0");

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenThrow(new IllegalArgumentException("missing"));
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);

        DependencyResolver.ResolutionResult result =
                resolver.resolveAndSortWithMissing(List.of("A", "C", " "));

        assertEquals(1, result.recipes().size());
        assertEquals("C", result.recipes().get(0).getName());
        assertEquals(Set.of("B"), Set.copyOf(result.missing()));
    }

    @Test
    void testResolveAndSortWithMissingHandlesEmptyInput() {
        DependencyResolver.ResolutionResult result = resolver.resolveAndSortWithMissing(List.of());

        assertTrue(result.recipes().isEmpty());
        assertTrue(result.missing().isEmpty());
    }

    @Test
    void testResolveAndSortWithMissingHandlesNullInput() {
        DependencyResolver.ResolutionResult result = resolver.resolveAndSortWithMissing(null);

        assertTrue(result.recipes().isEmpty());
        assertTrue(result.missing().isEmpty());
    }

    @Test
    void testResolveAndSortWithMissingWhenRecipeMissing() {
        when(recipeService.loadRecipe("missing")).thenReturn(null);

        DependencyResolver.ResolutionResult result = resolver.resolveAndSortWithMissing(List.of("missing"));

        assertTrue(result.recipes().isEmpty());
        assertEquals(Set.of("missing"), Set.copyOf(result.missing()));
    }

    @Test
    void testResolveAndSortWithMissingDetectsCircularDependencies() {
        Recipe recipeA = createRecipe("A", "1.0.0", List.of("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", List.of("A"));

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveAndSortWithMissing(List.of("A")));
    }

    @Test
    void testFormatInstallationPlan() {
        Recipe dependency = createRecipe("dep", "1.0.0");
        Recipe main = createRecipe("main", "1.0.0", List.of("dep"));

        String plan = resolver.formatInstallationPlan(List.of(dependency, main));

        assertTrue(plan.contains("Installation Plan"));
        assertTrue(plan.contains("✓ dep"));
        assertTrue(plan.contains("→ main"));
    }

    // ========== Circular Dependency Detection Tests ==========

    @Test
    void testCircularDependencyDirectSelfReference() {
        // A depends on A (self-reference)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("A"));
        when(recipeService.loadRecipe("A")).thenReturn(recipeA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Circular dependency"));
        assertTrue(ex.getMessage().contains("A"));
    }

    @Test
    void testCircularDependencyTwoRecipes() {
        // A depends on B, B depends on A
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("A"));

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Circular dependency"));
    }

    @Test
    void testCircularDependencyThreeRecipes() {
        // A -> B -> C -> A (cycle)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("C"));
        Recipe recipeC = createRecipe("C", "1.0.0", Arrays.asList("A"));

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Circular dependency"));
    }

    @Test
    void testCircularDependencyInSubgraph() {
        // A -> B -> C -> D -> C (cycle in middle of chain)
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0", Arrays.asList("C"));
        Recipe recipeC = createRecipe("C", "1.0.0", Arrays.asList("D"));
        Recipe recipeD = createRecipe("D", "1.0.0", Arrays.asList("C"));

        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(recipeB);
        when(recipeService.loadRecipe("C")).thenReturn(recipeC);
        when(recipeService.loadRecipe("D")).thenReturn(recipeD);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Circular dependency"));
    }

    // ========== Error Handling Tests ==========

    @Test
    void testRecipeNotFound() {
        when(recipeService.loadRecipe("nonexistent")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("nonexistent");
        });

        assertTrue(ex.getMessage().contains("Recipe not found"));
        assertTrue(ex.getMessage().contains("nonexistent"));
    }

    @Test
    void testDependencyNotFound() {
        // A depends on B, but B doesn't exist
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Recipe not found"));
        assertTrue(ex.getMessage().contains("B"));
    }

    @Test
    void testMultipleMissingDependenciesFailsOnFirst() {
        // A depends on B and C, both missing - should fail on first one encountered
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B", "C"));
        when(recipeService.loadRecipe("A")).thenReturn(recipeA);
        when(recipeService.loadRecipe("B")).thenReturn(null);

        // Should fail on first missing dependency (B is checked first)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAndSort("A");
        });

        assertTrue(ex.getMessage().contains("Recipe not found"));
        assertTrue(ex.getMessage().contains("B"));
    }

    // ========== Real-World Scenario Tests ==========

    @Test
    void testMavenLatestDependency() {
        // Real scenario: maven-latest depends on maven-3.9
        Recipe mavenLatest = createRecipe("maven-latest", "1.0.0", Arrays.asList("maven-3.9"));
        Recipe maven39 = createRecipe("maven-3.9", "3.9.0");

        when(recipeService.loadRecipe("maven-latest")).thenReturn(mavenLatest);
        when(recipeService.loadRecipe("maven-3.9")).thenReturn(maven39);

        List<Recipe> result = resolver.resolveAndSort("maven-latest");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("maven-3.9", result.get(0).getName());
        assertEquals("maven-latest", result.get(1).getName());
    }

    @Test
    void testComplexJavaDevEnvironment() {
        // Java dev environment: IDE depends on JDK and Maven, Maven depends on JDK
        //      IDE
        //     /   \
        //   JDK  Maven
        //         |
        //        JDK
        Recipe ide = createRecipe("eclipse", "1.0.0", Arrays.asList("jdk-21", "maven-3.9"));
        Recipe jdk = createRecipe("jdk-21", "21.0.0");
        Recipe maven = createRecipe("maven-3.9", "3.9.0", Arrays.asList("jdk-21"));

        when(recipeService.loadRecipe("eclipse")).thenReturn(ide);
        when(recipeService.loadRecipe("jdk-21")).thenReturn(jdk);
        when(recipeService.loadRecipe("maven-3.9")).thenReturn(maven);

        List<Recipe> result = resolver.resolveAndSort("eclipse");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("jdk-21", result.get(0).getName(), "JDK should be first");
        assertEquals("maven-3.9", result.get(1).getName(), "Maven should be second");
        assertEquals("eclipse", result.get(2).getName(), "Eclipse should be last");
    }

    // ========== Format Installation Plan Tests ==========

    @Test
    void testFormatInstallationPlanEmpty() {
        String plan = resolver.formatInstallationPlan(Collections.emptyList());
        
        assertNotNull(plan);
        assertTrue(plan.contains("Installation Plan"));
    }

    @Test
    void testFormatInstallationPlanSingleRecipe() {
        Recipe recipe = createRecipe("standalone", "1.0.0");
        List<Recipe> recipes = Arrays.asList(recipe);
        
        String plan = resolver.formatInstallationPlan(recipes);
        
        assertNotNull(plan);
        assertTrue(plan.contains("Installation Plan"));
        assertTrue(plan.contains("standalone"));
        assertTrue(plan.contains("1."));
    }

    @Test
    void testFormatInstallationPlanMultipleRecipes() {
        Recipe recipeA = createRecipe("A", "1.0.0", Arrays.asList("B"));
        Recipe recipeB = createRecipe("B", "1.0.0");
        List<Recipe> recipes = Arrays.asList(recipeB, recipeA);
        
        String plan = resolver.formatInstallationPlan(recipes);
        
        assertNotNull(plan);
        assertTrue(plan.contains("Installation Plan"));
        assertTrue(plan.contains("1. "));
        assertTrue(plan.contains("2. "));
        assertTrue(plan.contains("B"));
        assertTrue(plan.contains("A"));
    }

    @Test
    void testFormatInstallationPlanShowsDependencyIndicators() {
        Recipe withDeps = createRecipe("with-deps", "1.0.0", Arrays.asList("dependency"));
        Recipe noDeps = createRecipe("no-deps", "1.0.0");
        List<Recipe> recipes = Arrays.asList(noDeps, withDeps);
        
        String plan = resolver.formatInstallationPlan(recipes);
        
        assertNotNull(plan);
        // Recipes with dependencies should show → indicator
        // Recipes without dependencies should show ✓ indicator
        assertTrue(plan.contains("→") || plan.contains("✓"));
    }

    // ========== Helper Methods ==========

    private Recipe createRecipe(String name, String version) {
        return createRecipe(name, version, null);
    }

    private Recipe createRecipe(String name, String version, List<String> dependencies) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setVersion(version);
        recipe.setDescription("Test recipe for " + name);
        if (dependencies != null) {
            recipe.setDependencies(dependencies);
        }
        return recipe;
    }

    private int findIndex(List<Recipe> recipes, String name) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
