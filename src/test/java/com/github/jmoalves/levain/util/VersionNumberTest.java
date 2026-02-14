package com.github.jmoalves.levain.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VersionNumberTest {

    @Test
    void shouldParseSimpleVersion() {
        VersionNumber v = new VersionNumber("1.2.3");
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getPatch());
    }

    @Test
    void shouldDetectNewerMajorVersion() {
        VersionNumber v1 = new VersionNumber("2.0.0");
        VersionNumber v2 = new VersionNumber("1.9.9");
        assertTrue(v1.isNewerThan(v2));
        assertFalse(v2.isNewerThan(v1));
    }

    @Test
    void shouldDetectNewerMinorVersion() {
        VersionNumber v1 = new VersionNumber("1.5.0");
        VersionNumber v2 = new VersionNumber("1.4.9");
        assertTrue(v1.isNewerThan(v2));
        assertFalse(v2.isNewerThan(v1));
    }

    @Test
    void shouldDetectNewerPatchVersion() {
        VersionNumber v1 = new VersionNumber("1.0.5");
        VersionNumber v2 = new VersionNumber("1.0.4");
        assertTrue(v1.isNewerThan(v2));
        assertFalse(v2.isNewerThan(v1));
    }

    @Test
    void shouldDetectEqualVersions() {
        VersionNumber v1 = new VersionNumber("1.2.3");
        VersionNumber v2 = new VersionNumber("1.2.3");
        assertFalse(v1.isNewerThan(v2));
        assertFalse(v1.isOlderThan(v2));
        assertTrue(v1.isEqualTo(v2));
    }

    @Test
    void shouldHandleVersionsWithSnapshot() {
        VersionNumber v = new VersionNumber("2.0.0-SNAPSHOT");
        assertEquals(2, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());
    }

    @Test
    void shouldHandleNullVersion() {
        VersionNumber v = new VersionNumber(null);
        assertEquals(0, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());
    }

    @Test
    void shouldHandleIncompleteVersions() {
        VersionNumber v1 = new VersionNumber("2");
        assertEquals(2, v1.getMajor());
        assertEquals(0, v1.getMinor());
        assertEquals(0, v1.getPatch());

        VersionNumber v2 = new VersionNumber("2.1");
        assertEquals(2, v2.getMajor());
        assertEquals(1, v2.getMinor());
        assertEquals(0, v2.getPatch());
    }

    @Test
    void shouldCompare() {
        VersionNumber v1 = new VersionNumber("2.0.0");
        VersionNumber v2 = new VersionNumber("1.9.9");
        assertTrue(v1.compareTo(v2) > 0);
        assertTrue(v2.compareTo(v1) < 0);
        assertEquals(0, v1.compareTo(new VersionNumber("2.0.0")));
    }

    @Test
    void shouldCompareMinorAndPatchVersions() {
        VersionNumber v1 = new VersionNumber("1.2.0");
        VersionNumber v2 = new VersionNumber("1.3.0");
        VersionNumber v3 = new VersionNumber("1.2.3");
        VersionNumber v4 = new VersionNumber("1.2.4");

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v3.compareTo(v4) < 0);
    }

    @Test
    void shouldDetectOlderVersion() {
        VersionNumber v1 = new VersionNumber("1.0.0");
        VersionNumber v2 = new VersionNumber("2.0.0");
        assertTrue(v1.isOlderThan(v2));
        assertFalse(v2.isOlderThan(v1));
    }

    @Test
    void shouldHaveConsistentToString() {
        VersionNumber v = new VersionNumber("1.2.3");
        assertEquals("1.2.3", v.toString());
    }

    @Test
    void shouldHaveWorkingEqualsAndHashCode() {
        VersionNumber v1 = new VersionNumber("1.2.3");
        VersionNumber v2 = new VersionNumber("1.2.3");
        VersionNumber v3 = new VersionNumber("1.2.4");

        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void shouldHandleNonNumericVersionParts() {
        VersionNumber v = new VersionNumber("alpha.beta.gamma");
        assertEquals(0, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());
    }

    @Test
    void shouldCompareAgainstNull() {
        VersionNumber v = new VersionNumber("1.0.0");
        assertTrue(v.compareTo(null) > 0);
        assertFalse(v.isEqualTo(null));
        assertFalse(v.isOlderThan(null));
    }

    @Test
    void shouldReturnFallbackToStringForNullOriginal() {
        VersionNumber v = new VersionNumber(null);
        assertEquals("0.0.0", v.toString());
    }

    @Test
    void shouldNotEqualDifferentType() {
        VersionNumber v = new VersionNumber("1.2.3");
        assertNotEquals(v, "1.2.3");
    }

    @Test
    void shouldHandleOverflowVersionParts() {
        VersionNumber v = new VersionNumber("999999999999999999999");
        assertEquals(0, v.getMajor());
    }

    @Test
    void shouldNotEqualNull() {
        VersionNumber v = new VersionNumber("1.2.3");
        assertNotEquals(v, null);
    }

    @Test
    void shouldTreatEmptyVersionAsZero() {
        VersionNumber v = new VersionNumber("");
        assertEquals(0, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());
    }

    @Test
    void shouldReportNewerThanNull() {
        VersionNumber v = new VersionNumber("1.0.0");
        assertTrue(v.isNewerThan(null));
    }
}
