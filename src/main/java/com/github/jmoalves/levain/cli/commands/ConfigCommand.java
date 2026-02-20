package com.github.jmoalves.levain.cli.commands;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.cli.commands.config.backup.BackupCommand;
import com.github.jmoalves.levain.cli.commands.config.repo.RepoCommand;
import com.github.jmoalves.levain.cli.commands.config.shell.ShellCommand;

import picocli.CommandLine.Command;

/**
 * Command to manage Levain configuration.
 */
@Command(name = "config", description = "Manage Levain configuration", mixinStandardHelpOptions = true, subcommands = {
        RepoCommand.class,
    BackupCommand.class,
    ShellCommand.class
})
public class ConfigCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config --help' to see available subcommands");
        return 0;
    }
}
