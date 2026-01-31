package com.github.jmoalves.levain.cli;

import com.github.jmoalves.levain.cli.commands.InstallCommand;
import com.github.jmoalves.levain.cli.commands.ListCommand;
import com.github.jmoalves.levain.cli.commands.ShellCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Main command line interface for Levain.
 * Provides subcommands for package management and environment configuration.
 */
@Command(
    name = "levain",
    description = "Something to help you make your software grow",
    version = "2.0.0-SNAPSHOT",
    mixinStandardHelpOptions = true,
    subcommands = {
        ListCommand.class,
        InstallCommand.class,
        ShellCommand.class
    }
)
public class LevainCommand implements Callable<Integer> {

    @Option(names = {"--levainHome"}, description = "Levain home directory")
    private String levainHome;

    @Option(names = {"--levainCache"}, description = "Levain cache directory")
    private String levainCache;

    @Option(names = {"--addRepo"}, description = "Add a recipe repository")
    private String[] addRepo;

    @Option(names = {"--tempRepo"}, description = "Add a temporary recipe repository")
    private String[] tempRepo;

    @Option(names = {"--verbose", "-v"}, description = "Enable verbose output")
    private boolean verbose;

    @Override
    public Integer call() {
        // When no subcommand is specified, show usage
        CommandLine.usage(this, System.out);
        return 0;
    }
}
