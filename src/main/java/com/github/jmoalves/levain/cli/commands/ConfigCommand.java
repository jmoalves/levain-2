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
        ConfigCommand.RepoCommand.class
})
public class ConfigCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config --help' to see available subcommands");
        return 0;
    }

    /**
     * Subcommand to manage repositories.
     */
    @Command(name = "repo", description = "Manage repositories", mixinStandardHelpOptions = true, subcommands = {
            ConfigCommand.RepoCommand.AddCommand.class,
            ConfigCommand.RepoCommand.ListCommand.class,
            ConfigCommand.RepoCommand.RemoveCommand.class
    })
    public static class RepoCommand implements Callable<Integer> {
        private static final Logger console = LoggerFactory.getLogger("CONSOLE");

        @Override
        public Integer call() {
            console.info("Use 'levain config repo --help' to see available subcommands");
            return 0;
        }

        /**
         * Subcommand to add a repository to the configuration.
         */
        @Command(name = "add", description = "Add a repository to the configuration", mixinStandardHelpOptions = true)
        public static class AddCommand implements Callable<Integer> {
            private static final Logger logger = LoggerFactory.getLogger(AddCommand.class);
            private static final Logger console = LoggerFactory.getLogger("CONSOLE");

            @Parameters(index = "0", description = "Repository URI (git, directory, zip, or http URL)")
            private String repositoryUri;

            @Parameters(index = "1", arity = "0..1", description = "Repository name (optional)")
            private String name;

            private final ConfigService configService;

            @Inject
            public AddCommand(ConfigService configService) {
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

        /**
         * Subcommand to list configured repositories.
         */
        @Command(name = "list", description = "List configured repositories", mixinStandardHelpOptions = true)
        public static class ListCommand implements Callable<Integer> {
            private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);
            private static final Logger console = LoggerFactory.getLogger("CONSOLE");

            private final ConfigService configService;

            @Inject
            public ListCommand(ConfigService configService) {
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

        /**
         * Subcommand to remove a repository from the configuration.
         */
        @Command(name = "remove", description = "Remove a repository from the configuration", mixinStandardHelpOptions = true)
        public static class RemoveCommand implements Callable<Integer> {
            private static final Logger logger = LoggerFactory.getLogger(RemoveCommand.class);
            private static final Logger console = LoggerFactory.getLogger("CONSOLE");

            @Parameters(description = "Repository name or URI to remove")
            private String identifier;

            private final ConfigService configService;

            @Inject
            public RemoveCommand(ConfigService configService) {
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
    }
}
