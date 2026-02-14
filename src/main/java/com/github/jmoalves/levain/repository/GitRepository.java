package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import com.github.jmoalves.levain.util.JGitProgressMonitor;

import java.io.File;
import java.io.IOException;
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
 * Uses JGit so no external git binary is required.
 *
 * The repository is cloned or pulled to a local cache directory.
 * Recipes are then loaded from the local copy.
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
    private void ensureLocalCopy() throws IOException {
        File localDir = new File(localCachePath);
        File gitDir = new File(localCachePath, ".git");

        if (!localDir.exists() || !gitDir.exists()) {
            // Delete incomplete directory if it exists
            if (localDir.exists()) {
                logger.debug("Deleting incomplete repository at {}", localCachePath);
                deleteDirectory(localDir);
            }
            logger.debug("Cloning git repository from {} to {}", gitUrl, localCachePath);
            cloneRepository(localDir);
            logger.debug("Successfully cloned repository");
        } else {
            logger.debug("Updating existing repository at {}", localCachePath);
            pullRepository(localDir);
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

    private void cloneRepository(File localDir) throws IOException {
        JGitProgressMonitor monitor = new JGitProgressMonitor();
        try {
            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(localDir)
                    .setProgressMonitor(monitor)
                    .call()
                    .close();
        } catch (GitAPIException e) {
            throw new IOException("Failed to clone git repository: " + gitUrl + ": " + e.getMessage(), e);
        } finally {
            monitor.finish();
        }
    }

    private void pullRepository(File localDir) throws IOException {
        File gitDir = new File(localDir, ".git");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        JGitProgressMonitor monitor = new JGitProgressMonitor();
        try (Repository repository = builder.setGitDir(gitDir)
                .setWorkTree(localDir)
                .readEnvironment()
                .findGitDir()
                .build();
             Git git = new Git(repository)) {
            git.pull().setProgressMonitor(monitor).call();
        } catch (GitAPIException e) {
            throw new IOException("Failed to pull git repository: " + gitUrl + ": " + e.getMessage(), e);
        } finally {
            monitor.finish();
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
