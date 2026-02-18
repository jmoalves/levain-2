package com.github.jmoalves.levain.service.clean;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.rollback.RollbackService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing cleanup operations on backups.
 * 
 * Provides functionality to:
 * - List backups that would be cleaned
 * - Dry-run cleanup to preview what would be deleted
 * - Execute cleanup with various filtering options
 */
@ApplicationScoped
public class CleanService {
    private static final Logger logger = LoggerFactory.getLogger(CleanService.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");
    
    private final Config config;
    private final RollbackService rollbackService;
    
    @Inject
    public CleanService(Config config, RollbackService rollbackService) {
        this.config = config;
        this.rollbackService = rollbackService;
    }
    
    /**
     * Preview what would be cleaned without actually deleting anything.
     * 
     * @param packageName Optional package name (null = all packages)
     * @param olderThanDays Delete backups older than this many days (null = use config max-age)
     * @param keepCount Keep this many most recent backups (null = use config keep-count)
     * @return CleanupResult with items to delete
     */
    public CleanupResult previewCleanup(String packageName, Integer olderThanDays, Integer keepCount) {
        Integer maxAgeDays = olderThanDays != null ? olderThanDays : config.getBackupMaxAgeDays();
        Integer countToKeep = keepCount != null ? keepCount : config.getBackupKeepCount();
        
        return calculateBackupsToDelete(packageName, maxAgeDays, countToKeep);
    }
    
    /**
     * Execute cleanup according to policy.
     * 
     * @param packageName Optional package name (null = all packages)
     * @param olderThanDays Delete backups older than this many days (null = use config max-age)
     * @param keepCount Keep this many most recent backups (null = use config keep-count)
     * @return CleanupResult with items deleted
     */
    public CleanupResult executeCleanup(String packageName, Integer olderThanDays, Integer keepCount) {
        CleanupResult preview = previewCleanup(packageName, olderThanDays, keepCount);
        
        // Execute deletion
        preview.toDelete.forEach(item -> {
            try {
                rollbackService.deleteBackup(item.packageName, item.backup);
                logger.info("Deleted backup: {} from package {}", 
                    item.backup.timestampStr(), item.packageName);
            } catch (Exception e) {
                logger.error("Failed to delete backup {}: {}", 
                    item.backup.timestampStr(), e.getMessage());
                item.deleteFailed = true;
            }
        });
        
        return preview;
    }
    
    /**
     * Calculate which backups would be deleted.
     */
    private CleanupResult calculateBackupsToDelete(String packageName, int maxAgeDays, int keepCount) {
        CleanupResult result = new CleanupResult();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxAgeDays);
        
        Map<String, List<RollbackService.BackupInfo>> allBackups = 
            rollbackService.listAllBackups();
        
        allBackups.forEach((pkg, backups) -> {
            if (packageName != null && !pkg.equals(packageName)) {
                return; // Skip if filtering by package
            }
            
            result.totalBackups.put(pkg, backups.size());
            
            // Find backups to delete
            for (int i = 0; i < backups.size(); i++) {
                RollbackService.BackupInfo backup = backups.get(i);
                String reason = null;
                
                // Check if too old
                if (backup.timestamp().isBefore(cutoffDate)) {
                    reason = String.format("older than %d days", maxAgeDays);
                }
                
                // Check if exceeds keep count
                if (i >= keepCount) {
                    if (reason != null) {
                        reason += " AND exceeds keep-count";
                    } else {
                        reason = String.format("exceeds keep-count of %d", keepCount);
                    }
                }
                
                if (reason != null) {
                    result.toDelete.add(new CleanupItem(pkg, backup, reason));
                }
            }
        });
        
        return result;
    }
    
    /**
     * Result of cleanup operation.
     */
    public static class CleanupResult {
        public List<CleanupItem> toDelete = new ArrayList<>();
        public Map<String, Integer> totalBackups = new HashMap<>();
        
        /**
         * Calculate statistics.
         */
        public long getTotalSizeToDelete() {
            return toDelete.stream()
                .filter(item -> !item.deleteFailed)
                .mapToLong(item -> item.backup.size())
                .sum();
        }
        
        public int getSuccessfulDeletions() {
            return (int) toDelete.stream()
                .filter(item -> !item.deleteFailed)
                .count();
        }
        
        public int getFailedDeletions() {
            return (int) toDelete.stream()
                .filter(item -> item.deleteFailed)
                .count();
        }
    }
    
    /**
     * An individual backup marked for deletion.
     */
    public static class CleanupItem {
        public String packageName;
        public RollbackService.BackupInfo backup;
        public String deleteReason;
        public boolean deleteFailed = false;
        
        public CleanupItem(String packageName, RollbackService.BackupInfo backup, String deleteReason) {
            this.packageName = packageName;
            this.backup = backup;
            this.deleteReason = deleteReason;
        }
    }
}
