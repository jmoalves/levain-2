package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            var plan = installService.buildInstallationPlan(List.of(packages), force);
            if (plan.isEmpty()) {
                console.info("All packages already installed");
                return 0;
            }

            console.info("\n" + installService.formatInstallationPlan(plan));
            installService.installPlan(plan);
            console.info("\nAll packages installed successfully!");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to install packages", e);
            console.error("✗ Failed to install packages. See logs for details. Hint: check network/proxy and permissions.");
            return 1;
        }
    }
}
