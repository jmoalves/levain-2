package com.github.jmoalves.levain.service.clean;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.service.rollback.RollbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CleanService backup cleanup functionality.
 */
@DisplayName("CleanService Tests")
class CleanServiceTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Mock
    private RollbackService rollbackService;
    
    @Mock
    private Config config;
    
    @InjectMocks
    private CleanService cleanService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    // ============ Preview Tests ============
    
    @Test
    @DisplayName("Preview cleanup should return empty when no backups exist")
    void previewCleanup_NoBackups_ReturnsEmpty() throws Exception {
        Map<String, List<RollbackService.BackupInfo>> emptyMap = new HashMap<>();
        when(rollbackService.listAllBackups()).thenReturn(emptyMap);
        
        var result = cleanService.previewCleanup(null, 30, 5);
        
        assertEquals(0, result.toDelete.size());
    }
    
    @Test
    @DisplayName("Preview cleanup should not delete any backups in dry-run mode")
    void previewCleanup_DryRun_NoActualDeletion() throws Exception {
        List<RollbackService.BackupInfo> backups = createTestBackups("pkg1", 10);
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        backupMap.put("pkg1", backups);
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        var result = cleanService.previewCleanup(null, 2, 5);
        
        // Should identify some backups for deletion but not actually delete
        assertTrue(result.toDelete.size() > 0);
        verify(rollbackService, never()).deleteBackup(anyString(), any());
    }
    
    @Test
    @DisplayName("Preview cleanup should filter by older-than days")
    void previewCleanup_FilterByAge_OnlyOldBackups() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        
        // Recent backup (1 day old)
        backups.add(createBackupInfo("pkg1", now.minusDays(1), 100));
        
        // Old backup (35 days old)
        backups.add(createBackupInfo("pkg1", now.minusDays(35), 200));
        
        backupMap.put("pkg1", backups);
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        // Only delete backups older than 30 days, keep at least 1
        var result = cleanService.previewCleanup(null, 30, 1);
        
        // Should only identify the 35-day-old backup for deletion
        assertEquals(1, result.toDelete.size());
        assertEquals("pkg1", result.toDelete.get(0).packageName);
    }
    
    @Test
    @DisplayName("Preview cleanup should respect keep count per package")
    void previewCleanup_RespectKeepCount_KeepsNewest() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        
        // Create 5 backups for pkg1, all ancient to bypass age filter
        for (int i = 0; i < 5; i++) {
            backups.add(createBackupInfo("pkg1", now.minusDays(100 + i), 100 * (i + 1)));
        }
        
        backupMap.put("pkg1", backups);
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        // Keep only 2 most recent, no age filter (set to 365 to not filter by age)
        var result = cleanService.previewCleanup(null, 365, 2);
        
        // Should identify 3 backups for deletion (keep 2 newest based on index)
        assertEquals(3, result.toDelete.size());
    }
    
    @Test
    @DisplayName("Preview cleanup should filter by package name")
    void previewCleanup_FilterByPackage_OnlySelectedPackage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        
        List<RollbackService.BackupInfo> pkg1Backups = new ArrayList<>();
        pkg1Backups.add(createBackupInfo("pkg1", now.minusDays(50), 100));
        backupMap.put("pkg1", pkg1Backups);
        
        List<RollbackService.BackupInfo> pkg2Backups = new ArrayList<>();
        pkg2Backups.add(createBackupInfo("pkg2", now.minusDays(50), 200));
        backupMap.put("pkg2", pkg2Backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        var result = cleanService.previewCleanup("pkg1", 30, 1);
        
        // Should only have pkg1 backups for deletion
        assertTrue(result.toDelete.stream()
            .allMatch(item -> item.packageName.equals("pkg1")));
    }
    
    @Test
    @DisplayName("Preview cleanup should combine age and keep-count filters")
    void previewCleanup_CombinedFilters_ApplyBoth() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        
        // Mix of recent and old backups
        backups.add(createBackupInfo("pkg1", now.minusDays(5), 100));
        backups.add(createBackupInfo("pkg1", now.minusDays(15), 200));
        backups.add(createBackupInfo("pkg1", now.minusDays(40), 300));
        backups.add(createBackupInfo("pkg1", now.minusDays(60), 400));
        
        backupMap.put("pkg1", backups);
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        // Delete older than 30 days AND exceeding keep count of 2
        // Backups: 5d (index 0, keep), 15d (index 1, keep), 40d (index 2, > 30 days), 60d (index 3, > 30 days)
        // Both 40d and 60d exceed keep-count AND are old
        var result = cleanService.previewCleanup(null, 30, 2);
        
        // Should identify 2 backups for deletion (40d and 60d backups)
        assertEquals(2, result.toDelete.size());
    }
    
    // ============ Execute Tests ============
    
    @Test
    @DisplayName("Execute cleanup should actually delete backups")
    void executeCleanup_ValidCriteria_DeletesBackups() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        backups.add(createBackupInfo("pkg1", now.minusDays(50), 100));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        var result = cleanService.executeCleanup(null, 30, 1);
        
        // Should attempt deletion
        verify(rollbackService).deleteBackup(anyString(), any());
    }
    
    @Test
    @DisplayName("Execute cleanup should track deleted count and size")
    void executeCleanup_SuccessfulDelete_TracksStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        backups.add(createBackupInfo("pkg1", now.minusDays(50), 500));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        doNothing().when(rollbackService).deleteBackup(anyString(), any());
        
        var result = cleanService.executeCleanup(null, 30, 1);
        
        assertEquals(1, result.getSuccessfulDeletions());
        assertEquals(500, result.getTotalSizeToDelete());
        assertEquals(0, result.getFailedDeletions());
    }
    
    @Test
    @DisplayName("Execute cleanup should handle deletion failures gracefully")
    void executeCleanup_DeletionFails_TracksFailed() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        backups.add(createBackupInfo("pkg1", now.minusDays(50), 100));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        doThrow(new RuntimeException("Delete failed")).when(rollbackService).deleteBackup(anyString(), any());
        
        var result = cleanService.executeCleanup(null, 30, 1);
        
        assertEquals(0, result.getSuccessfulDeletions());
        assertEquals(1, result.getFailedDeletions());
    }
    
    @Test
    @DisplayName("Execute cleanup should continue on partial failures")
    void executeCleanup_PartialFailure_ContinuesProcessing() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        backups.add(createBackupInfo("pkg1", now.minusDays(50), 100));
        backups.add(createBackupInfo("pkg1", now.minusDays(60), 200));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        // First delete succeeds, second fails
        doNothing().doThrow(new RuntimeException("Delete failed"))
            .when(rollbackService).deleteBackup(anyString(), any());
        
        var result = cleanService.executeCleanup(null, 30, 1);
        
        // Should have processed both
        assertEquals(1, result.getSuccessfulDeletions());
        assertEquals(1, result.getFailedDeletions());
    }
    
    @Test
    @DisplayName("Execute cleanup should empty list when no criteria match")
    void executeCleanup_NothingToDelete_ReturnsEmpty() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        // Only recent backups
        backups.add(createBackupInfo("pkg1", now.minusDays(5), 100));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        var result = cleanService.executeCleanup(null, 30, 1);
        
        assertEquals(0, result.getSuccessfulDeletions());
        assertEquals(0, result.getFailedDeletions());
        verify(rollbackService, never()).deleteBackup(anyString(), any());
    }
    
    // ============ Edge Cases ============
    
    @Test
    @DisplayName("Cleanup should handle packages with backup-less directories")
    void cleanup_MultiplePackages_HandlesMissingBackups() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        
        List<RollbackService.BackupInfo> pkg1Backups = new ArrayList<>();
        pkg1Backups.add(createBackupInfo("pkg1", now.minusDays(50), 100));
        backupMap.put("pkg1", pkg1Backups);
        
        // pkg2 has no backups (not in map at all)
        
        List<RollbackService.BackupInfo> pkg3Backups = new ArrayList<>();
        pkg3Backups.add(createBackupInfo("pkg3", now.minusDays(50), 200));
        backupMap.put("pkg3", pkg3Backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        var result = cleanService.previewCleanup(null, 30, 1);
        
        // Should successfully handle mixed scenarios
        assertEquals(2, result.toDelete.size());
    }
    
    @Test
    @DisplayName("Cleanup should respect minimum keep count (never delete all)")
    void cleanup_AlwaysKeepMinimum_NeverDeleteAll() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<RollbackService.BackupInfo>> backupMap = new HashMap<>();
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        // Single old backup far in past
        backups.add(createBackupInfo("pkg1", now.minusDays(100), 100));
        backupMap.put("pkg1", backups);
        
        when(rollbackService.listAllBackups()).thenReturn(backupMap);
        
        // Even with aggressive age filter, keep at least 1
        var result = cleanService.previewCleanup("pkg1", 365, 1);
        
        // Should NOT delete the only backup for pkg1
        assertEquals(0, result.toDelete.size());
    }
    
    // ============ Helper Methods ============
    
    private List<RollbackService.BackupInfo> createTestBackups(String packageName, int count) {
        List<RollbackService.BackupInfo> backups = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            backups.add(createBackupInfo(packageName, now.minusDays(i), 100 * (i + 1)));
        }
        
        return backups;
    }
    
    private RollbackService.BackupInfo createBackupInfo(String packageName, LocalDateTime timestamp, long sizeBytes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestampStr = timestamp.format(formatter);
        
        return new RollbackService.BackupInfo(
            packageName,
            timestamp,
            timestampStr,
            sizeBytes
        );
    }
}
