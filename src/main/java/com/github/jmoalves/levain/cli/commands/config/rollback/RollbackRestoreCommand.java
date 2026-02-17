package com.github.jmoalves.levain.cli.commands.config.rollback;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.backup.BackupException;
import com.github.jmoalves.levain.service.rollback.RollbackService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Restore a package from a specific backup.
 * Usage: levain config rollback restore <package-name> [timestamp]
 * 
 * If timestamp is not provided, restores from the most recent backup.
 */
@Command(
    name = "restore",
    description = "Restore a package from a backup"
)
public class RollbackRestoreCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RollbackRestoreCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");
    
    @Parameters(
        index = "0",
        description = "Package name to restore"
    )
    private String packageName;
    
    @Parameters(
        index = "1",
        arity = "0..1",
        description = "Backup timestamp (yyyyMMdd-HHmmss, optional - uses newest if omitted)"
    )
    private String timestamp;
    
    @Option(
        names = {"-f", "--force"},
        description = "Force restore without confirmation"
    )
    private boolean force = false;
    
    private final RollbackService rollbackService;
    private final Config config;
    
    @Inject
    public RollbackRestoreCommand(RollbackService rollbackService, Config config) {
        this.rollbackService = rollbackService;
        this.config = config;
    }
    
    @Override
    public Integer call() {
        try {
            // Determine which backup to restore
            String backupTimestamp = timestamp;
            if (backupTimestamp == null) {
                backupTimestamp = selectNewestBackup();
                if (backupTimestamp == null) {
                    console.error("✗ No backups found for package: {}", packageName);
                    return 1;
                }
            }
            
            // Determine target directory
            Path targetDir = config.getLevainHome().resolve(packageName);
            
            // Confirm restore if not forced
            if (!force) {
                console.info("");
                console.info("This will restore package '{}' from backup {}",
                    packageName, backupTimestamp);
                if (Files.exists(targetDir)) {
                    console.info("The current installation at {} will be replaced.",
                        targetDir);
                }
                console.info("");
                console.info("Continue? (y/N)");
                
                String response = System.console() != null 
                    ? System.console().readLine().trim().toLowerCase()
                    : "n";
                
                if (!response.equals("y") && !response.equals("yes")) {
                    console.info("Restore cancelled");
                    return 0;
                }
            }
            
            // Perform restore
            console.info("Restoring package '{}' from backup {}...", 
                packageName, backupTimestamp);
            
            rollbackService.restore(packageName, backupTimestamp, targetDir);
            
            console.info("✓ Successfully restored {} to {}",
                packageName, targetDir);
            
            // Optional: run cleanup after successful restore
            if (config.isBackupEnabled()) {
                console.info("Cleaning up old backups...");
                rollbackService.cleanupOldBackups(packageName);
                console.info("✓ Backup cleanup completed");
            }
            
            return 0;
            
        } catch (BackupException e) {
            console.error("✗ Restore failed: {}", e.getMessage());
            logger.debug("Restore error", e);
            return 1;
        } catch (Exception e) {
            console.error("✗ Restore failed: {}", e.getMessage());
            logger.error("Unexpected error during restore", e);
            return 1;
        }
    }
    
    private String selectNewestBackup() {
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups(packageName);
        
        if (backups.isEmpty()) {
            return null;
        }
        
        // The list is already sorted by timestamp (newest first)
        RollbackService.BackupInfo newest = backups.get(0);
        console.info("Found {} backup(s). Using most recent: {}",
            backups.size(), newest.timestampStr());
        
        return newest.timestampStr();
    }
}
