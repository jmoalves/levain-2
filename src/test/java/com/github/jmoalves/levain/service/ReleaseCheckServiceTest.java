package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for ReleaseCheckService version comparison logic.
 */
@DisplayName("Release Check Service")
class ReleaseCheckServiceTest {

    @Test
    @DisplayName("Should correctly compare semantic versions")
    void testSemanticVersionComparison() {
        // Higher major version
        assertTrue(ReleaseCheckService.isNewerVersion("3.0.0", "2.5.1"));
        
        // Higher minor version
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.0", "2.0.5"));
        
        // Higher patch version
        assertTrue(ReleaseCheckService.isNewerVersion("2.0.1", "2.0.0"));
        
        // Same version
        assertFalse(ReleaseCheckService.isNewerVersion("2.0.0", "2.0.0"));
        
        // Older version
        assertFalse(ReleaseCheckService.isNewerVersion("1.9.9", "2.0.0"));
    }

    @Test
    @DisplayName("Should handle version prefixes like 'v'")
    void testVersionPrefixes() {
        assertTrue(ReleaseCheckService.isNewerVersion("v2.1.0", "v2.0.0"));
        assertTrue(ReleaseCheckService.isNewerVersion("v2.1.0", "2.0.0"));
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.0", "v2.0.0"));
    }

    @Test
    @DisplayName("Should handle SNAPSHOT and pre-release versions")
    void testPreReleaseVersions() {
        // 2.1.1 > 2.1.0-SNAPSHOT (higher base version)
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.1", "2.1.0-SNAPSHOT"));
        
        // 2.1.1 > 2.1.0-RC1 (higher base version)
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.1", "2.1.0-RC1"));
        
        // 2.1.1 > 2.1.0-BETA (higher base version)
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.1", "2.1.0-BETA"));
    }

    @Test
    @DisplayName("Should handle null versions gracefully")
    void testNullVersions() {
        assertFalse(ReleaseCheckService.isNewerVersion(null, "2.0.0"));
        assertFalse(ReleaseCheckService.isNewerVersion("2.0.0", null));
        assertFalse(ReleaseCheckService.isNewerVersion(null, null));
    }

    @Test
    @DisplayName("Should handle empty version parts")
    void testEmptyVersionParts() {
        assertTrue(ReleaseCheckService.isNewerVersion("2.0", "1.9.9.9"));
        assertTrue(ReleaseCheckService.isNewerVersion("2", "1.9.9.9"));
    }

    @Test
    @DisplayName("Should handle complex real-world versions")
    void testRealWorldVersions() {
        // Test realistic scenarios
        assertTrue(ReleaseCheckService.isNewerVersion("2.1.0", "2.0.0"));
        assertTrue(ReleaseCheckService.isNewerVersion("2.0.1-RC1", "2.0.0"));
        assertFalse(ReleaseCheckService.isNewerVersion("2.0.0", "2.0.1"));
    }
}
