package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to show current backup configuration.
 */
@Command(name = "show", description = "Show current backup configuration", mixinStandardHelpOptions = true)
public class BackupShowCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupShowCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public BackupShowCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Showing backup configuration");
            
            console.info("Backup Configuration:");
            console.info("  Enabled: {}", config.isBackupEnabled());
            console.info("  Directory: {}", config.getBackupDir());
            console.info("  Keep count: {} backups", config.getBackupKeepCount());
            console.info("  Max age: {} days", config.getBackupMaxAgeDays());
            
            return 0;
        } catch (Exception e) {
            logger.error("Failed to show backup configuration", e);
            console.error("✗ Failed to show backup configuration. See logs for details.");
            return 1;
        }
    }
}
