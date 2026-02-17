package com.github.jmoalves.levain.cli.commands.config.repo;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ConfigService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcommand to add a repository to the configuration.
 */
@Command(name = "add", description = "Add a repository to the configuration", mixinStandardHelpOptions = true)
public class RepoAddCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RepoAddCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(index = "0", description = "Repository URI (git, directory, zip, or http URL)")
    private String repositoryUri;

    @Parameters(index = "1", arity = "0..1", description = "Repository name (optional)")
    private String name;

    private final ConfigService configService;

    @Inject
    public RepoAddCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Adding repository: {} with name: {}", repositoryUri, name);
            configService.addRepository(repositoryUri, name);
            console.info("✓ Repository added: {}", repositoryUri);
            if (name != null) {
                console.info("  Name: {}", name);
            }
            return 0;
        } catch (Exception e) {
            logger.error("Failed to add repository: {}", repositoryUri, e);
            console.error("✗ Failed to add repository. See logs for details. Hint: check the URI and network/proxy.");
            return 1;
        }
    }
}
