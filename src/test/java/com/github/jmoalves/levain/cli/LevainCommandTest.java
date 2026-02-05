package com.github.jmoalves.levain.cli;

import com.github.jmoalves.levain.cli.commands.ConfigCommand;
import com.github.jmoalves.levain.cli.commands.InstallCommand;
import com.github.jmoalves.levain.cli.commands.ListCommand;
import com.github.jmoalves.levain.cli.commands.ShellCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevainCommand CLI Tests")
class LevainCommandTest {
    private LevainCommand command;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void setUp() {
        command = new LevainCommand();
        originalOut = System.out;
        capturedOutput = new ByteArrayOutputStream();
    }

    @Test
    @DisplayName("Should execute without subcommand and return 0")
    void testCallWithoutSubcommand() {
        Integer result = command.call();
        assertEquals(0, result, "Should return 0 when called without subcommand");
    }

    @Test
    @DisplayName("Should have ListCommand subcommand")
    void testHasListCommand() {
        assertTrue(isSubcommandDefined(ListCommand.class), "ListCommand should be registered");
    }

    @Test
    @DisplayName("Should have InstallCommand subcommand")
    void testHasInstallCommand() {
        assertTrue(isSubcommandDefined(InstallCommand.class), "InstallCommand should be registered");
    }

    @Test
    @DisplayName("Should have ShellCommand subcommand")
    void testHasShellCommand() {
        assertTrue(isSubcommandDefined(ShellCommand.class), "ShellCommand should be registered");
    }

    @Test
    @DisplayName("Should have ConfigCommand subcommand")
    void testHasConfigCommand() {
        assertTrue(isSubcommandDefined(ConfigCommand.class), "ConfigCommand should be registered");
    }

    @Test
    @DisplayName("Should implement Callable<Integer>")
    void testImplementsCallable() {
        assertTrue(command instanceof java.util.concurrent.Callable,
                "LevainCommand should implement Callable");
    }

    @Test
    @DisplayName("Should have verbose option")
    void testHasVerboseOption() {
        assertDoesNotThrow(() -> {
            LevainCommand.class.getDeclaredField("verbose");
        }, "Should have verbose field");
    }

    @Test
    @DisplayName("Should have levainHome option")
    void testHasLevainHomeOption() {
        assertDoesNotThrow(() -> {
            LevainCommand.class.getDeclaredField("levainHome");
        }, "Should have levainHome field");
    }

    @Test
    @DisplayName("Should have levainCache option")
    void testHasLevainCacheOption() {
        assertDoesNotThrow(() -> {
            LevainCommand.class.getDeclaredField("levainCache");
        }, "Should have levainCache field");
    }

    @Test
    @DisplayName("Should have addRepo option")
    void testHasAddRepoOption() {
        assertDoesNotThrow(() -> {
            LevainCommand.class.getDeclaredField("addRepo");
        }, "Should have addRepo field");
    }

    @Test
    @DisplayName("Should have tempRepo option")
    void testHasTempRepoOption() {
        assertDoesNotThrow(() -> {
            LevainCommand.class.getDeclaredField("tempRepo");
        }, "Should have tempRepo field");
    }

    @Test
    @DisplayName("Should be ApplicationScoped")
    void testIsApplicationScoped() {
        assertTrue(command.getClass().isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class),
                "LevainCommand should be ApplicationScoped");
    }

    @Test
    @DisplayName("Should be a Command")
    void testIsCommand() {
        assertTrue(command.getClass().isAnnotationPresent(CommandLine.Command.class),
                "LevainCommand should be annotated with @Command");
    }

    private boolean isSubcommandDefined(Class<?> subcommand) {
        CommandLine.Command cmd = LevainCommand.class.getAnnotation(CommandLine.Command.class);
        if (cmd != null && cmd.subcommands() != null) {
            for (Class<?> sc : cmd.subcommands()) {
                if (sc.equals(subcommand)) {
                    return true;
                }
            }
        }
        return false;
    }
}
