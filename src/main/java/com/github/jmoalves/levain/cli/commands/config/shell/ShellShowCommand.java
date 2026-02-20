package com.github.jmoalves.levain.cli.commands.config.shell;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to show current shell configuration.
 */
@Command(name = "show", description = "Show current shell configuration", mixinStandardHelpOptions = true)
public class ShellShowCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellShowCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public ShellShowCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Showing shell configuration");

            console.info("Shell Configuration:");
            console.info("  Shell path: {}", config.getShellPath());
            console.info("  Check for updates: {}", config.isShellCheckForUpdate());

            return 0;
        } catch (Exception e) {
            logger.error("Failed to show shell configuration", e);
            console.error("✗ Failed to show shell configuration. See logs for details.");
            return 1;
        }
    }
}
