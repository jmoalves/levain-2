package com.github.jmoalves.levain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecipeTreeTest {
    private Map<String, Recipe> recipeMap;
    private RecipeTree tree;

    @BeforeEach
    void setUp() {
        recipeMap = new HashMap<>();

        // Create levain recipe (no dependencies)
        Recipe levain = new Recipe();
        levain.setName("levain");
        levain.setVersion("1.0.0");
        recipeMap.put("levain", levain);

        // Create jdk-21 recipe (depends on levain implicitly)
        Recipe jdk = new Recipe();
        jdk.setName("jdk-21");
        jdk.setVersion("21.0.0");
        recipeMap.put("jdk-21", jdk);

        // Create maven recipe (depends on jdk-21)
        Recipe maven = new Recipe();
        maven.setName("maven");
        maven.setVersion("3.9.0");
        maven.setDependencies(new ArrayList<>(List.of("jdk-21")));
        recipeMap.put("maven", maven);

        // Create gradle recipe (depends on jdk-21)
        Recipe gradle = new Recipe();
        gradle.setName("gradle");
        gradle.setVersion("8.0.0");
        gradle.setDependencies(new ArrayList<>(List.of("jdk-21")));
        recipeMap.put("gradle", gradle);

        // Create git recipe (depends on levain implicitly)
        Recipe git = new Recipe();
        git.setName("git");
        git.setVersion("2.40.0");
        recipeMap.put("git", git);

        tree = new RecipeTree(recipeMap);
    }

    @Test
    void shouldResolveLevainRecipeDirect() {
        List<Recipe> resolved = tree.resolve("levain");
        assertEquals(1, resolved.size());
        assertEquals("levain", resolved.get(0).getName());
    }

    @Test
    void shouldResolveSimpleRecipeWithImplicitLevainDependency() {
        List<Recipe> resolved = tree.resolve("jdk-21");
        assertEquals(2, resolved.size());
        assertEquals("levain", resolved.get(0).getName());
        assertEquals("jdk-21", resolved.get(1).getName());
    }

    @Test
    void shouldResolveRecipeWithExplicitDependencies() {
        List<Recipe> resolved = tree.resolve("maven");
        assertEquals(3, resolved.size());
        assertEquals("levain", resolved.get(0).getName());
        assertEquals("jdk-21", resolved.get(1).getName());
        assertEquals("maven", resolved.get(2).getName());
    }

    @Test
    void shouldResolveMultipleRecipes() {
        List<Recipe> resolved = tree.resolveAll(List.of("maven", "gradle"));
        assertEquals(4, resolved.size());
        assertEquals("levain", resolved.get(0).getName());
        assertEquals("jdk-21", resolved.get(1).getName());
        List<String> names = resolved.stream().map(Recipe::getName).toList();
        assertTrue(names.contains("maven"));
        assertTrue(names.contains("gradle"));
    }

    @Test
    void shouldDeduplicateDependencies() {
        List<Recipe> resolved = tree.resolveAll(List.of("git", "maven"));
        assertEquals(4, resolved.size());
        List<String> names = resolved.stream().map(Recipe::getName).toList();
        assertEquals(1, names.stream().filter(n -> "levain".equals(n)).count());
        assertEquals(1, names.stream().filter(n -> "jdk-21".equals(n)).count());
    }

    @Test
    void shouldThrowOnMissingRecipe() {
        assertThrows(IllegalArgumentException.class, () -> tree.resolve("nonexistent"),
                "Should throw when recipe not found");
    }

    @Test
    void shouldThrowOnCircularDependency() {
        Recipe a = new Recipe();
        a.setName("a");
        a.setDependencies(new ArrayList<>(List.of("b")));

        Recipe b = new Recipe();
        b.setName("b");
        b.setDependencies(new ArrayList<>(List.of("a")));

        Recipe levain = new Recipe();
        levain.setName("levain");

        Map<String, Recipe> circularMap = new HashMap<>();
        circularMap.put("levain", levain);
        circularMap.put("a", a);
        circularMap.put("b", b);

        RecipeTree circularTree = new RecipeTree(circularMap);
        assertThrows(IllegalArgumentException.class, () -> circularTree.resolve("a"),
                "Should detect circular dependencies");
    }

    @Test
    void shouldGetRecipeByName() {
        assertTrue(tree.getRecipe("jdk-21").isPresent());
        assertEquals("jdk-21", tree.getRecipe("jdk-21").get().getName());
        assertTrue(tree.getRecipe("nonexistent").isEmpty());
    }

    @Test
    void shouldCheckRecipeExists() {
        assertTrue(tree.hasRecipe("maven"));
        assertTrue(!tree.hasRecipe("nonexistent"));
    }

    @Test
    void shouldListAllRecipes() {
        List<String> names = tree.getAvailableRecipeNames();
        assertEquals(5, names.size());
        assertTrue(names.contains("levain"));
        assertTrue(names.contains("jdk-21"));
        assertTrue(names.contains("maven"));
        assertTrue(names.contains("gradle"));
        assertTrue(names.contains("git"));
    }

    @Test
    void shouldFilterRecipes() {
        List<Recipe> filtered = tree.filterRecipes("maven");
        assertEquals(1, filtered.size());
        assertEquals("maven", filtered.get(0).getName());
    }

    @Test
    void shouldFilterRecipesPartialMatch() {
        List<Recipe> filtered = tree.filterRecipes("jdk");
        assertEquals(1, filtered.size());
        assertEquals("jdk-21", filtered.get(0).getName());
    }

    @Test
    void shouldGetDirectDependencies() {
        List<String> deps = tree.getDependencies("maven");
        assertEquals(2, deps.size());
        assertEquals("levain", deps.get(0));
        assertEquals("jdk-21", deps.get(1));
    }

    @Test
    void shouldGetDirectDependenciesForLevain() {
        List<String> deps = tree.getDependencies("levain");
        assertEquals(0, deps.size());
    }
}
