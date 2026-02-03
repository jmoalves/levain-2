package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.service.ShellService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ShellCommandTest {

    @Mock
    private ShellService shellService;

    @Test
    void testShellCommandWithPackages() throws Exception {
        ShellCommand command = new ShellCommand(shellService);

        doNothing().when(shellService).openShell(List.of("jdk-21", "maven"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "maven");

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of("jdk-21", "maven"));
    }

    @Test
    void testShellCommandWithoutPackages() throws Exception {
        ShellCommand command = new ShellCommand(shellService);

        doNothing().when(shellService).openShell(List.of());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of());
    }

    @Test
    void testShellCommandFailure() throws Exception {
        ShellCommand command = new ShellCommand(shellService);

        doThrow(new RuntimeException("Shell error")).when(shellService).openShell(any());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(1, exitCode);
        verify(shellService).openShell(List.of("jdk-21"));
    }
}
