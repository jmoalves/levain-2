package com.github.jmoalves.levain.cli;

import com.github.jmoalves.levain.cli.commands.CloneCommand;
import com.github.jmoalves.levain.cli.commands.InstallCommand;
import com.github.jmoalves.levain.cli.commands.ListCommand;
import com.github.jmoalves.levain.cli.commands.ShellCommand;
import com.github.jmoalves.levain.cli.commands.ConfigCommand;
import com.github.jmoalves.levain.cli.commands.RollbackCommand;
import com.github.jmoalves.levain.config.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * Main command line interface for Levain.
 * Provides subcommands for package management and environment configuration.
 */
@ApplicationScoped
@Command(name = "levain", description = "Something to help you make your software grow", version = "2.0.0-SNAPSHOT", mixinStandardHelpOptions = true, subcommands = {
    ListCommand.class,
    InstallCommand.class,
    ShellCommand.class,
    ConfigCommand.class,
    RollbackCommand.class,
    CloneCommand.class
})
public class LevainCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(LevainCommand.class);

    @Option(names = { "--levainHome" }, description = "Levain home directory")
    private String levainHome;

    @Option(names = { "--levainCache" }, description = "Levain cache directory")
    private String levainCache;

    @Option(names = { "--addRepo" }, description = "Add a recipe repository")
    private String[] addRepo;

    @Option(names = { "--tempRepo" }, description = "Add a temporary recipe repository")
    private String[] tempRepo;

    @Option(names = { "--verbose", "-v" }, description = "Enable verbose output")
    private boolean verbose;

    @Inject
    private Config config;

    public void applyOverrides() {
        if (config == null) {
            return;
        }
        if (levainHome != null && !levainHome.isBlank()) {
            config.setLevainHome(levainHome);
        }
        if (levainCache != null && !levainCache.isBlank()) {
            config.setCacheDir(levainCache);
        }
    }

    @Override
    public Integer call() {
        // When no subcommand is specified, show usage
        logger.info("Levain command called without subcommand - displaying usage");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            CommandLine.usage(this, ps);
            String usage = baos.toString(StandardCharsets.UTF_8);
            // Split by lines and log each line to capture in log files properly
            for (String line : usage.split(System.lineSeparator())) {
                if (!line.isBlank()) {
                    logger.info(line);
                }
            }
        }
        return 0;
    }
}
