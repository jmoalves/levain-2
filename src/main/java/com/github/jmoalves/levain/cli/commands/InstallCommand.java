package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.InstallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to install packages.
 */
@Command(name = "install", description = "Install one or more packages", mixinStandardHelpOptions = true)
public class InstallCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(InstallCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "1..*", description = "Package(s) to install")
    private List<String> packages;

    private final InstallService installService;

    public InstallCommand() {
        this.installService = new InstallService();
    }

    @Override
    public Integer call() {
        logger.info("Installing packages: {}", packages);

        for (String pkg : packages) {
            console.info("Installing package: " + pkg);
            try {
                installService.install(pkg);
                console.info("  ✓ " + pkg + " installed successfully");
            } catch (Exception e) {
                logger.error("Failed to install package: {}", pkg, e);
                console.error("  ✗ Failed to install " + pkg + ": " + e.getMessage());
                return 1;
            }
        }

        console.info("\nAll packages installed successfully!");
        return 0;
    }
}
