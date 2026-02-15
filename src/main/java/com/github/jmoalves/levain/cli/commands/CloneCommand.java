package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.ShellService;
import com.github.jmoalves.levain.util.GitSupport;
import com.github.jmoalves.levain.util.GitUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to clone a Git repository and open a shell inside it.
 */
@Command(name = "clone", description = "Clone a Git repository and open a shell", mixinStandardHelpOptions = true)
public class CloneCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(CloneCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(description = "Git repository URL", arity = "1")
    private String url;

    @Option(names = { "--dirs" }, description = "Create a parent directory using the repository owner")
    private boolean dirs;

    @Option(names = { "--branch" }, description = "Checkout a specific branch")
    private String branch;

    @Option(names = { "--depth" }, description = "Perform a shallow clone with the specified depth")
    private Integer depth;

    @Option(names = { "--recursive" }, description = "Clone submodules recursively")
    private boolean recursive;

    private final ShellService shellService;

    @Inject
    public CloneCommand(ShellService shellService) {
        this.shellService = shellService;
    }

    @Override
    public Integer call() {
        GitUrlUtils.ParsedGitUrl parsed = GitUrlUtils.parse(url);
        if (parsed == null) {
            console.error("Invalid git URL: {}", url);
            return 1;
        }

        String effectiveUrl = parsed.getUrl();
        String effectiveBranch = branch != null && !branch.isBlank() ? branch : parsed.getBranch();

        Path parentDir = Path.of("").toAbsolutePath();
        if (dirs) {
            String user = parsed.getUser();
            if (user == null || user.isBlank()) {
                console.error("Cannot derive owner directory from URL: {}", url);
                return 1;
            }
            parentDir = parentDir.resolve(user);
            try {
                Files.createDirectories(parentDir);
            } catch (Exception e) {
                console.error("Failed to create directory {}: {}", parentDir, e.getMessage());
                return 1;
            }
            if (!Files.isDirectory(parentDir)) {
                console.error("Invalid directory: {}", parentDir);
                return 1;
            }
        }

        Path repoDir = GitUrlUtils.deriveRepoPath(parentDir, effectiveUrl, parsed.getRepo());
        if (Files.exists(repoDir)) {
            logger.warn("Directory already exists: {}", repoDir);
        } else {
            try {
                GitSupport.cloneRepository(effectiveUrl, repoDir, effectiveBranch, depth, recursive);
            } catch (Exception e) {
                logger.error("Failed to clone repository", e);
                console.error("Failed to clone repository. See logs for details.");
                return 1;
            }
        }

        try {
            shellService.openShell(List.of(), repoDir);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to open shell", e);
            console.error("Failed to open shell. See logs for details.");
            return 1;
        }
    }
}
