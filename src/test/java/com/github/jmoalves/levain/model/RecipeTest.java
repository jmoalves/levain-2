package com.github.jmoalves.levain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RecipeTest {

    @Test
    void shouldSetAndGetFields() {
        Recipe recipe = new Recipe();
        recipe.setName("jdk-21");
        recipe.setVersion("21.0.5");
        recipe.setDescription("Java 21");
        recipe.setRecipesDir("/tmp/recipes");
        recipe.setCommands(Map.of("install", List.of("echo install")));
        recipe.setDependencies(List.of("levain"));

        assertEquals("jdk-21", recipe.getName());
        assertEquals("21.0.5", recipe.getVersion());
        assertEquals("Java 21", recipe.getDescription());
        assertEquals("/tmp/recipes", recipe.getRecipesDir());
        assertEquals(1, recipe.getCommands().size());
        assertEquals(1, recipe.getDependencies().size());
    }

    @Test
    void shouldProvideToString() {
        Recipe recipe = new Recipe();
        recipe.setName("git");
        recipe.setVersion("2.45.0");

        String output = recipe.toString();
        assertNotNull(output);
        assertTrue(output.contains("git"));
        assertTrue(output.contains("2.45.0"));
    }

    @Test
    void shouldDetectSkipInstallDirWithLevain1Attribute() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("levain.pkg.skipInstallDir", true);

        assertTrue(recipe.shouldSkipInstallDir());
    }

    @Test
    void shouldDetectSkipInstallDirWithLevain2Shorthand() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("skipInstallDir", true);

        assertTrue(recipe.shouldSkipInstallDir());
    }

    @Test
    void shouldDefaultToNotSkippingInstallDir() {
        Recipe recipe = new Recipe();
        assertFalse(recipe.shouldSkipInstallDir());
    }

    @Test
    void shouldHandleStringBooleanValues() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("levain.pkg.skipInstallDir", "true");

        assertTrue(recipe.shouldSkipInstallDir());
    }

    @Test
    void shouldPreferShorthandOverLegacy() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("levain.pkg.skipInstallDir", true);
        recipe.addCustomAttribute("skipInstallDir", false);

        // Shorthand is checked first, so it should return false
        assertFalse(recipe.shouldSkipInstallDir());
    }

    @Test
    void shouldReturnMinVersionWhenPresent() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("levain.minVersion", "2.0.0");

        assertEquals("2.0.0", recipe.getMinVersion());
    }

    @Test
    void shouldReturnNullMinVersionWhenMissing() {
        Recipe recipe = new Recipe();

        assertNull(recipe.getMinVersion());
    }

    @Test
    void shouldReturnNullMinVersionWhenNotString() {
        Recipe recipe = new Recipe();
        recipe.addCustomAttribute("levain.minVersion", 2);

        assertNull(recipe.getMinVersion());
    }

    @Test
    void shouldHandleNullCustomAttributes() {
        Recipe recipe = new Recipe();
        recipe.setCustomAttributes(null);

        recipe.addCustomAttribute("skipInstallDir", true);
        assertTrue(recipe.shouldSkipInstallDir());
    }
}

