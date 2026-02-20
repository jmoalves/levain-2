package com.github.jmoalves.levain.cli.commands.config.shell;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to disable shell update checks.
 */
@Command(name = "disable-updates", description = "Disable update checks for shell", mixinStandardHelpOptions = true)
public class ShellUpdateDisableCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellUpdateDisableCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public ShellUpdateDisableCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Disabling shell update checks");
            config.setShellCheckForUpdate(false);
            config.save();
            console.info("✓ Shell update checks disabled");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to disable shell update checks", e);
            console.error("✗ Failed to disable shell update checks. See logs for details.");
            return 1;
        }
    }
}
