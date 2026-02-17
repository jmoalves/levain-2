package com.github.jmoalves.levain.service.backup;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Result of a backup operation.
 * Contains information about the backup location, timestamp, and status.
 */
public record BackupResult(
    String packageName,
    String timestamp,
    Path backupPath,
    boolean success,
    String error,
    long backupSize
) {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    /**
     * Create a successful backup result.
     */
    public static BackupResult success(String packageName, Path backupPath, 
                                      LocalDateTime timestamp, long backupSize) {
        return new BackupResult(
            packageName,
            timestamp.format(TIMESTAMP_FORMAT),
            backupPath,
            true,
            null,
            backupSize
        );
    }
    
    /**
     * Create a failed backup result.
     */
    public static BackupResult failure(String packageName, String error) {
        return new BackupResult(
            packageName,
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            null,
            false,
            error,
            0
        );
    }
    
    /**
     * Check if this backup can be restored.
     */
    public boolean canRestore() {
        return success && backupPath != null;
    }
    
    @Override
    public String toString() {
        if (!success) {
            return String.format("BackupResult[package=%s, failed: %s]", 
                packageName, error);
        }
        return String.format("BackupResult[package=%s, backup=%s, timestamp=%s, size=%d]",
            packageName, backupPath, timestamp, backupSize);
    }
}