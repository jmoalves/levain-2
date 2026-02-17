package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcommand to set backup max age.
 */
@Command(name = "set-max-age", description = "Set maximum age of backups in days", mixinStandardHelpOptions = true)
public class BackupSetMaxAgeCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupSetMaxAgeCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(index = "0", description = "Maximum age in days")
    private int maxAgeDays;

    private final Config config;

    @Inject
    public BackupSetMaxAgeCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            if (maxAgeDays < 1) {
                console.error("✗ Max age must be at least 1 day");
                return 1;
            }
            
            logger.debug("Setting backup max age to: {} days", maxAgeDays);
            config.setBackupMaxAgeDays(maxAgeDays);
            config.save();
            console.info("✓ Backup max age set to: {} days", maxAgeDays);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to set backup max age", e);
            console.error("✗ Failed to set backup max age. See logs for details.");
            return 1;
        }
    }
}
