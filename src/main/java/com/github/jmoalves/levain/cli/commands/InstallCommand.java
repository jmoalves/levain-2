package com.github.jmoalves.levain.cli.commands;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.InstallService;
import com.github.jmoalves.levain.service.AlreadyInstalledException;

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

        for (String pkg : packages) {
            console.info("Installing package: {}", pkg);
            try {
                // Pass --force flag to install service
                installService.install(pkg, force);
                console.info("  ✓ {} installed successfully", pkg);
            } catch (AlreadyInstalledException e) {
                console.info("  ℹ {} already installed (use --force to reinstall)", pkg);
            } catch (Exception e) {
                logger.error("Failed to install package: {}", pkg, e);
                console.error("  ✗ Failed to install {}: {}", pkg, e.getMessage());
                return 1;
            }
        }

        console.info("\nAll packages installed successfully!");
        return 0;
    }
}
