package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to enable backups.
 */
@Command(name = "enable", description = "Enable backups", mixinStandardHelpOptions = true)
public class BackupEnableCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupEnableCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public BackupEnableCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Enabling backups");
            config.setBackupEnabled(true);
            config.save();
            console.info("✓ Backups enabled");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to enable backups", e);
            console.error("✗ Failed to enable backups. See logs for details.");
            return 1;
        }
    }
}
