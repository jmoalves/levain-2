package com.github.jmoalves.levain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Levain Main Entry Point Tests")
class LevainTest {

    @Test
    @DisplayName("Should get version string")
    void testGetVersionFromPomProperties() throws Exception {
        // This test verifies the getVersion method exists
        // The version is loaded from
        // /META-INF/maven/com.github.jmoalves/levain/pom.properties
        // Execute getVersion via reflection to increase coverage
        var method = Levain.class.getDeclaredMethod("getVersion");
        method.setAccessible(true);
        Object result = method.invoke(null);
        assertNotNull(result, "Version should not be null");
        assertFalse(result.toString().isBlank(), "Version should not be blank");
    }

    @Test
    @DisplayName("Should have main method")
    void testMainMethodExists() {
        // Verify the main method is present and callable
        assertDoesNotThrow(() -> {
            Levain.class.getDeclaredMethod("main", String[].class);
        }, "main method should exist");
    }

    @Test
    @DisplayName("Should initialize CDI container")
    void testCdiContainerInitialization() {
        // Verify that private methods exist for CDI initialization
        assertDoesNotThrow(() -> {
            var method = Levain.class.getDeclaredMethod("initializeCdiContainer");
            method.setAccessible(true);
            Object container = method.invoke(null);
            assertNotNull(container, "CDI container should be created");
            if (container instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }, "initializeCdiContainer should create a container");
    }

    @Test
    @DisplayName("Should execute command")
    void testCommandExecution() {
        // Verify that private method for command execution exists
        assertDoesNotThrow(() -> {
            Levain.class.getDeclaredMethod("executeCommand", org.jboss.weld.environment.se.WeldContainer.class,
                    String[].class);
        }, "executeCommand method should exist");
    }

    @Test
    @DisplayName("Should have version getter method")
    void testVersionGetter() {
        // Verify that getVersion method exists
        assertDoesNotThrow(() -> {
            Levain.class.getDeclaredMethod("getVersion");
        }, "getVersion method should exist");
    }

    @Test
    @DisplayName("Levain class should be instantiable")
    void testLevainInstantiation() {
        assertDoesNotThrow(() -> {
            new Levain();
        }, "Levain should be instantiable");
    }
}
