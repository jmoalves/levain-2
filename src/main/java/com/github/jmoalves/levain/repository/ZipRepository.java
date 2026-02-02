package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

/**
 * Repository that loads recipes from ZIP archive files.
 * Uses Java's built-in zip handling (no external tools needed).
 * 
 * The ZIP archive is extracted to a temporary directory and recipes are loaded
 * from there.
 */
@Dependent
public class ZipRepository extends AbstractRepository {
    private static final Logger logger = LogManager.getLogger(ZipRepository.class);
    private final String zipPath;
    private final String localCachePath;
    private DirectoryRepository localRepository;
    private Map<String, Recipe> recipes = Collections.emptyMap();

    public ZipRepository(String zipPath) {
        super("ZipRepository", zipPath);
        this.zipPath = zipPath;
        this.localCachePath = getDefaultCacheDirectory(zipPath);
    }

    @Override
    public void init() {
        logger.debug("Initializing ZipRepository from: {}", zipPath);
        try {
            extractZipArchive();
            if (localRepository != null) {
                localRepository.init();
                this.recipes = loadRecipesFromLocalDirectory();
                setInitialized();
                logger.info("ZipRepository initialized with {} recipes from {}", recipes.size(), zipPath);
            } else {
                logger.error("Failed to load recipes from zip: {}", zipPath);
                setInitialized();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize ZipRepository from {}: {}", zipPath, e.getMessage(), e);
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
     * Extract the ZIP archive to a local cache directory.
     * Only extracts if not already present.
     */
    private void extractZipArchive() throws IOException {
        File localDir = new File(localCachePath);
        File zipFile = resolveZipFile();

        if (!zipFile.exists()) {
            throw new IOException("ZIP file not found: " + zipPath);
        }

        // Check if already extracted
        if (localDir.exists() && localDir.isDirectory() && localDir.listFiles() != null
                && localDir.listFiles().length > 0) {
            logger.debug("ZIP archive already extracted at {}", localCachePath);
        } else {
            logger.debug("Extracting ZIP archive from {} to {}", zipPath, localCachePath);
            Files.createDirectories(localDir.toPath());
            extractZip(zipFile, localDir);
            logger.debug("Successfully extracted ZIP archive");
        }

        // Create directory repository for the extracted content
        this.localRepository = new DirectoryRepository("LocalZipRepository", localCachePath);
    }

    /**
     * Extract a ZIP file to a directory.
     */
    private void extractZip(File zipFile, File targetDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            zip.entries().asIterator().forEachRemaining(entry -> {
                try {
                    Path targetPath = targetDir.toPath().resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        try (InputStream inputStream = zip.getInputStream(entry);
                                FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to extract entry {}: {}", entry.getName(), e.getMessage());
                }
            });
        }
    }

    /**
     * Resolve the ZIP file path (local file or remote URL).
     */
    private File resolveZipFile() throws IOException {
        if (zipPath.startsWith("http://") || zipPath.startsWith("https://")) {
            return downloadZipFile();
        } else {
            return new File(zipPath);
        }
    }

    /**
     * Download a ZIP file from a remote URL.
     */
    private File downloadZipFile() throws IOException {
        URL url = new URL(zipPath);
        String filename = new File(url.getPath()).getName();
        if (filename.isEmpty()) {
            filename = "archive.zip";
        }

        File downloadDir = Paths.get(System.getProperty("user.home"), ".levain", "downloads").toFile();
        Files.createDirectories(downloadDir.toPath());
        File downloadedFile = new File(downloadDir, filename);

        logger.debug("Downloading ZIP archive from {} to {}", zipPath, downloadedFile);
        try (InputStream inputStream = url.openStream();
                FileOutputStream outputStream = new FileOutputStream(downloadedFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        return downloadedFile;
    }

    /**
     * Load recipes from the extracted directory.
     */
    private Map<String, Recipe> loadRecipesFromLocalDirectory() {
        if (localRepository == null) {
            return Collections.emptyMap();
        }
        return localRepository.listRecipes().stream()
                .collect(Collectors.toMap(Recipe::getName, r -> r));
    }

    /**
     * Get the default cache directory for this ZIP archive.
     * Uses a hash of the ZIP path to create a unique directory.
     */
    private String getDefaultCacheDirectory(String path) {
        String cacheDir = System.getProperty("levain.cache.dir");
        if (cacheDir == null) {
            cacheDir = System.getenv("LEVAIN_CACHE_DIR");
        }
        if (cacheDir == null) {
            cacheDir = Paths.get(System.getProperty("user.home"), ".levain", "cache").toString();
        }

        // Use hash of the path to create a unique directory name
        String archiveName = String.valueOf(Math.abs(path.hashCode()));
        Path repositoryPath = Paths.get(cacheDir, "zip", archiveName);

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
            return String.format("ZipRepository (%s -> %s)", zipPath, localCachePath);
        }
        return String.format("ZipRepository (%s)", zipPath);
    }
}
