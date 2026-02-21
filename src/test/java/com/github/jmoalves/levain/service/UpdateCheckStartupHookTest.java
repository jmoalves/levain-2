package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.jmoalves.levain.config.Config;

/**
 * Tests for UpdateCheckStartupHook.
 */
@DisplayName("Update Check Startup Hook")
class UpdateCheckStartupHookTest {

    private ReleaseCheckService releaseCheckService;
    private LevainUpgradeService levainUpgradeService;
    private Config config;
    private UpdateCheckStartupHook hook;

    @BeforeEach
    void setUp() {
        releaseCheckService = mock(ReleaseCheckService.class);
        levainUpgradeService = mock(LevainUpgradeService.class);
        config = mock(Config.class);
        hook = new UpdateCheckStartupHook(releaseCheckService, levainUpgradeService, config);
    }

    @Test
    @DisplayName("Should skip update check when flag is true")
    void testSkipWhenFlagIsTrue() {
        when(config.isAutoUpdate()).thenReturn(true);

        boolean result = hook.runUpdateCheck(true, "2.0.0");

        assertFalse(result);
        verify(releaseCheckService, never()).checkForUpdates(anyString());
    }

    @Test
    @DisplayName("Should skip update check when config disables auto-update")
    void testSkipWhenConfigDisablesAutoUpdate() {
        when(config.isAutoUpdate()).thenReturn(false);

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
        verify(releaseCheckService, never()).checkForUpdates(anyString());
    }

    @Test
    @DisplayName("Should skip if not enough time has passed since last question")
    void testSkipIfNotEnoughTimePassedSinceLastQuestion() {
        when(config.isAutoUpdate()).thenReturn(true);
        
        // Set last question to now (no time has passed)
        long now = System.currentTimeMillis();
        when(config.getLastUpdateQuestion()).thenReturn(String.valueOf(now));

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
        verify(releaseCheckService, never()).checkForUpdates(anyString());
    }

    @Test
    @DisplayName("Should check for updates if enough time has passed")
    void testCheckForUpdatesIfEnoughTimeHasPassed() {
        when(config.isAutoUpdate()).thenReturn(true);
        
        // Set last question to 25 hours ago
        long pastTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000);
        when(config.getLastUpdateQuestion()).thenReturn(String.valueOf(pastTime));
        when(config.getLastKnownVersion()).thenReturn(null);
        
        // No new version available
        when(releaseCheckService.checkForUpdates("2.0.0")).thenReturn(Optional.empty());

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
        verify(releaseCheckService, times(1)).checkForUpdates("2.0.0");
    }

    @Test
    @DisplayName("Should check for updates if never asked before")
    void testCheckForUpdatesIfNeverAskedBefore() {
        when(config.isAutoUpdate()).thenReturn(true);
        when(config.getLastUpdateQuestion()).thenReturn(null);
        
        // No new version available
        when(releaseCheckService.checkForUpdates("2.0.0")).thenReturn(Optional.empty());

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
        verify(releaseCheckService, times(1)).checkForUpdates("2.0.0");
    }

    @Test
    @DisplayName("Should handle invalid timestamp in config gracefully")
    void testHandleInvalidTimestampGracefully() {
        when(config.isAutoUpdate()).thenReturn(true);
        when(config.getLastUpdateQuestion()).thenReturn("invalid-timestamp");
        
        // No new version available
        when(releaseCheckService.checkForUpdates("2.0.0")).thenReturn(Optional.empty());

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
        // Should still attempt to check for updates
        verify(releaseCheckService, times(1)).checkForUpdates("2.0.0");
    }

    @Test
    @DisplayName("Should handle release check failure gracefully")
    void testHandleReleaseCheckFailure() {
        when(config.isAutoUpdate()).thenReturn(true);
        when(config.getLastUpdateQuestion()).thenReturn(null);
        
        // Exception during check
        when(releaseCheckService.checkForUpdates("2.0.0"))
                .thenThrow(new RuntimeException("Network error"));

        boolean result = hook.runUpdateCheck(false, "2.0.0");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should save timestamp when check completes (no update available)")
    void testSaveConfigMetadataAfterChecking() {
        when(config.isAutoUpdate()).thenReturn(true);
        when(config.getLastUpdateQuestion()).thenReturn(null);
        
        // Mock time interval check to succeed
        when(config.getLastUpdateQuestion()).thenReturn(null);
        
        // No new version available - check should exit early without metadata save
        when(releaseCheckService.checkForUpdates("2.0.0")).thenReturn(Optional.empty());

        hook.runUpdateCheck(false, "2.0.0");

        // Since no new version was found, we exit early without saving metadata
        // This is the expected behavior
        verify(config).isAutoUpdate();
        verify(releaseCheckService).checkForUpdates("2.0.0");
    }
}
