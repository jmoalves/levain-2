package com.github.jmoalves.levain.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for upgrading Levain to a new version.
 * 
 * Uses a helper-process swap approach:
 * 1. Download new JAR to temporary location
 * 2. Spawn a helper process that:
 *    - Moves old JAR to backup location
 *    - Moves new JAR into place
 *    - Optionally relaunches Levain
 * 3. Main process exits
 * 
 * This approach avoids file-lock issues that would occur with in-place replacement.
 */
@ApplicationScoped
public class LevainUpgradeService {
    private static final Logger logger = LoggerFactory.getLogger(LevainUpgradeService.class);

    private final ReleaseCheckService releaseCheckService;

    @Inject
    public LevainUpgradeService(ReleaseCheckService releaseCheckService) {
        this.releaseCheckService = releaseCheckService;
    }

    /**
     * Perform a full upgrade of Levain.
     * 
     * @param latestVersion the version to upgrade to
     * @param releaseAssets the release assets from GitHub
     * @param relaunch if true, relaunch Levain after upgrade
     * @return true if upgrade was successful
     */
    public boolean upgradeToVersion(String latestVersion, 
                                   ReleaseCheckService.GithubRelease release,
                                   boolean relaunch) {
        try {
            logger.info("Starting Levain upgrade to version: {}", latestVersion);

            // Verify we have the JAR asset
            Optional<ReleaseCheckService.Asset> jarAsset = release.getJarAsset();
            if (jarAsset.isEmpty()) {
                logger.error("No JAR artifact found in release {}", latestVersion);
                return false;
            }

            ReleaseCheckService.Asset asset = jarAsset.get();

            // Get the current JAR location
            Path currentJar = getCurrentLevainJarPath();
            if (currentJar == null) {
                logger.error("Could not determine current Levain JAR location");
                return false;
            }

            logger.debug("Current JAR location: {}", currentJar);

            // Download the new JAR
            Path tempDir = Files.createTempDirectory("levain-upgrade-");
            Path newJar = tempDir.resolve("levain-" + latestVersion + ".jar");

            if (!releaseCheckService.downloadRelease(asset.getDownloadUrl(), newJar)) {
                logger.error("Failed to download new Levain JAR");
                return false;
            }

            // Create backup directory
            Path backupDir = currentJar.getParent().resolve("backups");
            Files.createDirectories(backupDir);

            // Create backup filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            Path backupJar = backupDir.resolve("levain-" + timestamp + ".jar");

            // Stage the upgrade by creating a helper script
            Path helperScript = createHelperScript(currentJar, newJar, backupJar, relaunch);

            // Execute the helper script in a separate process
            if (!executeHelperScript(helperScript)) {
                logger.error("Failed to execute upgrade helper script");
                return false;
            }

            logger.info("Upgrade initialization complete. Helper process will complete the upgrade.");
            return true;

        } catch (Exception e) {
            logger.error("Error during upgrade process: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the path to the current Levain JAR file.
     * 
     * This attempts to determine the JAR file that is currently running.
     * Returns null if it cannot be determined.
     */
    private Path getCurrentLevainJarPath() {
        try {
            String classpath = System.getProperty("java.class.path");
            if (classpath != null && !classpath.isEmpty()) {
                // Try to find a levain JAR in the classpath
                for (String path : classpath.split(System.getProperty("path.separator"))) {
                    if (path.contains("levain-") && path.endsWith(".jar")) {
                        return Paths.get(path).toAbsolutePath();
                    }
                }
            }

            // Try the CodeSource (for JAR execution)
            Class<?> clazz = LevainUpgradeService.class;
            String location = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (location != null && location.endsWith(".jar")) {
                return Paths.get(location).toAbsolutePath();
            }

            return null;
        } catch (Exception e) {
            logger.debug("Could not determine current JAR path: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create a helper script that will perform the upgrade.
     * 
     * The script is platform-specific (Windows batch or Unix shell script).
     */
    private Path createHelperScript(Path currentJar, Path newJar, Path backupJar, boolean relaunch) 
            throws IOException {
        
        Path scriptDir = Files.createTempDirectory("levain-helper-");
        Path scriptFile;

        if (isWindows()) {
            scriptFile = scriptDir.resolve("upgrade.bat");
            String script = createWindowsUpgradeScript(currentJar, newJar, backupJar, relaunch);
            Files.writeString(scriptFile, script);
        } else {
            scriptFile = scriptDir.resolve("upgrade.sh");
            String script = createUnixUpgradeScript(currentJar, newJar, backupJar, relaunch);
            Files.writeString(scriptFile, script);
            
            // Make script executable
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
            Files.setPosixFilePermissions(scriptFile, perms);
        }

        logger.debug("Created helper script: {}", scriptFile);
        return scriptFile;
    }

    /**
     * Create a Windows batch script for the upgrade process.
     */
    private String createWindowsUpgradeScript(Path currentJar, Path newJar, Path backupJar, boolean relaunch) {
        StringBuilder script = new StringBuilder();
        script.append("@echo off\n");
        script.append("setlocal enabledelayedexpansion\n");
        script.append("\n");
        script.append("REM Levain upgrade helper script - Windows\n");
        script.append("REM This script handles the atomic swap of old and new JAR files\n");
        script.append("\n");
        
        // Wait a bit for main process to exit
        script.append("timeout /t 2 /nobreak\n");
        
        // Backup current JAR
        script.append(String.format("echo Backing up current JAR to: %s\n", backupJar));
        script.append(String.format("move /Y \"%s\" \"%s\"\n", currentJar, backupJar));
        script.append("if !errorlevel! neq 0 (\n");
        script.append("    echo Error backing up JAR\n");
        script.append("    exit /b 1\n");
        script.append(")\n");
        script.append("\n");
        
        // Move new JAR into place
        script.append(String.format("echo Deploying new JAR to: %s\n", currentJar));
        script.append(String.format("move /Y \"%s\" \"%s\"\n", newJar, currentJar));
        script.append("if !errorlevel! neq 0 (\n");
        script.append("    echo Error deploying new JAR\n");
        script.append(String.format("    move /Y \"%s\" \"%s\"\n", backupJar, currentJar)); // Restore backup
        script.append("    exit /b 1\n");
        script.append(")\n");
        script.append("\n");
        
        // Relaunch if requested
        if (relaunch) {
            script.append("echo Relaunching Levain\n");
            script.append(String.format("start \"\" \"%s\"\n", currentJar));
        } else {
            script.append("echo Upgrade complete. Levain has been updated.\n");
        }

        return script.toString();
    }

    /**
     * Create a Unix shell script for the upgrade process.
     */
    private String createUnixUpgradeScript(Path currentJar, Path newJar, Path backupJar, boolean relaunch) {
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("\n");
        script.append("# Levain upgrade helper script - Unix\n");
        script.append("# This script handles the atomic swap of old and new JAR files\n");
        script.append("\n");
        
        script.append("set -e\n");
        script.append("trap 'echo \"Upgrade failed\"; exit 1' ERR\n");
        script.append("\n");
        
        // Wait a bit for main process to exit
        script.append("sleep 2\n");
        script.append("\n");
        
        // Backup current JAR
        script.append(String.format("echo \"Backing up current JAR to: %s\"\n", backupJar));
        script.append(String.format("mv \"%s\" \"%s\"\n", currentJar, backupJar));
        script.append("\n");
        
        // Move new JAR into place
        script.append(String.format("echo \"Deploying new JAR to: %s\"\n", currentJar));
        script.append(String.format("mv \"%s\" \"%s\"\n", newJar, currentJar));
        script.append("\n");
        
        // Relaunch if requested
        if (relaunch) {
            script.append("echo \"Relaunching Levain\"\n");
            script.append(String.format("java -jar \"%s\" &\n", currentJar));
        } else {
            script.append("echo \"Upgrade complete. Levain has been updated.\"\n");
        }

        return script.toString();
    }

    /**
     * Execute the helper script in a separate process.
     */
    private boolean executeHelperScript(Path scriptFile) {
        try {
            List<String> command = new ArrayList<>();
            
            if (isWindows()) {
                command.add("cmd.exe");
                command.add("/c");
            } else {
                command.add("sh");
            }
            
            command.add(scriptFile.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit I/O for logging
            
            Process process = pb.start();
            logger.debug("Helper process started with PID: {}", process.pid());
            
            // Don't wait for completion - we'll exit and let it run
            return true;

        } catch (IOException e) {
            logger.error("Failed to execute helper script: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if running on Windows.
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}
