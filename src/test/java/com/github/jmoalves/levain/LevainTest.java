package com.github.jmoalves.levain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import com.github.jmoalves.levain.cli.LevainCommand;
import com.github.jmoalves.levain.config.Config;

import org.jboss.weld.inject.WeldInstance;

import java.lang.reflect.Field;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

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
    @DisplayName("Should return unknown when version stream missing")
    void testGetVersionWithNullStream() {
        String version = Levain.getVersion(() -> null);
        assertEquals("unknown", version);
    }

    @Test
    @DisplayName("Should return unknown when version stream fails")
    void testGetVersionWithFailingStream() {
        String version = Levain.getVersion(() -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        });
        assertEquals("unknown", version);
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
    @DisplayName("Should apply overrides during execution")
    void testExecuteCommandAppliesOverrides() throws Exception {
        LevainCommand command = new LevainCommand();
        Config config = Mockito.mock(Config.class);
        Field configField = LevainCommand.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(command, config);

        org.jboss.weld.environment.se.WeldContainer container = Mockito.mock(org.jboss.weld.environment.se.WeldContainer.class);
        @SuppressWarnings("unchecked")
        WeldInstance<LevainCommand> instance = Mockito.mock(WeldInstance.class);
        Mockito.when(container.select(LevainCommand.class)).thenReturn(instance);
        Mockito.when(instance.get()).thenReturn(command);

        var method = Levain.class.getDeclaredMethod("executeCommand",
                org.jboss.weld.environment.se.WeldContainer.class, String[].class);
        method.setAccessible(true);
        Object result = method.invoke(null, container,
                new String[] { "--levainHome", "/tmp/levain-home", "--levainCache", "/tmp/levain-cache" });

        assertEquals(0, result);
        Mockito.verify(config).setLevainHome("/tmp/levain-home");
        Mockito.verify(config).setCacheDir("/tmp/levain-cache");
    }

    @Test
    @DisplayName("Should return exit code from run")
    void testRunReturnsExitCode() {
        LevainCommand command = new LevainCommand();
        org.jboss.weld.environment.se.WeldContainer container = Mockito.mock(org.jboss.weld.environment.se.WeldContainer.class);
        @SuppressWarnings("unchecked")
        WeldInstance<LevainCommand> instance = Mockito.mock(WeldInstance.class);
        Mockito.when(container.select(LevainCommand.class)).thenReturn(instance);
        Mockito.when(instance.get()).thenReturn(command);

        Supplier<org.jboss.weld.environment.se.WeldContainer> supplier = () -> container;

        int result = Levain.run(new String[0], supplier);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should return 1 when run throws")
    void testRunHandlesException() {
        Supplier<org.jboss.weld.environment.se.WeldContainer> supplier = () -> {
            throw new RuntimeException("boom");
        };

        int result = Levain.run(new String[0], supplier);

        assertEquals(1, result);
    }

    @Test
    @DisplayName("Should run with default container")
    void testRunDefault() {
        int result = Levain.run(new String[0]);
        assertEquals(0, result);
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
