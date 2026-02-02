package com.github.jmoalves.levain.cli.commands;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.InstallService;
import com.github.jmoalves.levain.cli.LevainCommand;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Command to install packages.
 */
@Command(name = "install", description = "Install one or more packages", mixinStandardHelpOptions = true)
public class InstallCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(InstallCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(description = "Package(s) to install")
    private String[] packages;

    @ParentCommand
    private LevainCommand parent;

    @Spec
    private CommandSpec spec;

    private final InstallService installService;

    @Inject
    public InstallCommand(InstallService installService) {
        this.installService = installService;
    }

    @Override
    public Integer call() {
        logger.info("Installing packages: {}", (Object[]) packages);

        if (packages == null || packages.length == 0) {
            console.error("At least one package name is required");
            return 1;
        }

        // Get --addRepo from parent command using spec
        String[] addRepos = null;
        if (spec.parent() != null) {
            CommandSpec parentSpec = spec.parent();
            Object addRepoOption = parentSpec.findOption("--addRepo");
            if (addRepoOption != null) {
                addRepos = parentSpec.findOption("--addRepo").getValue();
            }
        }

        // Fallback to @ParentCommand if it was populated
        if (addRepos == null && parent != null) {
            addRepos = parent.getAddRepo();
        }

        logger.info("AddRepos: {}", (Object[]) addRepos);

        for (String pkg : packages) {
            console.info("Installing package: {}", pkg);
            try {
                // If --addRepo was specified, use the first repository
                if (addRepos != null && addRepos.length > 0) {
                    logger.info("Using repository: {}", addRepos[0]);
                    installService.install(pkg, addRepos[0]);
                } else {
                    logger.info("Using default repositories");
                    installService.install(pkg);
                }
                console.info("  ✓ {} installed successfully", pkg);
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
