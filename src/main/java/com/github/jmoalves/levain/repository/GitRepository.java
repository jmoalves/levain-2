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

    /**
     * Ensure we have a local copy of the git repository.
     * Clones if not present, otherwise pulls latest changes.
     */
    private void ensureLocalCopy() throws IOException, InterruptedException {
        File localDir = new File(localCachePath);

        if (!localDir.exists()) {
            logger.debug("Cloning git repository from {} to {}", gitUrl, localCachePath);
            executeGitCommand("clone", gitUrl, localCachePath);
            logger.debug("Successfully cloned repository");
        } else {
            logger.debug("Updating existing repository at {}", localCachePath);
            executeGitCommand("pull", "-C", localCachePath);
            logger.debug("Successfully pulled latest changes");
        }

        // Create directory repository for the local copy
        this.localRepository = new DirectoryRepository("LocalGitRepository", localCachePath);
    }

    /**
     * Execute a git command and check for success.
     */
    private void executeGitCommand(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("git: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Git command failed with exit code: " + exitCode);
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
