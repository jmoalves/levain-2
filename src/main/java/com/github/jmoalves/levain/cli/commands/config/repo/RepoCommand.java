package com.github.jmoalves.levain.cli.commands.config.repo;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

/**
 * Subcommand to manage repositories.
 */
@Command(name = "repo", description = "Manage repositories", mixinStandardHelpOptions = true, subcommands = {
        RepoAddCommand.class,
        RepoListCommand.class,
        RepoRemoveCommand.class
})
public class RepoCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config repo --help' to see available subcommands");
        return 0;
    }
}
