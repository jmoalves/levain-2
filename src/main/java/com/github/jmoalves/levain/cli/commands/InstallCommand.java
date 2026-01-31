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
@Command(
    name = "install",
    description = "Install one or more packages",
    mixinStandardHelpOptions = true
)
public class InstallCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(InstallCommand.class);

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
            System.out.println("Installing package: " + pkg);
            try {
                installService.install(pkg);
                System.out.println("  ✓ " + pkg + " installed successfully");
            } catch (Exception e) {
                logger.error("Failed to install package: {}", pkg, e);
                System.err.println("  ✗ Failed to install " + pkg + ": " + e.getMessage());
                return 1;
            }
        }
        
        System.out.println("\nAll packages installed successfully!");
        return 0;
    }
}
