package com.github.jmoalves.levain.cli.commands.config.shell;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to enable shell update checks.
 */
@Command(name = "enable-updates", description = "Enable update checks for shell", mixinStandardHelpOptions = true)
public class ShellUpdateEnableCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellUpdateEnableCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public ShellUpdateEnableCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Enabling shell update checks");
            config.setShellCheckForUpdate(true);
            config.save();
            console.info("✓ Shell update checks enabled");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to enable shell update checks", e);
            console.error("✗ Failed to enable shell update checks. See logs for details.");
            return 1;
        }
    }
}
