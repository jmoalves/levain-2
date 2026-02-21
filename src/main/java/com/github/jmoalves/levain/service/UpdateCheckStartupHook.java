package com.github.jmoalves.levain.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.config.Config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Startup hook service for checking Levain self-updates.
 * 
 * This service:
 * - Runs on every command execution
 * - Checks if updates are available (max once per 24 hours)
 * - Prompts the user to upgrade if a new version is found
 * - Can be disabled globally in config or per-command with --skip-levain-updates
 */
@ApplicationScoped
public class UpdateCheckStartupHook {
    private static final Logger logger = LoggerFactory.getLogger(UpdateCheckStartupHook.class);
    private static final long MIN_PROMPT_INTERVAL_HOURS = 24;

    private final ReleaseCheckService releaseCheckService;
    private final LevainUpgradeService levainUpgradeService;
    private final Config config;

    @Inject
    public UpdateCheckStartupHook(ReleaseCheckService releaseCheckService, 
                                 LevainUpgradeService levainUpgradeService,
                                 Config config) {
        this.releaseCheckService = releaseCheckService;
        this.levainUpgradeService = levainUpgradeService;
        this.config = config;
    }

    /**
     * Run the update check on startup.
     * 
     * @param skipLevainUpdates if true, skip update checks for this command
     * @param currentVersion the current Levain version
     * @return true if an update was found and accepted by user
     */
    public boolean runUpdateCheck(boolean skipLevainUpdates, String currentVersion) {
        // Respect command-line flag and config setting
        if (skipLevainUpdates || !config.isAutoUpdate()) {
            logger.debug("Levain update check skipped (flag={}, config={})", 
                    skipLevainUpdates, config.isAutoUpdate());
            return false;
        }

        // Check if we should prompt based on time interval
        if (!shouldPromptForUpdate()) {
            logger.debug("Not enough time has passed since last update question");
            return false;
        }

        // Check for updates
        try {
            Optional<ReleaseCheckService.GithubRelease> latestRelease = 
                    releaseCheckService.checkForUpdates(currentVersion);

            if (latestRelease.isEmpty()) {
                return false;
            }

            ReleaseCheckService.GithubRelease release = latestRelease.get();
            String latestVersion = release.getVersion();

            // Prompt user
            if (promptUserForUpdate(latestVersion)) {
                updateCheckMetadata(latestVersion);
                // Perform the upgrade
                if (levainUpgradeService.upgradeToVersion(latestVersion, release, false)) {
                    System.exit(0); // Exit after initiating upgrade
                }
                return true;
            }

            // Mark that we asked, even if user declined
            updateCheckMetadata(latestVersion);
            return false;

        } catch (Exception e) {
            logger.debug("Error during update check: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if enough time has passed since we last prompted the user.
     */
    private boolean shouldPromptForUpdate() {
        String lastQuestionStr = config.getLastUpdateQuestion();
        
        if (lastQuestionStr == null || lastQuestionStr.isEmpty()) {
            return true; // Never asked before
        }

        try {
            long lastQuestionTime = Long.parseLong(lastQuestionStr);
            long now = System.currentTimeMillis();
            long elapsedHours = (now - lastQuestionTime) / (1000L * 60L * 60L);
            
            return elapsedHours >= MIN_PROMPT_INTERVAL_HOURS;
        } catch (NumberFormatException e) {
            logger.debug("Invalid lastUpdateQuestion timestamp: {}", lastQuestionStr);
            return true; // Treat invalid timestamp as "ask again"
        }
    }

    /**
     * Prompt the user to upgrade Levain.
     */
    private boolean promptUserForUpdate(String newVersion) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("New Levain version available: " + newVersion);
        System.out.println("Current version: " + getCurrentVersion());
        System.out.println("=".repeat(60));
        System.out.print("Would you like to upgrade now? (y/n) ");
        System.out.flush();

        try (Scanner scanner = new Scanner(System.in)) {
            String input = scanner.nextLine().trim().toLowerCase();
            return input.equals("y") || input.equals("yes");
        }
    }

    /**
     * Update the config metadata with the last update question timestamp.
     */
    private void updateCheckMetadata(String latestVersion) {
        try {
            config.setLastUpdateQuestion(String.valueOf(System.currentTimeMillis()));
            config.setLastKnownVersion(latestVersion);
            config.save();
            logger.debug("Updated update check metadata");
        } catch (Exception e) {
            logger.warn("Failed to save update check metadata: {}", e.getMessage());
        }
    }

    /**
     * Get the current Levain version.
     */
    private String getCurrentVersion() {
        return "2.0.0-SNAPSHOT"; // This matches LevainCommand version - could extract to constant
    }
}
