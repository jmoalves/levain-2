package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.InstallService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class InstallCommandTest {

    @Mock
    private InstallService installService;

    @Test
    void testInstallSinglePackageSuccess() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        Recipe recipe = new Recipe();
        recipe.setName("jdk-21");
        InstallService.PlanResult result = new InstallService.PlanResult(List.of(recipe), List.of(), List.of());

        doNothing().when(installService).installPlan(List.of(recipe));
        org.mockito.Mockito.when(installService.findUpdates(List.of("jdk-21")))
            .thenReturn(List.of());
        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21"), false, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21")))
            .thenReturn("Installation Plan:\n1.   ✓ jdk-21\n");

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(0, exitCode);
        verify(installService).buildInstallationPlan(List.of("jdk-21"), false, List.of());
        verify(installService).installPlan(List.of(recipe));
    }

    @Test
    void testInstallMultiplePackagesSuccess() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        Recipe recipe1 = new Recipe();
        recipe1.setName("jdk-21");
        Recipe recipe2 = new Recipe();
        recipe2.setName("maven");
        Recipe recipe3 = new Recipe();
        recipe3.setName("git");
        List<Recipe> plan = List.of(recipe1, recipe2, recipe3);
        InstallService.PlanResult result = new InstallService.PlanResult(plan, List.of(), List.of());

        org.mockito.Mockito.when(installService.findUpdates(List.of("jdk-21", "maven", "git")))
            .thenReturn(List.of());
        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21", "maven", "git"), false, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21", "maven", "git")))
            .thenReturn("Installation Plan:\n1.   ✓ jdk-21\n2.   ✓ maven\n3.   ✓ git\n");
        doNothing().when(installService).installPlan(plan);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "maven", "git");

        assertEquals(0, exitCode);
        verify(installService).buildInstallationPlan(List.of("jdk-21", "maven", "git"), false, List.of());
        verify(installService).installPlan(plan);
    }

    @Test
    void testInstallWithForceFlag() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        Recipe recipe = new Recipe();
        recipe.setName("jdk-21");
        InstallService.PlanResult result = new InstallService.PlanResult(List.of(recipe), List.of(), List.of());

        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21"), true, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21")))
            .thenReturn("Installation Plan:\n1.   ✓ jdk-21\n");
        doNothing().when(installService).installPlan(List.of(recipe));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--force", "jdk-21");

        assertEquals(0, exitCode);
        verify(installService).buildInstallationPlan(List.of("jdk-21"), true, List.of());
    }

    @Test
    void testInstallAlreadyInstalled() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        InstallService.PlanResult result = new InstallService.PlanResult(List.of(), List.of(), List.of("jdk-21"));

        org.mockito.Mockito.when(installService.findUpdates(List.of("jdk-21")))
            .thenReturn(List.of());
        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21"), false, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21")))
            .thenReturn("Installation Plan:\n1. * jdk-21 [installed]\n");

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21");

        assertEquals(0, exitCode); // Should succeed even if already installed
        verify(installService).buildInstallationPlan(List.of("jdk-21"), false, List.of());
        verify(installService, never()).installPlan(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testInstallWithNoUpdateSkipsUpdateCheck() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        Recipe recipe = new Recipe();
        recipe.setName("jdk-21");
        InstallService.PlanResult result = new InstallService.PlanResult(List.of(recipe), List.of(), List.of());

        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21"), false, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21")))
            .thenReturn("Installation Plan:\n1.   ✓ jdk-21\n");
        doNothing().when(installService).installPlan(List.of(recipe));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--noUpdate", "jdk-21");

        assertEquals(0, exitCode);
        verify(installService, never()).findUpdates(List.of("jdk-21"));
        verify(installService).buildInstallationPlan(List.of("jdk-21"), false, List.of());
        verify(installService).installPlan(List.of(recipe));
    }

    @Test
    void testInstallFailure() throws Exception {
        InstallCommand command = new InstallCommand(installService);
        InstallService.PlanResult result = new InstallService.PlanResult(List.of(), List.of("invalid-package"), List.of());

        org.mockito.Mockito.when(installService.findUpdates(List.of("invalid-package")))
            .thenReturn(List.of());
        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("invalid-package"), false, List.of()))
            .thenReturn(result);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("invalid-package");

        assertEquals(1, exitCode);
        verify(installService).buildInstallationPlan(List.of("invalid-package"), false, List.of());
        verify(installService, never()).installPlan(org.mockito.ArgumentMatchers.any());
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
        Recipe recipe1 = new Recipe();
        recipe1.setName("jdk-21");
        Recipe recipe2 = new Recipe();
        recipe2.setName("invalid-package");
        Recipe recipe3 = new Recipe();
        recipe3.setName("maven");
        List<Recipe> plan = List.of(recipe1, recipe2, recipe3);
        InstallService.PlanResult result = new InstallService.PlanResult(plan, List.of(), List.of());

        org.mockito.Mockito.when(installService.findUpdates(List.of("jdk-21", "invalid-package", "maven")))
            .thenReturn(List.of());
        org.mockito.Mockito.when(installService.buildInstallationPlan(List.of("jdk-21", "invalid-package", "maven"), false, List.of()))
            .thenReturn(result);
        org.mockito.Mockito.when(installService.formatInstallationPlan(result, List.of("jdk-21", "invalid-package", "maven")))
            .thenReturn("Installation Plan:\n1.   ✓ jdk-21\n2.   ✓ invalid-package\n3.   ✓ maven\n");
        doThrow(new RuntimeException("Package not found")).when(installService).installPlan(plan);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("jdk-21", "invalid-package", "maven");

        assertEquals(1, exitCode);
        verify(installService).buildInstallationPlan(List.of("jdk-21", "invalid-package", "maven"), false, List.of());
        verify(installService).installPlan(plan);
    }
}
