package com.github.jmoalves.levain.service;

import jakarta.enterprise.inject.Vetoed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShellService Additional Tests")
class ShellServiceAdditionalTest {
    private String originalOsName;

    @Vetoed
    private static class TestShellService extends ShellService {
        private List<String> capturedCommand;
        private boolean throwInterrupted;

        void setThrowInterrupted(boolean throwInterrupted) {
            this.throwInterrupted = throwInterrupted;
        }

        List<String> getCapturedCommand() {
            return capturedCommand;
        }

        @Override
        protected void runProcess(List<String> command) throws IOException, InterruptedException {
            this.capturedCommand = command;
            if (throwInterrupted) {
                throw new InterruptedException("Interrupted for test");
            }
        }
    }

    @BeforeEach
    void setUp() {
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void tearDown() {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
    }

    @Test
    @DisplayName("Should be ApplicationScoped")
    void testIsApplicationScoped() {
        assertTrue(ShellService.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class),
                "ShellService should be ApplicationScoped");
    }

    @Test
    @DisplayName("Should build Windows shell command")
    void testOpenShellWindowsCommand() throws IOException {
        System.setProperty("os.name", "Windows 11");

        TestShellService service = new TestShellService();
        service.openShell(List.of("jdk-21", "maven"));

        List<String> command = service.getCapturedCommand();
        assertNotNull(command, "Command should be captured");
        assertEquals(List.of(
                "cmd.exe",
                "/k",
                "echo Levain shell initialized with packages: jdk-21, maven"), command);
    }

    @Test
    @DisplayName("Should build Unix shell command")
    void testOpenShellUnixCommand() throws IOException {
        System.setProperty("os.name", "Linux");

        TestShellService service = new TestShellService();
        service.openShell(List.of("jdk-21", "maven"));

        List<String> command = service.getCapturedCommand();
        assertNotNull(command, "Command should be captured");
        assertEquals(List.of(
                "/bin/bash",
                "-c",
                "echo 'Levain shell initialized with packages: jdk-21, maven' && bash"), command);
    }

    @Test
    @DisplayName("Should convert InterruptedException to IOException")
    void testOpenShellInterrupted() {
        System.setProperty("os.name", "Linux");

        TestShellService service = new TestShellService();
        service.setThrowInterrupted(true);

        IOException ex = assertThrows(IOException.class,
                () -> service.openShell(List.of("jdk-21")),
                "Should throw IOException when interrupted");
        assertTrue(ex.getMessage().contains("interrupted"));
    }
}
