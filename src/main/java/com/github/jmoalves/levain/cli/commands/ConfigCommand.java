package com.github.jmoalves.levain.cli.commands;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ConfigService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to manage Levain configuration.
 */
@Command(name = "config", description = "Manage Levain configuration", mixinStandardHelpOptions = true, subcommands = {
        ConfigCommand.AddRepoCommand.class,
        ConfigCommand.ListReposCommand.class,
        ConfigCommand.RemoveRepoCommand.class
})
public class ConfigCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config --help' to see available subcommands");
        return 0;
    }

    /**
     * Subcommand to add a repository to the configuration.
     */
    @Command(name = "add-repo", description = "Add a repository to the configuration")
    public static class AddRepoCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(AddRepoCommand.class);
        private static final Logger console = LoggerFactory.getLogger("CONSOLE");

        @Parameters(index = "0", description = "Repository URI (git, directory, zip, or http URL)")
        private String repositoryUri;

        @Parameters(index = "1", arity = "0..1", description = "Repository name (optional)")
        private String name;

        private final ConfigService configService;

        @Inject
        public AddRepoCommand(ConfigService configService) {
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
                console.error("✗ Failed to add repository: {}", e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Subcommand to list configured repositories.
     */
    @Command(name = "list-repos", description = "List configured repositories")
    public static class ListReposCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(ListReposCommand.class);
        private static final Logger console = LoggerFactory.getLogger("CONSOLE");

        private final ConfigService configService;

        @Inject
        public ListReposCommand(ConfigService configService) {
            this.configService = configService;
        }

        @Override
        public Integer call() {
            try {
                var repos = configService.getRepositories();
                if (repos.isEmpty()) {
                    console.info("No repositories configured.");
                    console.info("Use 'levain config add-repo <uri>' to add a repository.");
                } else {
                    console.info("Configured repositories:");
                    for (var repo : repos) {
                        console.info("  • {} ({})", repo.getName(), repo.getUri());
                    }
                }
                return 0;
            } catch (Exception e) {
                logger.error("Failed to list repositories", e);
                console.error("✗ Failed to list repositories: {}", e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Subcommand to remove a repository from the configuration.
     */
    @Command(name = "remove-repo", description = "Remove a repository from the configuration")
    public static class RemoveRepoCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(RemoveRepoCommand.class);
        private static final Logger console = LoggerFactory.getLogger("CONSOLE");

        @Parameters(description = "Repository name or URI to remove")
        private String identifier;

        private final ConfigService configService;

        @Inject
        public RemoveRepoCommand(ConfigService configService) {
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
                console.error("✗ Failed to remove repository: {}", e.getMessage());
                return 1;
            }
        }
    }
}
