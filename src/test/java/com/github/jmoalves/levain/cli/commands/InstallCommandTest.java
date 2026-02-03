package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.service.AlreadyInstalledException;
import com.github.jmoalves.levain.service.InstallService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class InstallCommandTest {

    @Mock
    private InstallService installService;

    @Test
    void testInstallSinglePackageSuccess() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doNothing().when(installService).install("jdk-21", false);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(0, exitCode);
        verify(installService).install("jdk-21", false);
    }

    @Test
    void testInstallMultiplePackagesSuccess() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doNothing().when(installService).install(anyString(), eq(false));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "maven", "git");

        assertEquals(0, exitCode);
        verify(installService).install("jdk-21", false);
        verify(installService).install("maven", false);
        verify(installService).install("git", false);
    }

    @Test
    void testInstallWithForceFlag() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doNothing().when(installService).install("jdk-21", true);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--force", "jdk-21");

        assertEquals(0, exitCode);
        verify(installService).install("jdk-21", true);
    }

    @Test
    void testInstallAlreadyInstalled() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doThrow(new AlreadyInstalledException("jdk-21")).when(installService).install("jdk-21", false);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(0, exitCode); // Should succeed even if already installed
        verify(installService).install("jdk-21", false);
    }

    @Test
    void testInstallFailure() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doThrow(new RuntimeException("Package not found")).when(installService).install("invalid-package", false);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("invalid-package");

        assertEquals(1, exitCode);
        verify(installService).install("invalid-package", false);
    }

    @Test
    void testInstallWithNoPackages() {
        InstallCommand command = new InstallCommand(installService);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(1, exitCode); // Command returns 1 when no packages provided
        verifyNoInteractions(installService);
    }

    @Test
    void testInstallMultipleWithOneFailure() throws Exception {
        InstallCommand command = new InstallCommand(installService);

        doNothing().when(installService).install("jdk-21", false);
        doThrow(new RuntimeException("Package not found")).when(installService).install("invalid-package", false);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "invalid-package", "maven");

        assertEquals(1, exitCode);
        verify(installService).install("jdk-21", false);
        verify(installService).install("invalid-package", false);
        verify(installService, never()).install("maven", false); // Should stop after first failure
    }
}
