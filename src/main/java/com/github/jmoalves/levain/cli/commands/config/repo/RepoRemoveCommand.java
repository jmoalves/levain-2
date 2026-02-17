package com.github.jmoalves.levain.cli.commands.config.repo;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ConfigService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcommand to remove a repository from the configuration.
 */
@Command(name = "remove", description = "Remove a repository from the configuration", mixinStandardHelpOptions = true)
public class RepoRemoveCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RepoRemoveCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(description = "Repository name or URI to remove")
    private String identifier;

    private final ConfigService configService;

    @Inject
    public RepoRemoveCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            boolean removed = configService.removeRepository(identifier);
            if (removed) {
                console.info("✓ Repository removed: {}", identifier);
                return 0;
            } else {
                console.info("✗ Repository not found: {}", identifier);
                return 1;
            }
        } catch (Exception e) {
            logger.error("Failed to remove repository: {}", identifier, e);
            console.error("✗ Failed to remove repository. See logs for details. Hint: verify the name or URI.");
            return 1;
        }
    }
}
