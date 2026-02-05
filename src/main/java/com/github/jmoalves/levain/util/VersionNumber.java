package com.github.jmoalves.levain.util;

/**
 * Utility class for comparing semantic versions (major.minor.patch).
 */
public class VersionNumber implements Comparable<VersionNumber> {
    private final int major;
    private final int minor;
    private final int patch;
    private final String original;

    public VersionNumber(String versionString) {
        this.original = versionString;
        String[] parts = (versionString != null ? versionString : "0.0.0").split("\\.");
        
        this.major = parseInt(parts.length > 0 ? parts[0] : "0");
        this.minor = parseInt(parts.length > 1 ? parts[1] : "0");
        this.patch = parseInt(parts.length > 2 ? parts[2] : "0");
    }

    private static int parseInt(String str) {
        try {
            // Handle versions like "2.0.0-SNAPSHOT" by extracting just the number
            String numOnly = str.replaceAll("[^0-9]", "");
            return numOnly.isEmpty() ? 0 : Integer.parseInt(numOnly);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    /**
     * Check if this version is newer than the other version.
     */
    public boolean isNewerThan(VersionNumber other) {
        if (other == null) {
            return true;
        }
        if (this.major != other.major) {
            return this.major > other.major;
        }
        if (this.minor != other.minor) {
            return this.minor > other.minor;
        }
        return this.patch > other.patch;
    }

    /**
     * Check if this version is older than the other version.
     */
    public boolean isOlderThan(VersionNumber other) {
        if (other == null) {
            return false;
        }
        return other.isNewerThan(this);
    }

    /**
     * Check if versions are equal.
     */
    public boolean isEqualTo(VersionNumber other) {
        if (other == null) {
            return false;
        }
        return this.major == other.major && 
               this.minor == other.minor && 
               this.patch == other.patch;
    }

    @Override
    public int compareTo(VersionNumber other) {
        if (other == null) {
            return 1;
        }
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return original != null ? original : String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionNumber that = (VersionNumber) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return 31 * 31 * major + 31 * minor + patch;
    }
}
