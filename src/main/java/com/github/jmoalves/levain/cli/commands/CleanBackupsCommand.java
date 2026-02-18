package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.clean.CleanService;
import com.github.jmoalves.levain.service.clean.CleanService.CleanupResult;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Clean up old backups.
 * Usage: levain clean backups [package] [--older-than=days] [--keep=count] [--dry-run] [-f]
 */
@Command(
    name = "backups",
    description = "Clean up old package backups"
)
public class CleanBackupsCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");
    
    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Package name (optional, cleans all if omitted)"
    )
    private String packageName;
    
    @Option(
        names = {"--older-than"},
        description = "Delete backups older than N days (default: 30)",
        defaultValue = "30"
    )
    private int olderThanDays;
    
    @Option(
        names = {"--keep"},
        description = "Keep at least N most recent backups per package (default: 5)",
        defaultValue = "5"
    )
    private int keepCount;
    
    @Option(
        names = {"--dry-run"},
        description = "Preview what would be deleted without actually deleting"
    )
    private boolean dryRun = false;
    
    @Option(
        names = {"-f", "--force"},
        description = "Skip confirmation prompts"
    )
    private boolean force = false;
    
    private final CleanService cleanService;
    
    @Inject
    public CleanBackupsCommand(CleanService cleanService) {
        this.cleanService = cleanService;
    }
    
    @Override
    public Integer call() {
        try {
            // First, preview what would be cleaned
            console.info("Scanning backups...");
            CleanupResult preview = cleanService.previewCleanup(packageName, olderThanDays, keepCount);
            
            if (preview.toDelete.isEmpty()) {
                console.info("✓ No backups match cleanup criteria");
                return 0;
            }
            
            // Show preview
            displayPreview(preview);
            
            // If dry-run, stop here
            if (dryRun) {
                console.info("(Dry-run mode - no backups deleted)");
                return 0;
            }
            
            // Ask for confirmation unless force is set
            if (!force && !confirmCleanup()) {
                console.info("Cleanup cancelled");
                return 0;
            }
            
            // Execute cleanup
            console.info("Deleting backups...");
            CleanupResult result = cleanService.executeCleanup(packageName, olderThanDays, keepCount);
            
            // Show results
            displayResults(result);
            
            return 0;
        } catch (Exception e) {
            console.error("✗ Cleanup failed: {}", e.getMessage());
            return 1;
        }
    }
    
    private void displayPreview(CleanupResult preview) {
        console.info("");
        console.info("Will delete {} backup(s):", preview.toDelete.size());
        preview.toDelete.forEach(item -> 
            console.info("  • {}/{} ({})", item.packageName, item.backup.timestampStr(), formatSize(item.backup.size()))
        );
        console.info("");
        long totalSize = preview.getTotalSizeToDelete();
        console.info("Total size to reclaim: {}", formatSize(totalSize));
        console.info("Estimated free space: {}", formatSize(totalSize));
    }
    
    private void displayResults(CleanupResult result) {
        console.info("");
        int successful = result.getSuccessfulDeletions();
        int failed = result.getFailedDeletions();
        
        if (successful > 0) {
            console.info("✓ Deleted {} backup(s), freed {} of space", 
                successful, formatSize(result.getTotalSizeToDelete()));
        }
        if (failed > 0) {
            console.error("✗ Failed to delete {} backup(s)", failed);
            result.toDelete.stream()
                .filter(item -> item.deleteFailed)
                .forEach(item -> 
                    console.error("  • {}/{}", item.packageName, item.backup.timestampStr())
                );
        }
        console.info("");
    }
    
    private boolean confirmCleanup() {
        console.info("Proceed with cleanup? [y/N] ");
        try (Scanner scanner = new Scanner(System.in)) {
            String response = scanner.nextLine().trim().toLowerCase();
            return response.equals("y") || response.equals("yes");
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", 
            bytes / Math.pow(1024, digitGroups), 
            units[digitGroups]);
    }
}
