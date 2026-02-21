package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.InstallService;
import com.github.jmoalves.levain.service.ShellService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ShellCommandTest {

    @Mock
    private ShellService shellService;

    @Mock
    private InstallService installService;

    @Mock
    private Config config;

    @Test
    void testShellCommandWithPackages() throws Exception {
        ShellCommand command = new ShellCommand(shellService, installService, config);

        when(config.isShellCheckForUpdate()).thenReturn(false);
        doNothing().when(shellService).openShell(List.of("jdk-21", "maven"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "maven");

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of("jdk-21", "maven"));
        verify(installService, never()).findUpdates(List.of("jdk-21", "maven"));
    }

    @Test
    void testShellCommandWithoutPackages() throws Exception {
        ShellCommand command = new ShellCommand(shellService, installService, config);

        when(config.isShellCheckForUpdate()).thenReturn(false);
        doNothing().when(shellService).openShell(List.of());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of());
        verify(installService, never()).findUpdates(List.of());
    }

    @Test
    void testShellCommandFailure() throws Exception {
        ShellCommand command = new ShellCommand(shellService, installService, config);

        when(config.isShellCheckForUpdate()).thenReturn(false);
        doThrow(new RuntimeException("Shell error")).when(shellService).openShell(any());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(1, exitCode);
        verify(shellService).openShell(List.of("jdk-21"));
        verify(installService, never()).findUpdates(List.of("jdk-21"));
    }

    @Test
    void testShellCommandWithNoUpdateFlag() throws Exception {
        ShellCommand command = new ShellCommand(shellService, installService, config);

        doNothing().when(shellService).openShell(List.of("jdk-21", "maven"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--noUpdate", "jdk-21", "maven");

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of("jdk-21", "maven"));
        // Should NOT call findUpdates when --noUpdate is passed
        verify(installService, never()).findUpdates(List.of("jdk-21", "maven"));
    }

    @Test
    void testShellCommandWithUpdateCheckEnabled() throws Exception {
        ShellCommand command = new ShellCommand(shellService, installService, config);

        when(config.isShellCheckForUpdate()).thenReturn(true);
        when(installService.findUpdates(List.of("jdk-21"))).thenReturn(List.of());
        doNothing().when(shellService).openShell(List.of("jdk-21"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(0, exitCode);
        verify(shellService).openShell(List.of("jdk-21"));
        // Should call findUpdates when update check is enabled
        verify(installService).findUpdates(List.of("jdk-21"));
    }
}
