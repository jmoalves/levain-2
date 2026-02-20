package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ShellService;
import com.github.jmoalves.levain.service.InstallService;
import com.github.jmoalves.levain.config.Config;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to open a configured shell with specified packages.
 */
@Command(name = "shell", description = "Open a configured shell with specified packages", mixinStandardHelpOptions = true)
public class ShellCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "0..*", description = "Package(s) to include in shell environment")
    private List<String> packages;

    private final ShellService shellService;
    private final InstallService installService;
    private final Config config;

    @Inject
    public ShellCommand(ShellService shellService, InstallService installService, Config config) {
        this.shellService = shellService;
        this.installService = installService;
        this.config = config;
    }

    @Override
    public Integer call() {
        logger.info("Opening shell with packages: {}", packages);

        try {
            List<String> requested = packages != null ? packages : List.of();
            if (config.isShellCheckForUpdate() && !requested.isEmpty()) {
                handleUpdateCheck(requested);
            }

            shellService.openShell(requested);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to open shell", e);
            console.error("Failed to open shell. See logs for details. Hint: check your shell path and permissions.");
            return 1;
        }
    }

    private void handleUpdateCheck(List<String> requested) {
        List<String> updatePackages = installService.findUpdates(requested);
        if (updatePackages.isEmpty()) {
            return;
        }

        console.info("\nPackage updates available:");
        for (String name : updatePackages) {
            console.info("  - {}", name);
        }

        if (!confirmUpdate()) {
            return;
        }

        InstallService.PlanResult result = installService.buildInstallationPlan(requested, false, updatePackages);
        if (!result.missing().isEmpty()) {
            console.error("\nMissing recipes:");
            for (String missing : result.missing()) {
                console.error("  ✗ {}", missing);
            }
            return;
        }

        if (!result.plan().isEmpty()) {
            console.info("\n" + installService.formatInstallationPlan(result, requested));
            installService.installPlan(result.plan());
            console.info("\nAll packages installed successfully!");
        }
    }

    private boolean confirmUpdate() {
        console.info("Proceed with update? [Y/n] ");
        try (Scanner scanner = new Scanner(System.in)) {
            String response = scanner.nextLine().trim();
            return response.isBlank() || response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes");
        }
    }
}
