package com.github.jmoalves.levain.service.rollback;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.backup.BackupService;
import com.github.jmoalves.levain.service.backup.BackupException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing rollback operations.
 * 
 * Provides functionality to:
 * - List available backups for a package
 * - Restore a specific backup to its original location
 * - Enforce backup retention policies (keep-count, max-age)
 */
@ApplicationScoped
public class RollbackService {
    private static final Logger logger = LoggerFactory.getLogger(RollbackService.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String BACKUP_SUFFIX = ".backup-";
    
    private final Config config;
    private final BackupService backupService;
    
    @Inject
    public RollbackService(Config config, BackupService backupService) {
        this.config = config;
        this.backupService = backupService;
    }
    
    /**
     * Get all available backups for a specific package.
     * 
     * @param packageName Name of the package
     * @return List of BackupInfo sorted by timestamp (newest first)
     */
    public List<BackupInfo> listBackups(String packageName) {
        Path backupDir = config.getBackupDir();
        
        if (!Files.exists(backupDir)) {
            logger.debug("Backup directory does not exist: {}", backupDir);
            return Collections.emptyList();
        }
        
        try {
            return Files.list(backupDir)
                .filter(Files::isDirectory)
                .map(path -> parseBackupDirectory(path, packageName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(BackupInfo::timestamp).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list backups for {}: {}", packageName, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all available backups regardless of package.
     * 
     * @return Map of package name to list of BackupInfo
     */
    public Map<String, List<BackupInfo>> listAllBackups() {
        Path backupDir = config.getBackupDir();
        
        if (!Files.exists(backupDir)) {
            return Collections.emptyMap();
        }
        
        try {
            return Files.list(backupDir)
                .filter(Files::isDirectory)
                .map(this::parseBackupDirectory)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(
                    BackupInfo::packageName,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> list.stream()
                            .sorted(Comparator.comparing(BackupInfo::timestamp).reversed())
                            .collect(Collectors.toList())
                    )
                ));
        } catch (IOException e) {
            logger.error("Failed to list all backups: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Restore a specific backup to the original package directory.
     * 
     * @param packageName Name of the package to restore
     * @param timestamp Timestamp of the backup to restore (yyyyMMdd-HHmmss format)
     * @param targetDir The directory to restore to (typically the package installation directory)
     * @throws BackupException if backup not found or restore fails
     */
    public void restore(String packageName, String timestamp, Path targetDir) {
        Path backupDir = config.getBackupDir();
        String backupDirName = packageName + BACKUP_SUFFIX + timestamp;
        Path backupPath = backupDir.resolve(backupDirName);
        
        if (!Files.exists(backupPath)) {
            throw new BackupException(
                "Backup not found: " + backupDirName + 
                " (checked in " + backupDir + ")");
        }
        
        logger.info("Restoring backup {} for package {} to {}", 
            timestamp, packageName, targetDir);
        
        try {
            // Delete target directory if it exists
            if (Files.exists(targetDir)) {
                logger.debug("Deleting existing installation: {}", targetDir);
                deleteRecursively(targetDir);
            }
            
            // Restore from backup
            long copiedBytes = copyDirectory(backupPath, targetDir);
            
            logger.info("Successfully restored {} from backup {} ({} bytes)", 
                packageName, timestamp, copiedBytes);
            
        } catch (IOException e) {
            logger.error("Failed to restore backup: {}", e.getMessage());
            throw new BackupException("Failed to restore from backup", e);
        }
    }
    
    /**
     * Clean up old backups based on retention policies.
     * 
     * @param packageName Optional package name to clean (null = all packages)
     */
    public void cleanupOldBackups(String packageName) {
        int keepCount = config.getBackupKeepCount();
        int maxAgeDays = config.getBackupMaxAgeDays();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxAgeDays);
        
        // Get fresh list of all backups (already sorted newest first)
        Map<String, List<BackupInfo>> allBackups = listAllBackups();
        
        allBackups.forEach((pkg, backups) -> {
            if (packageName != null && !pkg.equals(packageName)) {
                return; // Skip if filtering by package
            }
            
            int deleted = 0;
            
            // Mark backups for deletion:
            // - Any older than maxAgeDays
            // - Any beyond the keepCount limit
            for (int i = 0; i < backups.size(); i++) {
                BackupInfo backup = backups.get(i);
                boolean shouldDelete = false;
                String reason = "";
                
                // Check if backup is too old
                if (backup.timestamp().isBefore(cutoffDate)) {
                    shouldDelete = true;
                    reason = String.format("older than %d days", maxAgeDays);
                }
                
                // Check if backup exceeds keep count (backups are newest first, so index >= keepCount means delete)
                if (i >= keepCount) {
                    shouldDelete = true;
                    reason = String.format("exceeds keep-count of %d", keepCount);
                }
                
                if (shouldDelete) {
                    logger.debug("Deleting backup {} {}: {}", 
                        pkg, backup.timestampStr(), reason);
                    deleteBackup(pkg, backup);
                    deleted++;
                }
            }
            
            if (deleted > 0) {
                logger.info("Cleaned up {} backup(s) for {}", deleted, pkg);
            }
        });
    }
    
    /**
     * Delete a specific backup (public for cleanup operations).
     */
    public void deleteBackup(String packageName, BackupInfo backup) {
        Path backupDir = config.getBackupDir();
        Path backupPath = backupDir.resolve(
            packageName + BACKUP_SUFFIX + backup.timestampStr());
        
        try {
            if (Files.exists(backupPath)) {
                logger.info("Deleting old backup: {}", backupPath.getFileName());
                deleteRecursively(backupPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete backup {}: {}", 
                backupPath.getFileName(), e.getMessage());
        }
    }
    
    /**
     * Parse a backup directory name to extract package name and timestamp.
     * Format: packagename.backup-yyyyMMdd-HHmmss[-seqnum]
     * The sequence number suffix (-001, -002, etc) is optional and ignored for sorting.
     */
    private Optional<BackupInfo> parseBackupDirectory(Path backupPath) {
        String dirName = backupPath.getFileName().toString();
        int suffixIndex = dirName.lastIndexOf(BACKUP_SUFFIX);
        
        if (suffixIndex <= 0 || suffixIndex + BACKUP_SUFFIX.length() >= dirName.length()) {
            return Optional.empty();
        }
        
        String packageName = dirName.substring(0, suffixIndex);
        String afterSuffix = dirName.substring(suffixIndex + BACKUP_SUFFIX.length());
        
        // Extract timestamp (first 15 chars: yyyyMMdd-HHmmss)
        if (afterSuffix.length() < 15) {
            return Optional.empty();
        }
        
        String timestampStr = afterSuffix.substring(0, 15);
        
        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
            long size = calculateDirectorySize(backupPath);
            return Optional.of(new BackupInfo(packageName, timestamp, timestampStr, size));
        } catch (Exception e) {
            logger.debug("Failed to parse backup directory {}: {}", dirName, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Parse a backup directory matching a specific package name.
     * Format: packagename.backup-yyyyMMdd-HHmmss[-seqnum]
     */
    private Optional<BackupInfo> parseBackupDirectory(Path backupPath, String packageName) {
        String dirName = backupPath.getFileName().toString();
        String expectedPrefix = packageName + BACKUP_SUFFIX;
        
        if (!dirName.startsWith(expectedPrefix)) {
            return Optional.empty();
        }
        
        String afterPrefix = dirName.substring(expectedPrefix.length());
        
        // Extract timestamp (first 15 chars: yyyyMMdd-HHmmss)
        if (afterPrefix.length() < 15) {
            return Optional.empty();
        }
        
        String timestampStr = afterPrefix.substring(0, 15);
        
        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
            long size = calculateDirectorySize(backupPath);
            return Optional.of(new BackupInfo(packageName, timestamp, timestampStr, size));
        } catch (Exception e) {
            logger.debug("Failed to parse backup directory {}: {}", dirName, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Calculate total size of a directory recursively.
     */
    private long calculateDirectorySize(Path dir) {
        try {
            return Files.walk(dir)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            logger.debug("Failed to calculate directory size for {}: {}", dir, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Copy directory recursively.
     */
    private long copyDirectory(Path source, Path target) throws IOException {
        long totalBytes = 0;
        
        Files.createDirectories(target);
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path path : stream) {
                Path targetPath = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    totalBytes += copyDirectory(path, targetPath);
                } else {
                    Files.copy(path, targetPath, StandardCopyOption.COPY_ATTRIBUTES,
                        StandardCopyOption.REPLACE_EXISTING);
                    totalBytes += Files.size(path);
                }
            }
        }
        
        return totalBytes;
    }
    
    /**
     * Delete directory recursively.
     */
    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Information about a backup.
     */
    public record BackupInfo(
        String packageName,
        LocalDateTime timestamp,
        String timestampStr,
        long size
    ) {}
}
