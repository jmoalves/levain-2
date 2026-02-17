package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcommand to set backup keep count.
 */
@Command(name = "set-keep-count", description = "Set how many backups to keep per package", mixinStandardHelpOptions = true)
public class BackupSetKeepCountCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackupSetKeepCountCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(index = "0", description = "Number of backups to keep")
    private int keepCount;

    private final Config config;

    @Inject
    public BackupSetKeepCountCommand(Config config) {
        this.config = config;
    }

    @Override
    public Integer call() {
        try {
            if (keepCount < 1) {
                console.error("✗ Keep count must be at least 1");
                return 1;
            }
            
            logger.debug("Setting backup keep count to: {}", keepCount);
            config.setBackupKeepCount(keepCount);
            config.save();
            console.info("✓ Backup keep count set to: {}", keepCount);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to set backup keep count", e);
            console.error("✗ Failed to set backup keep count. See logs for details.");
            return 1;
        }
    }
}
