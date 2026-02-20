package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.InstallService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to install packages.
 */
@Command(name = "install", description = "Install one or more packages", mixinStandardHelpOptions = true)
public class InstallCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(InstallCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(description = "Package(s) to install")
    private String[] packages;

    @Option(names = { "--force", "-f" }, description = "Reinstall package even if already installed")
    private boolean force = false;

    @Option(names = { "--noUpdate" }, description = "Skip checking for updates to already installed packages")
    private boolean noUpdate = false;

    private final InstallService installService;

    @Inject
    public InstallCommand(InstallService installService) {
        this.installService = installService;
    }

    @Override
    public Integer call() {
        if (packages == null || packages.length == 0) {
            console.error("At least one package name is required");
            return 1;
        }

        logger.debug("Installing {} packages", packages.length);
        try {
            List<String> requested = List.of(packages);
            List<String> updatePackages = List.of();
            if (!force && !noUpdate) {
                updatePackages = installService.findUpdates(requested);
                if (!updatePackages.isEmpty()) {
                    console.info("\nPackage updates available:");
                    for (String name : updatePackages) {
                        console.info("  - {}", name);
                    }
                    if (!confirmUpdate()) {
                        updatePackages = List.of();
                    }
                }
            }

            InstallService.PlanResult result = installService.buildInstallationPlan(requested, force, updatePackages);
            List<Recipe> plan = result.plan();
            boolean hasPlanOutput = !plan.isEmpty() || !result.alreadyInstalled().isEmpty();
            if (!hasPlanOutput && result.missing().isEmpty()) {
                console.info("All packages already installed");
                return 0;
            }

            if (hasPlanOutput) {
                console.info("\n" + installService.formatInstallationPlan(result, requested));
            }

            if (!result.missing().isEmpty()) {
                console.error("\nMissing recipes:");
                for (String missing : result.missing()) {
                    console.error("  ✗ {}", missing);
                }
                return 1;
            }

            if (!plan.isEmpty()) {
                installService.installPlan(plan);
                console.info("\nAll packages installed successfully!");
            } else {
                console.info("\nAll packages already installed");
            }
            return 0;
        } catch (Exception e) {
            logger.error("Failed to install packages", e);
            console.error("✗ Failed to install packages. See logs for details. Hint: check network/proxy and permissions.");
            return 1;
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
