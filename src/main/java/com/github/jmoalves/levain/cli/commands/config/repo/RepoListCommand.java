package com.github.jmoalves.levain.cli.commands.config.repo;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ConfigService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

/**
 * Subcommand to list configured repositories.
 */
@Command(name = "list", description = "List configured repositories", mixinStandardHelpOptions = true)
public class RepoListCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RepoListCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    private final ConfigService configService;

    @Inject
    public RepoListCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var repos = configService.getRepositories();

            // Show repository search order
            console.info("Repository search order (by priority):");
            console.info("  1. Built-in recipes (levain.jar) - cannot be overridden");

            if (repos.isEmpty()) {
                console.info("  2. No additional repositories configured.");
                console.info("");
                console.info("Use 'levain config repo add <uri>' to add a repository.");
            } else {
                for (int i = 0; i < repos.size(); i++) {
                    var repo = repos.get(i);
                    console.info("  {}. {} ({})", i + 2, repo.getName(), repo.getUri());
                }
            }
            return 0;
        } catch (Exception e) {
            logger.error("Failed to list repositories", e);
            console.error("✗ Failed to list repositories. See logs for details.");
            return 1;
        }
    }
}
