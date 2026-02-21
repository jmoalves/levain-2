package com.github.jmoalves.levain.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for checking and managing Levain releases from GitHub.
 * 
 * Connects to the GitHub API to:
 * - Fetch the latest release
 * - Compare version numbers
 * - Download release artifacts
 */
@ApplicationScoped
public class ReleaseCheckService {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseCheckService.class);
    private static final String GITHUB_REPO = "jmoalves/levain";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/" + GITHUB_REPO + "/releases";
    private static final int TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReleaseCheckService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if a new version is available on GitHub.
     * 
     * @param currentVersion The current version string (e.g., "2.0.0")
     * @return Optional containing the latest release if a newer version is available
     */
    public Optional<GithubRelease> checkForUpdates(String currentVersion) {
        try {
            logger.debug("Checking for updates from GitHub API");
            Optional<GithubRelease> latestRelease = fetchLatestRelease();

            if (latestRelease.isEmpty()) {
                logger.debug("No release information available from GitHub");
                return Optional.empty();
            }

            GithubRelease release = latestRelease.get();
            String latestVersion = release.getVersion();

            if (isNewerVersion(latestVersion, currentVersion)) {
                logger.info("New version available: {} (current: {})", latestVersion, currentVersion);
                return Optional.of(release);
            }

            logger.debug("Already running the latest version: {}", currentVersion);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to check for updates: {}", e.getMessage());
            logger.debug("Update check error details:", e);
            return Optional.empty();
        }
    }

    /**
     * Fetch the latest release information from GitHub API.
     * 
     * @return Optional containing the latest release
     */
    private Optional<GithubRelease> fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Levain-UpdateCheck/2.0")
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                GithubRelease release = objectMapper.readValue(response.body(), GithubRelease.class);
                logger.debug("Latest release from GitHub: {}", release.getTagName());
                return Optional.of(release);
            }

            logger.warn("GitHub API returned status code: {}", response.statusCode());
            return Optional.empty();
        } catch (IOException | InterruptedException e) {
            logger.debug("Failed to fetch latest release from GitHub: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Reset interrupted status if needed
            return Optional.empty();
        }
    }

    /**
     * Download a release artifact from GitHub.
     * 
     * @param downloadUrl The URL of the artifact to download
     * @param targetPath The path where the artifact should be saved
     * @return true if download was successful, false otherwise
     */
    public boolean downloadRelease(String downloadUrl, Path targetPath) {
        try {
            logger.info("Downloading release from: {}", downloadUrl);

            // Ensure parent directory exists
            Files.createDirectories(targetPath.getParent());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("Accept", "application/octet-stream")
                    .header("User-Agent", "Levain-UpdateCheck/2.0")
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                Files.write(targetPath, response.body());
                logger.info("Downloaded release to: {}", targetPath);
                return true;
            }

            logger.warn("Download failed with status code: {}", response.statusCode());
            return false;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to download release: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Compare two semantic version strings and determine if the new version is newer.
     * 
     * @param newVersion The new version to compare
     * @param currentVersion The current version
     * @return true if newVersion > currentVersion
     */
    public static boolean isNewerVersion(String newVersion, String currentVersion) {
        if (newVersion == null || currentVersion == null) {
            return false;
        }

        // Remove 'v' prefix if present
        String newVer = newVersion.replaceFirst("^v", "");
        String currVer = currentVersion.replaceFirst("^v", "");

        // Handle SNAPSHOT and other suffixes
        newVer = newVer.replaceAll("(-SNAPSHOT|-RC\\d+|-BETA|-ALPHA|-PREVIEW).*$", "");
        currVer = currVer.replaceAll("(-SNAPSHOT|-RC\\d+|-BETA|-ALPHA|-PREVIEW).*$", "");

        try {
            String[] newParts = newVer.split("\\.");
            String[] currParts = currVer.split("\\.");

            int maxLength = Math.max(newParts.length, currParts.length);
            for (int i = 0; i < maxLength; i++) {
                int newPartNum = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
                int currPartNum = i < currParts.length ? parseVersionPart(currParts[i]) : 0;

                if (newPartNum > currPartNum) {
                    return true;
                } else if (newPartNum < currPartNum) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("Error comparing versions {} vs {}: {}", newVersion, currentVersion, e.getMessage());
            return false;
        }
    }

    /**
     * Parse a version part (e.g., "2" from "2.0.0") to an integer.
     */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * GitHub Release API response structure.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GithubRelease {
        @JsonProperty("tag_name")
        public String tagName;

        @JsonProperty("name")
        public String name;

        @JsonProperty("draft")
        public boolean draft;

        @JsonProperty("prerelease")
        public boolean prerelease;

        @JsonProperty("published_at")
        public String publishedAt;

        @JsonProperty("assets")
        public List<Asset> assets;

        public String getTagName() {
            return tagName;
        }

        public String getVersion() {
            return tagName;
        }

        public String getName() {
            return name;
        }

        public boolean isDraft() {
            return draft;
        }

        public boolean isPrerelease() {
            return prerelease;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public List<Asset> getAssets() {
            return assets;
        }

        /**
         * Find the JAR artifact for the current OS.
         * Looks for "levain-*.jar" in the latest release assets.
         */
        public Optional<Asset> getJarAsset() {
            if (assets == null) {
                return Optional.empty();
            }
            return assets.stream()
                    .filter(a -> a.getName().matches("levain-.*\\.jar$"))
                    .findFirst();
        }

        @Override
        public String toString() {
            return "GithubRelease{" + "tagName='" + tagName + '\'' + '}';
        }
    }

    /**
     * GitHub Release Asset structure.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Asset {
        @JsonProperty("name")
        public String name;

        @JsonProperty("download_url")
        public String downloadUrl;

        @JsonProperty("browser_download_url")
        public String browserDownloadUrl;

        @JsonProperty("size")
        public long size;

        public String getName() {
            return name;
        }

        public String getDownloadUrl() {
            return browserDownloadUrl != null ? browserDownloadUrl : downloadUrl;
        }

        public long getSize() {
            return size;
        }

        @Override
        public String toString() {
            return "Asset{" + "name='" + name + '\'' + ", size=" + size + '}';
        }
    }
}
