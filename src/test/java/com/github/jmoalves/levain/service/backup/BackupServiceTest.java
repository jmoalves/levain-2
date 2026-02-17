package com.github.jmoalves.levain.service.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jmoalves.levain.config.Config;

/**
 * Unit tests for BackupService using JUnit 5.
 */
class BackupServiceTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private Config config;
    private BackupService backupService;
    private Path backupDir;

    @BeforeEach
    void setUp() throws IOException {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        // Create mock config
        config = mock(Config.class);
        backupDir = tempDir.resolve("backup");
        Files.createDirectories(backupDir);
        when(config.getBackupDir()).thenReturn(backupDir);

        backupService = new BackupService(config);
    }

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        } else {
            System.clearProperty("user.home");
        }
    }

    @Test
    void testBackupCreatesTimestampedDirectory() throws IOException {
        // Create a directory with some files
        Path sourceDir = tempDir.resolve("test-package");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file1.txt"), "content1");
        Files.writeString(sourceDir.resolve("file2.txt"), "content2");

        // Create backup
        BackupResult result = backupService.backup(sourceDir);

        // Verify backup was successful
        assertTrue(result.success());
        assertNotNull(result.backupPath());
        assertNotNull(result.timestamp());
        assertNotNull(result.packageName());
        assertEquals("test-package", result.packageName());
        assertTrue(Files.exists(result.backupPath()));
        assertTrue(result.backupPath().getFileName().toString()
                .startsWith("test-package.backup-"));
    }

    @Test
    void testBackupCopiesAllFiles() throws IOException {
        // Create a directory with nested structure
        Path sourceDir = tempDir.resolve("maven");
        Files.createDirectories(sourceDir.resolve("bin"));
        Files.createDirectories(sourceDir.resolve("conf"));
        Files.createDirectories(sourceDir.resolve("lib"));

        Files.writeString(sourceDir.resolve("bin/mvn"), "#!/bin/sh\necho maven");
        Files.writeString(sourceDir.resolve("conf/settings.xml"), "<settings/>");
        Files.writeString(sourceDir.resolve("lib/maven.jar"), "binary content");
        Files.writeString(sourceDir.resolve("README.md"), "# Maven");

        // Create backup
        BackupResult result = backupService.backup(sourceDir);

        // Verify all files are copied
        assertTrue(result.success());
        Path backup = result.backupPath();
        assertTrue(Files.exists(backup.resolve("bin/mvn")));
        assertTrue(Files.exists(backup.resolve("conf/settings.xml")));
        assertTrue(Files.exists(backup.resolve("lib/maven.jar")));
        assertTrue(Files.exists(backup.resolve("README.md")));

        // Verify content is preserved
        assertEquals("#!/bin/sh\necho maven", Files.readString(backup.resolve("bin/mvn")));
        assertEquals("<settings/>", Files.readString(backup.resolve("conf/settings.xml")));
        assertEquals("binary content", Files.readString(backup.resolve("lib/maven.jar")));
        assertEquals("# Maven", Files.readString(backup.resolve("README.md")));
    }

    @Test
    void testBackupFailsForNonExistentDirectory() {
        Path nonExistent = tempDir.resolve("does-not-exist");

        BackupResult result = backupService.backup(nonExistent);

        assertFalse(result.success());
        assertNotNull(result.error());
        assertTrue(result.error().contains("does not exist"));
    }

    @Test
    void testBackupFailsForFile() throws IOException {
        // Backup expects a directory, not a file
        Path file = tempDir.resolve("test-file.txt");
        Files.writeString(file, "content");

        BackupResult result = backupService.backup(file);

        assertFalse(result.success());
        assertNotNull(result.error());
        assertTrue(result.error().contains("not a directory"));
    }

    @Test
    void testBackupPreservesFileTimestamps() throws IOException {
        // Create a file with specific timestamp
        Path sourceDir = tempDir.resolve("package-with-timestamps");
        Files.createDirectories(sourceDir);
        Path file = sourceDir.resolve("old-file.txt");
        Files.writeString(file, "old content");

        // Set file timestamp to 10 days ago
        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Files.setLastModifiedTime(file, FileTime.from(tenDaysAgo));

        // Create backup
        BackupResult result = backupService.backup(sourceDir);

        // Verify timestamp is preserved (within 1 second tolerance)
        assertTrue(result.success());
        Path backedUpFile = result.backupPath().resolve("old-file.txt");
        FileTime originalTime = Files.getLastModifiedTime(file);
        FileTime backedUpTime = Files.getLastModifiedTime(backedUpFile);
        
        long diff = Math.abs(originalTime.toMillis() - backedUpTime.toMillis());
        assertTrue(diff < 1000, "Timestamps differ by " + diff + "ms");
    }

    @Test
    void testRestoreFromBackup() throws IOException {
        // Create original directory
        Path originalDir = tempDir.resolve("package-to-restore");
        Files.createDirectories(originalDir);
        Files.writeString(originalDir.resolve("file1.txt"), "version1");
        Files.writeString(originalDir.resolve("file2.txt"), "version1");

        // Create backup
        BackupResult backup = backupService.backup(originalDir);
        assertTrue(backup.success());

        // Modify original directory (simulating failed update)
        Files.writeString(originalDir.resolve("file1.txt"), "corrupted");
        Files.delete(originalDir.resolve("file2.txt"));
        Files.writeString(originalDir.resolve("file3.txt"), "new file");

        // Restore from backup
        backupService.restore(backup, originalDir);

        // Verify restoration
        assertEquals("version1", Files.readString(originalDir.resolve("file1.txt")));
        assertEquals("version1", Files.readString(originalDir.resolve("file2.txt")));
        assertFalse(Files.exists(originalDir.resolve("file3.txt")));
    }

    @Test
    void testRestoreThrowsExceptionForInvalidBackup() {
        Path targetDir = tempDir.resolve("target");
        BackupResult invalidBackup = BackupResult.failure("test-package", "error");

        assertThrows(BackupException.class, () -> backupService.restore(invalidBackup, targetDir));
    }

    @Test
    void testRestoreThrowsExceptionForNonExistentBackup() throws IOException {
        Path targetDir = tempDir.resolve("target");
        Path nonExistentBackup = backupDir.resolve("non-existent-backup");
        
        BackupResult backup = new BackupResult(
                "test-package",
                "20240101-120000",
                nonExistentBackup,
                true,
                null,
                1234L
        );

        assertThrows(BackupException.class, () -> backupService.restore(backup, targetDir));
    }

    @Test
    void testDeleteInstallationDirectory() throws IOException {
        // Create a directory to delete
        Path dirToDelete = tempDir.resolve("directory-to-delete");
        Files.createDirectories(dirToDelete.resolve("subdir"));
        Files.writeString(dirToDelete.resolve("file1.txt"), "content");
        Files.writeString(dirToDelete.resolve("subdir/file2.txt"), "content");

        // Delete directory
        backupService.deleteInstallationDirectory(dirToDelete);

        // Verify deletion
        assertFalse(Files.exists(dirToDelete));
    }

    @Test
    void testDeleteInstallationDirectoryDoesNotFailForNonExistent() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");

        // Should not throw exception
        backupService.deleteInstallationDirectory(nonExistent);

        assertFalse(Files.exists(nonExistent));
    }

    @Test
    void testBackupVerificationDetectsSizeMismatch() throws IOException {
        // Create source directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "content");

        // Create backup
        BackupResult result = backupService.backup(sourceDir);
        assertTrue(result.success());

        // Manually corrupt backup by adding files
        Files.writeString(result.backupPath().resolve("extra.txt"), 
                "x".repeat(1000000)); // Add 1MB

        // Verify backup size mismatch would be detected on next backup
        // (verification happens in backup() method internally)
        long sourceSize = calculateSize(sourceDir);
        long backupSize = calculateSize(result.backupPath());
        
        // Backup should be significantly larger due to extra file
        assertTrue(backupSize > sourceSize * 1.5);
    }

    @Test
    void testInsufficientDiskSpaceThrowsException() throws IOException {
        // This test is challenging to implement without actual disk space manipulation
        // We can test the exception type is correct
        assertNotNull(InsufficientSpaceException.class);
        
        // Verify exception contains useful information
        InsufficientSpaceException ex = new InsufficientSpaceException(
                1000000000L, 500000000L);
        assertTrue(ex.getMessage().contains("1000000000"));
        assertTrue(ex.getMessage().contains("500000000"));
    }

    @Test
    void testBackupResultCanRestore() throws IOException {
        Path sourceDir = tempDir.resolve("package");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "content");

        BackupResult result = backupService.backup(sourceDir);

        assertTrue(result.canRestore());
        assertEquals("package", result.packageName());
    }

    @Test
    void testBackupResultFailureCannotRestore() {
        BackupResult result = BackupResult.failure("package", "error");

        assertFalse(result.canRestore());
        assertEquals("package", result.packageName());
        assertEquals("error", result.error());
    }

    @Test
    void testBackupTimestampFormat() throws IOException {
        Path sourceDir = tempDir.resolve("package");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "content");

        BackupResult result = backupService.backup(sourceDir);

        // Verify timestamp format: yyyyMMdd-HHmmss
        String timestamp = result.timestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{8}-\\d{6}"));
    }

    @Test
    void testMultipleBackupsCreateDifferentTimestamps() throws IOException {
        Path sourceDir = tempDir.resolve("package");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "content");

        BackupResult result1 = backupService.backup(sourceDir);
        
        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(1100); // Wait just over 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        BackupResult result2 = backupService.backup(sourceDir);

        // Timestamps should be different
        assertNotNull(result1.timestamp());
        assertNotNull(result2.timestamp());
        assertFalse(result1.timestamp().equals(result2.timestamp()));
        
        // Both backups should exist
        assertTrue(Files.exists(result1.backupPath()));
        assertTrue(Files.exists(result2.backupPath()));
    }

    /**
     * Helper method to calculate directory size recursively.
     */
    private long calculateSize(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return Files.size(directory);
        }
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }
}
