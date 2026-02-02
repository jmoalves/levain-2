package com.github.jmoalves.levain.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jmoalves.levain.model.LevainConfig;
import com.github.jmoalves.levain.model.RepositoryConfig;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for managing Levain configuration.
 */
@ApplicationScoped
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_FILE = "config.json";

    private final Path configPath;
    private final ObjectMapper mapper;
    private LevainConfig config;

    public ConfigService() {
        String userHome = System.getProperty("user.home");
        Path levainDir = Paths.get(userHome, ".levain");
        this.configPath = levainDir.resolve(CONFIG_FILE);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadConfig();
    }

    /**
     * Load configuration from file, or create default if not exists.
     */
    private void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                config = mapper.readValue(configPath.toFile(), LevainConfig.class);
                logger.debug("Loaded config from: {}", configPath);
            } else {
                config = new LevainConfig();
                logger.debug("Created new config (file not found)");
            }

            // Ensure repositories list is not null
            if (config.getRepositories() == null) {
                config.setRepositories(new ArrayList<>());
            }
        } catch (IOException e) {
            logger.error("Failed to load config from {}: {}", configPath, e.getMessage());
            config = new LevainConfig();
            config.setRepositories(new ArrayList<>());
        }
    }

    /**
     * Save configuration to file.
     */
    private void saveConfig() throws IOException {
        // Ensure directory exists
        Files.createDirectories(configPath.getParent());

        // Write config
        mapper.writeValue(configPath.toFile(), config);
        logger.debug("Saved config to: {}", configPath);
    }

    /**
     * Add a repository to the configuration.
     *
     * @param uri  Repository URI
     * @param name Optional name for the repository
     * @throws IOException if unable to save configuration
     */
    public void addRepository(String uri, String name) throws IOException {
        // Generate name if not provided
        if (name == null || name.isEmpty()) {
            name = generateRepositoryName(uri);
        }

        // Check if already exists
        for (RepositoryConfig repo : config.getRepositories()) {
            if (repo.getUri().equals(uri)) {
                throw new IllegalArgumentException("Repository already configured: " + uri);
            }
            if (repo.getName().equals(name)) {
                throw new IllegalArgumentException("Repository name already in use: " + name);
            }
        }

        // Add repository
        RepositoryConfig repoConfig = new RepositoryConfig();
        repoConfig.setName(name);
        repoConfig.setUri(uri);
        config.getRepositories().add(repoConfig);

        // Save
        saveConfig();
        logger.info("Added repository: {} ({})", name, uri);
    }

    /**
     * Remove a repository from the configuration.
     *
     * @param identifier Repository name or URI
     * @return true if removed, false if not found
     * @throws IOException if unable to save configuration
     */
    public boolean removeRepository(String identifier) throws IOException {
        List<RepositoryConfig> repos = config.getRepositories();
        boolean removed = repos.removeIf(repo -> repo.getName().equals(identifier) || repo.getUri().equals(identifier));

        if (removed) {
            saveConfig();
            logger.info("Removed repository: {}", identifier);
        }

        return removed;
    }

    /**
     * Get all configured repositories.
     *
     * @return List of repository configurations
     */
    public List<RepositoryConfig> getRepositories() {
        return new ArrayList<>(config.getRepositories());
    }

    /**
     * Get the full configuration object.
     *
     * @return Levain configuration
     */
    public LevainConfig getConfig() {
        return config;
    }

    /**
     * Generate a repository name from URI.
     */
    private String generateRepositoryName(String uri) {
        // Extract meaningful name from URI
        String name = uri;

        // Remove protocol
        name = name.replaceFirst("^[a-z]+://", "");

        // Extract from git URLs
        if (name.contains("github.com")) {
            String[] parts = name.split("/");
            if (parts.length >= 2) {
                name = parts[parts.length - 2] + "/" + parts[parts.length - 1];
            }
        }

        // Remove .git suffix
        name = name.replaceFirst("\\.git$", "");

        // Clean up
        name = name.replaceAll("[^a-zA-Z0-9/_-]", "-");

        return name;
    }
}
