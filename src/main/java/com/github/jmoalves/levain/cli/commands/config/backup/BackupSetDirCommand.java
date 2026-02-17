package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcommand to set backup directory.
 */
@Command(name = "set-dir", description = "Set backup directory", mixinStandardHelpOptions = true)
public class BackupSetDirCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupSetDirCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(index = "0", description = "Backup directory path")
    private String backupDir;

    private final Config config;

    @Inject
    public BackupSetDirCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Setting backup directory to: {}", backupDir);
            config.setBackupDir(backupDir);
            config.save();
            console.info("✓ Backup directory set to: {}", backupDir);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to set backup directory", e);
            console.error("✗ Failed to set backup directory. See logs for details.");
            return 1;
        }
    }
}
