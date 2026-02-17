package com.github.jmoalves.levain.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jmoalves.levain.cli.commands.config.repo.RepoAddCommand;
import com.github.jmoalves.levain.cli.commands.config.repo.RepoCommand;
import com.github.jmoalves.levain.cli.commands.config.repo.RepoListCommand;
import com.github.jmoalves.levain.cli.commands.config.repo.RepoRemoveCommand;
import com.github.jmoalves.levain.model.RepositoryConfig;
import com.github.jmoalves.levain.service.ConfigService;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConfigCommandTest {

    @Mock
    private ConfigService configService;

    @Test
    void testConfigCommandCall() {
        ConfigCommand command = new ConfigCommand();
        assertEquals(0, command.call());
    }

    @Test
    void testRepoCommandCall() {
        RepoCommand command = new RepoCommand();
        assertEquals(0, command.call());
    }

    // AddCommand Tests
    @Test
    void testAddCommandSuccess() throws Exception {
        RepoAddCommand command = new RepoAddCommand(configService);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("https://github.com/user/repo.git", "test-repo");

        assertEquals(0, exitCode);
        verify(configService).addRepository("https://github.com/user/repo.git", "test-repo");
    }

    @Test
    void testAddCommandWithoutName() throws Exception {
        RepoAddCommand command = new RepoAddCommand(configService);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("https://github.com/user/repo.git");

        assertEquals(0, exitCode);
        verify(configService).addRepository("https://github.com/user/repo.git", null);
    }

    @Test
    void testAddCommandFailure() throws Exception {
        RepoAddCommand command = new RepoAddCommand(configService);

        doThrow(new RuntimeException("Invalid URI")).when(configService).addRepository(anyString(), any());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("invalid-uri", "test");

        assertEquals(1, exitCode);
        verify(configService).addRepository("invalid-uri", "test");
    }

    // ListCommand Tests
    @Test
    void testListCommandWithRepositories() throws Exception {
        RepoListCommand command = new RepoListCommand(configService);

        List<RepositoryConfig> repos = new ArrayList<>();
        RepositoryConfig repo1 = new RepositoryConfig();
        repo1.setName("repo1");
        repo1.setUri("https://github.com/user/repo1.git");
        RepositoryConfig repo2 = new RepositoryConfig();
        repo2.setName("repo2");
        repo2.setUri("/path/to/repo2");
        repos.add(repo1);
        repos.add(repo2);

        when(configService.getRepositories()).thenReturn(repos);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(0, exitCode);
        verify(configService).getRepositories();
    }

    @Test
    void testListCommandWithNoRepositories() throws Exception {
        RepoListCommand command = new RepoListCommand(configService);

        when(configService.getRepositories()).thenReturn(new ArrayList<>());

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(0, exitCode);
        verify(configService).getRepositories();
    }

    @Test
    void testListCommandFailure() throws Exception {
        RepoListCommand command = new RepoListCommand(configService);

        when(configService.getRepositories()).thenThrow(new RuntimeException("Config error"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute();

        assertEquals(1, exitCode);
        verify(configService).getRepositories();
    }

    // RemoveCommand Tests
    @Test
    void testRemoveCommandSuccess() throws Exception {
        RepoRemoveCommand command = new RepoRemoveCommand(configService);

        when(configService.removeRepository("repo1")).thenReturn(true);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("repo1");

        assertEquals(0, exitCode);
        verify(configService).removeRepository("repo1");
    }

    @Test
    void testRemoveCommandNotFound() throws Exception {
        RepoRemoveCommand command = new RepoRemoveCommand(configService);

        when(configService.removeRepository("nonexistent")).thenReturn(false);

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("nonexistent");

        assertEquals(1, exitCode);
        verify(configService).removeRepository("nonexistent");
    }

    @Test
    void testRemoveCommandFailure() throws Exception {
        RepoRemoveCommand command = new RepoRemoveCommand(configService);

        when(configService.removeRepository("repo1")).thenThrow(new RuntimeException("Config error"));

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("repo1");

        assertEquals(1, exitCode);
        verify(configService).removeRepository("repo1");
    }
}
