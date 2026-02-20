package com.github.jmoalves.levain.cli.commands.config.shell;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

/**
 * Subcommand to manage shell configuration.
 */
@Command(name = "shell", description = "Manage shell configuration", mixinStandardHelpOptions = true, subcommands = {
        ShellShowCommand.class,
        ShellUpdateEnableCommand.class,
        ShellUpdateDisableCommand.class
})
public class ShellCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config shell --help' to see available subcommands");
        return 0;
    }
}
