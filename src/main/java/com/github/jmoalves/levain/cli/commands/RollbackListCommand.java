package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.rollback.RollbackService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * List available backups for a package or all packages.
 * Usage: levain rollback list [package-name]
 */
@Command(
    name = "list",
    description = "List available backups for packages"
)
public class RollbackListCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");
    
    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Package name (optional, shows all if omitted)"
    )
    private String packageName;
    
    @Option(
        names = {"-v", "--verbose"},
        description = "Show detailed backup information"
    )
    private boolean verbose = false;
    
    private final RollbackService rollbackService;
    
    @Inject
    public RollbackListCommand(RollbackService rollbackService) {
        this.rollbackService = rollbackService;
    }
    
    @Override
    public Integer call() {
        try {
            if (packageName != null) {
                listPackageBackups(packageName);
            } else {
                listAllBackups();
            }
            return 0;
        } catch (Exception e) {
            console.error("✗ Failed to list backups: {}", e.getMessage());
            return 1;
        }
    }
    
    private void listPackageBackups(String packageName) {
        List<RollbackService.BackupInfo> backups = 
            rollbackService.listBackups(packageName);
        
        if (backups.isEmpty()) {
            console.info("No backups found for package: {}", packageName);
            return;
        }
        
        console.info("Available backups for package '{}':", packageName);
        console.info("");
        
        for (int i = 0; i < backups.size(); i++) {
            RollbackService.BackupInfo backup = backups.get(i);
            String marker = (i == 0) ? "→ " : "  ";
            console.info("{}[{}] {} ({})", 
                marker,
                i + 1,
                backup.timestampStr(),
                formatSize(backup.size())
            );
            
            if (verbose) {
                console.info("    Timestamp: {}", backup.timestamp());
                console.info("    Size: {} bytes", backup.size());
            }
        }
        
        console.info("");
        console.info("Use 'levain rollback restore {} <timestamp>' to restore",
            packageName);
    }
    
    private void listAllBackups() {
        Map<String, List<RollbackService.BackupInfo>> allBackups = 
            rollbackService.listAllBackups();
        
        if (allBackups.isEmpty()) {
            console.info("No backups found");
            return;
        }
        
        console.info("Available backups:");
        console.info("");
        
        allBackups.forEach((packageName, backups) -> {
            console.info("Package: {}", packageName);
            for (int i = 0; i < backups.size(); i++) {
                RollbackService.BackupInfo backup = backups.get(i);
                String marker = (i == 0) ? "→ " : "  ";
                console.info("  {}[{}] {} ({})",
                    marker,
                    i + 1,
                    backup.timestampStr(),
                    formatSize(backup.size())
                );
                
                if (verbose) {
                    console.info("      Timestamp: {}", backup.timestamp());
                    console.info("      Size: {} bytes", backup.size());
                }
            }
            console.info("");
        });
    }
    
    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", 
            bytes / Math.pow(1024, digitGroups), 
            units[digitGroups]);
    }
}
