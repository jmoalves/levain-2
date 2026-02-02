package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from a Git repository.
 * Uses git command-line interface (must be available on system PATH).
 * 
 * The repository is cloned or pulled to a local cache directory.
 * Recipes are then loaded from the local copy.
 * 
 * NOTE: This implementation requires git to be installed and available on PATH.
 * This is intentional - it keeps the application lightweight and portable.
 * If git is not available, the repository will fail gracefully with a clear
 * error message.
 */
@Dependent
public class GitRepository extends AbstractRepository {
    private static final Logger logger = LogManager.getLogger(GitRepository.class);
    private final String gitUrl;
    private final String localCachePath;
    private DirectoryRepository localRepository;
    private Map<String, Recipe> recipes = Collections.emptyMap();

    public GitRepository(String gitUrl) {
        super("GitRepository", gitUrl);
        this.gitUrl = gitUrl;
        this.localCachePath = getDefaultCacheDirectory(gitUrl);
    }

    @Override
    public void init() {
        logger.debug("Initializing GitRepository from: {}", gitUrl);
        try {
            ensureLocalCopy();
            if (localRepository != null) {
                localRepository.init();
                this.recipes = loadRecipesFromLocalRepository();
                setInitialized();
                logger.info("GitRepository initialized with {} recipes from {}", recipes.size(), gitUrl);
            } else {
                logger.error("Failed to load recipes from git repository: {}", gitUrl);
                setInitialized();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize GitRepository from {}: {}", gitUrl, e.getMessage(), e);
            setInitialized();
        }
    }

    @Override
    public List<Recipe> listRecipes() {
        return List.copyOf(recipes.values());
    }

    @Override
    public Optional<Recipe> resolveRecipe(String recipeName) {
        return Optional.ofNullable(recipes.get(recipeName));
    }

    @Override
    public Optional<String> getRecipeYamlContent(String recipeName) {
        // Delegate to the local directory repository
        if (localRepository != null) {
            return localRepository.getRecipeYamlContent(recipeName);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getRecipeFileName(String recipeName) {
        // All recipes use standardized .levain.yaml extension
        // Validate recipe name doesn't contain .levain.yaml
        if (recipeName.contains(".levain.yaml")) {
            logger.warn("Recipe name contains .levain.yaml: {}", recipeName);
            return Optional.empty();
        }
        return Optional.of(recipeName + ".levain.yaml");
    }

    /**
     * Ensure we have a local copy of the git repository.
     * Clones if not present, otherwise pulls latest changes.
     */
    private void ensureLocalCopy() throws IOException, InterruptedException {
        File localDir = new File(localCachePath);
        File gitDir = new File(localCachePath, ".git");

        if (!localDir.exists() || !gitDir.exists()) {
            // Delete incomplete directory if it exists
            if (localDir.exists()) {
                logger.debug("Deleting incomplete repository at {}", localCachePath);
                deleteDirectory(localDir);
            }
            logger.debug("Cloning git repository from {} to {}", gitUrl, localCachePath);
            executeGitCommand(null, "clone", gitUrl, localCachePath);
            logger.debug("Successfully cloned repository");
        } else {
            logger.debug("Updating existing repository at {}", localCachePath);
            executeGitCommand(localCachePath, "pull");
            logger.debug("Successfully pulled latest changes");
        }

        // Create directory repository for the local copy
        this.localRepository = new DirectoryRepository("LocalGitRepository", localCachePath);
    }

    /**
     * Recursively delete a directory.
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * Execute a git command and check for success.
     * 
     * @param workingDirectory Optional working directory for the git command (null
     *                         for current directory)
     * @param args             The git command arguments (excluding 'git')
     */
    private void executeGitCommand(String workingDirectory, String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        logger.debug("Executing git command: {}", String.join(" ", command));
        if (workingDirectory != null) {
            logger.debug("Working directory: {}", workingDirectory);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDirectory != null) {
            pb.directory(new File(workingDirectory));
        }
        // Ensure git can find its config
        Map<String, String> env = pb.environment();
        if (!env.containsKey("HOME")) {
            env.put("HOME", System.getProperty("user.home"));
        }

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("git: {}", line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorMessage = String.format("Git command failed with exit code: %d\nCommand: %s\nOutput: %s",
                    exitCode, String.join(" ", command), output.toString());
            logger.error(errorMessage);
            throw new IOException(errorMessage);
        }
    }

    /**
     * Load recipes from the local repository copy.
     */
    private Map<String, Recipe> loadRecipesFromLocalRepository() {
        if (localRepository == null) {
            return Collections.emptyMap();
        }
        return localRepository.listRecipes().stream()
                .collect(Collectors.toMap(Recipe::getName, r -> r));
    }

    /**
     * Get the default cache directory for this git repository.
     * Uses a hash of the git URL to create a unique directory.
     */
    private String getDefaultCacheDirectory(String url) {
        String cacheDir = System.getProperty("levain.cache.dir");
        if (cacheDir == null) {
            cacheDir = System.getenv("LEVAIN_CACHE_DIR");
        }
        if (cacheDir == null) {
            cacheDir = Paths.get(System.getProperty("user.home"), ".levain", "cache").toString();
        }

        // Use hash of the URL to create a unique directory name
        String repoName = String.valueOf(Math.abs(url.hashCode()));
        Path repositoryPath = Paths.get(cacheDir, "git", repoName);

        try {
            Files.createDirectories(repositoryPath);
        } catch (IOException e) {
            logger.warn("Failed to create cache directory: {}", repositoryPath);
        }

        return repositoryPath.toString();
    }

    @Override
    public String describe() {
        if (localRepository != null) {
            return String.format("GitRepository (%s -> %s)", gitUrl, localCachePath);
        }
        return String.format("GitRepository (%s)", gitUrl);
    }
}
