package com.github.jmoalves.levain.service.rollback;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.backup.BackupException;
import com.github.jmoalves.levain.service.backup.BackupService;

class RollbackServiceTest {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    @TempDir
    private Path tempDir;
    
    private Config config;
    private BackupService backupService;
    private RollbackService rollbackService;
    private Path backupDir;
    
    @BeforeEach
    void setUp() throws IOException {
        config = mock(Config.class);
        backupService = mock(BackupService.class);
        
        backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);
        
        when(config.getBackupDir()).thenReturn(backupDir);
        when(config.getBackupKeepCount()).thenReturn(5);
        when(config.getBackupMaxAgeDays()).thenReturn(30);
        
        rollbackService = new RollbackService(config, backupService);
    }
    
    @Test
    void shouldListBackupsForSpecificPackage() throws IOException {
        // Create some backup directories
        createBackupDirectory("jdk-21", "20260201-100000");
        createBackupDirectory("jdk-21", "20260202-100000");
        createBackupDirectory("git", "20260201-120000");
        
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups("jdk-21");
        
        assertEquals(2, backups.size());
        assertEquals("jdk-21", backups.get(0).packageName());
        assertEquals("jdk-21", backups.get(1).packageName());
        // Should be sorted newest first
        assertEquals("20260202-100000", backups.get(0).timestampStr());
        assertEquals("20260201-100000", backups.get(1).timestampStr());
    }
    
    @Test
    void shouldReturnEmptyListWhenNoBackupsExist() {
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups("nonexistent");
        
        assertTrue(backups.isEmpty());
    }
    
    @Test
    void shouldReturnEmptyListWhenBackupDirDoesNotExist() {
        when(config.getBackupDir()).thenReturn(tempDir.resolve("nonexistent"));
        
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups("jdk-21");
        
        assertTrue(backups.isEmpty());
    }
    
    @Test
    void shouldListAllBackups() throws IOException {
        // Create some backup directories
        createBackupDirectory("jdk-21", "20260201-100000");
        createBackupDirectory("jdk-21", "20260202-100000");
        createBackupDirectory("git", "20260201-120000");
        createBackupDirectory("git", "20260203-090000");
        
        Map<String, List<RollbackService.BackupInfo>> allBackups = 
            rollbackService.listAllBackups();
        
        assertEquals(2, allBackups.size());
        assertEquals(2, allBackups.get("jdk-21").size());
        assertEquals(2, allBackups.get("git").size());
        
        // Each package's list should be sorted newest first
        assertEquals("20260202-100000", allBackups.get("jdk-21").get(0).timestampStr());
        assertEquals("20260203-090000", allBackups.get("git").get(0).timestampStr());
    }
    
    @Test
    void shouldRestoreBackupToTargetDirectory() throws IOException {
        // Create backup and target structure
        Path jdkBackupPath = createBackupDirectory("jdk-21", "20260201-100000");
        createTestFile(jdkBackupPath, "bin/java.exe", "java content");
        createTestFile(jdkBackupPath, "lib/rt.jar", "jar content");
        
        Path targetDir = tempDir.resolve("packages").resolve("jdk-21");
        
        // Perform restore
        rollbackService.restore("jdk-21", "20260201-100000", targetDir);
        
        // Verify files were restored
        assertTrue(Files.exists(targetDir.resolve("bin/java.exe")));
        assertTrue(Files.exists(targetDir.resolve("lib/rt.jar")));
        assertEquals("java content", 
            new String(Files.readAllBytes(targetDir.resolve("bin/java.exe"))));
    }
    
    @Test
    void shouldThrowExceptionWhenBackupNotFound() {
        Path targetDir = tempDir.resolve("packages").resolve("jdk-21");
        
        assertThrows(BackupException.class, () -> 
            rollbackService.restore("jdk-21", "20260201-100000", targetDir));
    }
    
    @Test
    void shouldThrowExceptionWhenBackupDirWasDeleted() throws IOException {
        // Create backup
        createBackupDirectory("jdk-21", "20260201-100000");
        Path backupPath = backupDir.resolve("jdk-21.backup-20260201-100000");
        
        // Delete the backup after creating reference
        Files.walkFileTree(backupPath, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, 
                    java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) 
                    throws IOException {
                if (exc == null) Files.delete(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
        
        Path targetDir = tempDir.resolve("packages").resolve("jdk-21");
        
        assertThrows(BackupException.class, () -> 
            rollbackService.restore("jdk-21", "20260201-100000", targetDir));
    }
    
    @Test
    void shouldReplaceExistingTargetDirectoryDuringRestore() throws IOException {
        // Create backup
        Path backupPath = createBackupDirectory("jdk-21", "20260201-100000");
        createTestFile(backupPath, "version.txt", "new version");
        
        // Create existing target with old content
        Path targetDir = tempDir.resolve("packages").resolve("jdk-21");
        Files.createDirectories(targetDir);
        createTestFile(targetDir, "old-file.txt", "old content");
        
        // Restore
        rollbackService.restore("jdk-21", "20260201-100000", targetDir);
        
        // Verify old file is gone and new file exists
        assertFalse(Files.exists(targetDir.resolve("old-file.txt")));
        assertTrue(Files.exists(targetDir.resolve("version.txt")));
        assertEquals("new version", 
            new String(Files.readAllBytes(targetDir.resolve("version.txt"))));
    }
    
    @Test
    void shouldCleanupBackupsExceedingKeepCount() throws IOException {
        when(config.getBackupKeepCount()).thenReturn(2);
        
        // Create 5 backups for jdk
        createBackupDirectory("jdk-21", "20260201-100000");
        createBackupDirectory("jdk-21", "20260202-100000");
        createBackupDirectory("jdk-21", "20260203-100000");
        createBackupDirectory("jdk-21", "20260204-100000");
        createBackupDirectory("jdk-21", "20260205-100000");
        
        // Cleanup
        rollbackService.cleanupOldBackups("jdk-21");
        
        List<RollbackService.BackupInfo> remaining = 
            rollbackService.listBackups("jdk-21");
        
        // Should keep only the 2 newest
        assertEquals(2, remaining.size());
        assertEquals("20260205-100000", remaining.get(0).timestampStr());
        assertEquals("20260204-100000", remaining.get(1).timestampStr());
    }
    
    @Test
    void shouldCleanupBackupsExceedingMaxAge() throws IOException {
        when(config.getBackupMaxAgeDays()).thenReturn(5);

        LocalDateTime now = LocalDateTime.now();
        String veryOldBackup = now.minusDays(50).format(TIMESTAMP_FORMAT);
        String oldBackup = now.minusDays(10).format(TIMESTAMP_FORMAT);
        String recentBackup = now.minusDays(2).format(TIMESTAMP_FORMAT);
        
        // Create backups relative to current date
        createBackupDirectory("jdk-21", veryOldBackup);
        createBackupDirectory("jdk-21", oldBackup);
        createBackupDirectory("jdk-21", recentBackup);
        
        // Cleanup
        rollbackService.cleanupOldBackups("jdk-21");
        
        List<RollbackService.BackupInfo> remaining = 
            rollbackService.listBackups("jdk-21");
        
        // Should have removed the oldest ones (older than 5 days)
        assertTrue(remaining.size() <= 3);
        // Recent backup should still be there
        assertTrue(remaining.stream()
            .anyMatch(b -> b.timestampStr().equals(recentBackup)));
        // Old backup should be removed
        assertFalse(remaining.stream()
            .anyMatch(b -> b.timestampStr().equals(veryOldBackup)));
    }
    
    @Test
    void shouldCleanupAllPackagesWhenNameIsNull() throws IOException {
        when(config.getBackupKeepCount()).thenReturn(1);
        
        // Create multiple backups per package
        createBackupDirectory("jdk-21", "20260201-100000");
        createBackupDirectory("jdk-21", "20260202-100000");
        createBackupDirectory("git", "20260201-100000");
        createBackupDirectory("git", "20260202-100000");
        
        // Cleanup all
        rollbackService.cleanupOldBackups(null);
        
        List<RollbackService.BackupInfo> jdkBackups = 
            rollbackService.listBackups("jdk-21");
        List<RollbackService.BackupInfo> gitBackups = 
            rollbackService.listBackups("git");
        
        // Each should have kept only 1
        assertEquals(1, jdkBackups.size());
        assertEquals(1, gitBackups.size());
    }
    
    @Test
    void shouldHandleBackupDirectoryWithoutSuffix() throws IOException {
        // Create a directory without the .backup- suffix
        Path invalidDir = backupDir.resolve("jdk-21");
        Files.createDirectories(invalidDir);
        
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups("jdk-21");
        
        // Should not include this directory
        assertTrue(backups.isEmpty());
    }
    
    @Test
    void shouldHandleBackupDirectoryWithInvalidTimestamp() throws IOException {
        // Create a directory with invalid timestamp format
        Path invalidDir = backupDir.resolve("jdk-21.backup-invalid");
        Files.createDirectories(invalidDir);
        
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups("jdk-21");
        
        // Should not include this directory
        assertTrue(backups.isEmpty());
    }
    
    // Helper methods
    
    private Path createBackupDirectory(String packageName, String timestamp) throws IOException {
        String dirName = packageName + ".backup-" + timestamp;
        Path backupPath = backupDir.resolve(dirName);
        Files.createDirectories(backupPath);
        return backupPath;
    }
    
    private void createTestFile(Path baseDir, String relativePath, String content) throws IOException {
        Path filePath = baseDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes());
    }
}
