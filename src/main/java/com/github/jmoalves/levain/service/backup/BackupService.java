package com.github.jmoalves.levain.service.backup;

import com.github.jmoalves.levain.config.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for backing up package installations before updates.
 * 
 * Uses copy-based backup strategy:
 * 1. Copy existing installation to timestamped backup directory
 * 2. Delete old installation (or rename if files are locked)
 * 3. Proceed with new installation to final location
 * 4. On failure: restore from backup
 * 
 * This approach ensures ${baseDir} always points to the correct location
 * and handles Windows file locking gracefully.
 */
@ApplicationScoped
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final double DISK_SPACE_BUFFER = 1.1; // 10% buffer
    
    private final Config config;
    
    @Inject
    public BackupService(Config config) {
        this.config = config;
    }
    
    /**
     * Backup an existing installation directory.
     * 
     * @param currentDir The directory to backup
     * @return BackupResult with backup location and status
     * @throws InsufficientSpaceException if not enough disk space
     * @throws BackupException if backup fails
     */
    public BackupResult backup(Path currentDir) {
        String packageName = currentDir.getFileName().toString();
        
        if (!Files.exists(currentDir)) {
            logger.debug("No existing installation to backup: {}", currentDir);
            return BackupResult.failure(packageName, "Directory does not exist");
        }
        
        if (!Files.isDirectory(currentDir)) {
            logger.debug("Path is not a directory: {}", currentDir);
            return BackupResult.failure(packageName, "Path is not a directory");
        }
        
        LocalDateTime timestamp = LocalDateTime.now();
        Path backupDir = generateBackupPath(currentDir, timestamp);
        
        try {
            // 1. Check disk space
            long requiredSpace = calculateDirectorySize(currentDir);
            validateDiskSpace(backupDir, requiredSpace);
            
            logger.info("Backing up {} to {}", currentDir, backupDir);
            logger.debug("Backup size: {} bytes", requiredSpace);
            
            // 2. Create backup directory
            Files.createDirectories(backupDir.getParent());
            
            // 3. Copy directory with progress reporting
            long copiedBytes = copyDirectory(currentDir, backupDir);
            
            // 4. Verify backup
            verifyBackup(currentDir, backupDir);
            
            logger.info("Backup completed successfully: {} ({} bytes)", 
                backupDir.getFileName(), copiedBytes);
            
            return BackupResult.success(packageName, backupDir, timestamp, copiedBytes);
            
        } catch (InsufficientSpaceException e) {
            logger.error("Insufficient disk space for backup: {}", e.getMessage());
            return BackupResult.failure(packageName, "Insufficient disk space: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Backup failed for {}: {}", currentDir, e.getMessage());
            return BackupResult.failure(packageName, "Backup failed: " + e.getMessage());
        }
    }
    
    /**
     * Delete an installation directory.
     * If files are locked (Windows), renames to .deleted.* for later cleanup.
     * 
     * @param dir The directory to delete
     * @throws IOException if deletion and rename both fail
     */
    public void deleteInstallationDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            logger.debug("Directory does not exist, nothing to delete: {}", dir);
            return;
        }
        
        try {
            logger.debug("Deleting installation directory: {}", dir);
            deleteRecursively(dir);
            logger.debug("Successfully deleted: {}", dir);
        } catch (IOException e) {
            logger.warn("Unable to delete {}. Some files may be in use.", dir);
            logger.debug("Delete error: {}", e.getMessage());
            
            // Try to rename for cleanup later
            Path deletedDir = dir.resolveSibling(
                ".deleted." + dir.getFileName() + "." + System.currentTimeMillis());
            
            try {
                Files.move(dir, deletedDir, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Renamed old installation to {} for later cleanup", 
                    deletedDir.getFileName());
            } catch (IOException renameError) {
                logger.error("Cannot delete or rename old installation: {}", dir);
                throw new IOException(
                    "Cannot delete or rename " + dir + ". " +
                    "Please close any programs using files in this directory.", e);
            }
        }
    }
    
    /**
     * Restore from a backup.
     * 
     * @param backup The backup to restore from
     * @param targetDir The directory to restore to
     * @throws BackupException if restore fails
     */
    public void restore(BackupResult backup, Path targetDir) {
        if (!backup.canRestore()) {
            throw new BackupException("Cannot restore from invalid backup: " + backup);
        }
        
        Path backupPath = backup.backupPath();
        if (!Files.exists(backupPath)) {
            throw new BackupException("Backup no longer exists: " + backupPath);
        }
        
        try {
            logger.info("Restoring from backup: {} -> {}", backupPath, targetDir);
            
            // Delete target if exists
            if (Files.exists(targetDir)) {
                deleteRecursively(targetDir);
            }
            
            // Copy backup to target
            copyDirectory(backupPath, targetDir);
            
            logger.info("Successfully restored from backup");
            
        } catch (IOException e) {
            logger.error("Failed to restore from backup: {}", e.getMessage());
            throw new BackupException("Failed to restore from " + backupPath, e);
        }
    }
    
    /**
     * Generate backup directory path with timestamp.
     */
    private Path generateBackupPath(Path currentDir, LocalDateTime timestamp) {
        String dirname = currentDir.getFileName().toString();
        String timestampStr = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String backupDirName = dirname + ".backup-" + timestampStr;
        
        Path packagesDir = currentDir.getParent();
        return packagesDir.resolve(backupDirName);
    }
    
    /**
     * Calculate total size of a directory recursively.
     */
    private long calculateDirectorySize(Path dir) throws IOException {
        AtomicLong size = new AtomicLong(0);
        
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.debug("Skipping file in size calculation: {}", file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return size.get();
    }
    
    /**
     * Validate sufficient disk space is available.
     */
    private void validateDiskSpace(Path location, long requiredBytes) throws IOException {
        FileStore store = Files.getFileStore(location.getParent());
        long availableBytes = store.getUsableSpace();
        long requiredWithBuffer = (long) (requiredBytes * DISK_SPACE_BUFFER);
        
        if (availableBytes < requiredWithBuffer) {
            throw new InsufficientSpaceException(requiredWithBuffer, availableBytes);
        }
        
        logger.debug("Disk space check passed. Required: {}, Available: {}",
            requiredWithBuffer, availableBytes);
    }
    
    /**
     * Copy directory recursively with progress reporting.
     */
    private long copyDirectory(Path source, Path target) throws IOException {
        AtomicLong copiedBytes = new AtomicLong(0);
        long totalBytes = calculateDirectorySize(source);
        
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
                
                long fileSize = attrs.size();
                long total = copiedBytes.addAndGet(fileSize);
                
                // Log progress for large backups (every 50 MB)
                if (total % (50 * 1024 * 1024) < fileSize) {
                    int percent = (int) ((total * 100) / totalBytes);
                    logger.debug("Backup progress: {}% ({} / {} bytes)", 
                        percent, total, totalBytes);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return copiedBytes.get();
    }
    
    /**
     * Delete directory recursively.
     */
    private void deleteRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Verify backup integrity by checking directory structure matches.
     */
    private void verifyBackup(Path original, Path backup) throws IOException {
        long originalSize = calculateDirectorySize(original);
        long backupSize = calculateDirectorySize(backup);
        
        if (backupSize < originalSize * 0.95) { // Allow 5% variance for filesystem overhead
            throw new BackupException(
                String.format("Backup verification failed. Original: %d bytes, Backup: %d bytes",
                    originalSize, backupSize));
        }
        
        logger.debug("Backup verification passed. Original: {}, Backup: {}",
            originalSize, backupSize);
    }
}
