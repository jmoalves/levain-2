package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.RecipeLoader;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from a remote HTTP/HTTPS source.
 * 
 * Supports loading from:
 * - A directory listing endpoint that returns recipe files
 * - A GitHub repository (converts to raw content URLs)
 * 
 * Uses Java's built-in HttpClient (no external download tools needed).
 */
@Dependent
public class RemoteRepository extends AbstractRepository {
    private static final Logger logger = LogManager.getLogger(RemoteRepository.class);
    private static final int HTTP_TIMEOUT_SECONDS = 30;
    private final String remoteUrl;
    private final String localCachePath;
    private Map<String, Recipe> recipes = Collections.emptyMap();
    private final HttpClient httpClient;

    public RemoteRepository(String remoteUrl) {
        super("RemoteRepository", remoteUrl);
        this.remoteUrl = remoteUrl;
        this.localCachePath = getDefaultCacheDirectory(remoteUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
    }

    @Override
    public void init() {
        logger.debug("Initializing RemoteRepository from: {}", remoteUrl);
        try {
            this.recipes = downloadAndLoadRecipes();
            setInitialized();
            logger.info("RemoteRepository initialized with {} recipes from {}", recipes.size(), remoteUrl);
        } catch (Exception e) {
            logger.error("Failed to initialize RemoteRepository from {}: {}", remoteUrl, e.getMessage(), e);
            setInitialized(); // Mark as initialized even if empty
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
     * Download and load recipes from the remote source.
     * 
     * Supports multiple formats:
     * - GitHub repos: https://github.com/owner/repo -> loads recipes/
     * - Direct HTTP: https://example.com/recipes/ -> loads all .levain* files
     */
    private Map<String, Recipe> downloadAndLoadRecipes() throws IOException, InterruptedException {
        String repositoryUrl = normalizeUrl(remoteUrl);
        logger.debug("Loading recipes from remote URL: {}", repositoryUrl);

        Map<String, Recipe> loadedRecipes = Collections.emptyMap();

        // Try to download recipes from the remote location
        String[] recipeFiles = getRecipeFileList(repositoryUrl);

        for (String recipeFile : recipeFiles) {
            String recipeUrl = repositoryUrl.endsWith("/")
                    ? repositoryUrl + recipeFile
                    : repositoryUrl + "/" + recipeFile;

            try {
                Recipe recipe = downloadRecipe(recipeUrl);
                if (recipe != null) {
                    if (loadedRecipes.isEmpty()) {
                        loadedRecipes = new java.util.LinkedHashMap<>();
                    }
                    loadedRecipes.put(recipe.getName(), recipe);
                    logger.debug("Loaded recipe: {} from {}", recipe.getName(), recipeUrl);
                }
            } catch (Exception e) {
                logger.warn("Failed to load recipe from {}: {}", recipeUrl, e.getMessage());
            }
        }

        return loadedRecipes;
    }

    /**
     * Download and parse a recipe from a remote URL.
     */
    private Recipe downloadRecipe(String recipeUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(recipeUrl))
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Failed to download recipe from {}: HTTP {}", recipeUrl, response.statusCode());
            return null;
        }

        try {
            return RecipeLoader.parseRecipeYaml(response.body(), extractRecipeName(recipeUrl));
        } catch (Exception e) {
            logger.warn("Failed to parse recipe from {}: {}", recipeUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Get list of recipe files available at the remote location.
     * This is a simple implementation that searches for common recipe file
     * patterns.
     */
    private String[] getRecipeFileList(String repositoryUrl) {
        // Common recipe file names and patterns
        return new String[] {
                "levain.levain.yaml",
                "levain.levain.yml",
                "recipes/levain.levain.yaml",
                "jdk-21.levain.yaml",
                "jdk-11.levain.yaml",
                "git.levain.yaml",
                "maven.levain.yaml",
                "gradle.levain.yaml",
                "nodejs.levain.yaml",
                "python.levain.yaml",
                "rust.levain.yaml"
        };
    }

    /**
     * Normalize a GitHub URL to a raw content URL if needed.
     */
    private String normalizeUrl(String url) {
        // Convert GitHub web URLs to raw content URLs
        if (url.contains("github.com") && !url.contains("raw.githubusercontent.com")) {
            // https://github.com/owner/repo/tree/branch/path ->
            // https://raw.githubusercontent.com/owner/repo/branch/path
            String normalized = url
                    .replace("github.com", "raw.githubusercontent.com")
                    .replace("/tree/", "/")
                    .replace("/blob/", "/");
            if (!normalized.endsWith("/recipes")) {
                normalized = normalized + "/recipes";
            }
            return normalized;
        }

        // Ensure trailing slash for directory URLs
        if (!url.endsWith("/")) {
            url = url + "/recipes";
        } else if (!url.endsWith("/recipes/")) {
            url = url + "recipes/";
        }

        return url;
    }

    /**
     * Extract recipe name from URL.
     */
    private String extractRecipeName(String url) {
        String filename = url.substring(url.lastIndexOf('/') + 1);
        return filename.replaceAll("\\.levain(\\.ya?ml)?$", "");
    }

    /**
     * Get the default cache directory for this remote repository.
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
        String remoteId = String.valueOf(Math.abs(url.hashCode()));
        Path repositoryPath = Paths.get(cacheDir, "remote", remoteId);

        try {
            Files.createDirectories(repositoryPath);
        } catch (IOException e) {
            logger.warn("Failed to create cache directory: {}", repositoryPath);
        }

        return repositoryPath.toString();
    }
}
