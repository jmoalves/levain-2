package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to disable backups.
 */
@Command(name = "disable", description = "Disable backups", mixinStandardHelpOptions = true)
public class BackupDisableCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupDisableCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final Config config;

    @Inject
    public BackupDisableCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Disabling backups");
            config.setBackupEnabled(false);
            config.save();
            console.info("✓ Backups disabled");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to disable backups", e);
            console.error("✗ Failed to disable backups. See logs for details.");
            return 1;
        }
    }
}
