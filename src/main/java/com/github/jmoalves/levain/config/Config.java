package com.github.jmoalves.levain.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Levain configuration management.
 * 
 * Loads and manages configuration from ~/.levain/config.json
 * 
 * Configuration includes:
 * - levainHome: Directory where packages are installed
 * - registry: Location of installed recipes registry
 * - cache: Cache directory for downloaded artifacts
 * - shellPath: Path to preferred shell executable
 * - defaultPackage: Default package to install
 * 
 * The config file is optional. If not present, sensible defaults are used:
 * - levainHome: ~/levain
 * - registry: ~/.levain/registry
 * - cache: ~/.levain/cache
 */
@ApplicationScoped
public class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);
    private static final String DEFAULT_CONFIG_FILE = ".levain/config.json";
    private static final String DEFAULT_LEVAIN_HOME = "levain";

    private ConfigData configData;
    private Path configPath;

    public Config() {
        this.configData = new ConfigData();
        initializeConfig();
    }

    private void initializeConfig() {
        try {
            // Determine config file location
            String userHome = System.getProperty("user.home");
            configPath = Paths.get(userHome).resolve(DEFAULT_CONFIG_FILE);

            // Try to load existing config
            if (Files.exists(configPath)) {
                loadConfig();
                logger.info("Loaded configuration from: {}", configPath);
            } else {
                logger.debug("No configuration file found at {}, using defaults", configPath);
            }
        } catch (Exception e) {
            logger.warn("Failed to load configuration: {}. Using defaults.", e.getMessage());
        }
    }

    /**
     * Load configuration from file.
     */
    private void loadConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ConfigData loaded = mapper.readValue(configPath.toFile(), ConfigData.class);

        if (loaded != null) {
            this.configData = loaded;
            logger.debug("Configuration loaded: {}", configData);
        }
    }

    /**
     * Save configuration to file.
     * Creates directories and parent files as needed.
     */
    public void save() {
        try {
            // Ensure parent directory exists
            Files.createDirectories(configPath.getParent());

            // Write configuration
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(configPath.toFile(), configData);

            logger.info("Configuration saved to: {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration: {}", e.getMessage());
            throw new RuntimeException("Cannot save configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Get the levain home directory (where packages are installed).
     * Default: ~/levain
     */
    public Path getLevainHome() {
        String envHome = System.getenv("LEVAIN_HOME");
        if ((configData.levainHome == null || configData.levainHome.isEmpty())
                && envHome != null && !envHome.isBlank()) {
            return Paths.get(envHome);
        }
        if (configData.levainHome == null || configData.levainHome.isEmpty()) {
            String userHome = System.getProperty("user.home");
            return Paths.get(userHome).resolve(DEFAULT_LEVAIN_HOME);
        }
        return Paths.get(configData.levainHome);
    }

    /**
     * Set the levain home directory.
     */
    public void setLevainHome(String levainHome) {
        configData.levainHome = levainHome;
    }

    /**
     * Get the registry directory (where installed recipes are stored).
     * Default: ~/.levain/registry
     */
    public Path getRegistryDir() {
        String envRegistry = System.getenv("LEVAIN_REGISTRY_DIR");
        if ((configData.registryDir == null || configData.registryDir.isEmpty())
                && envRegistry != null && !envRegistry.isBlank()) {
            return Paths.get(envRegistry);
        }
        if (configData.registryDir == null || configData.registryDir.isEmpty()) {
            String userHome = System.getProperty("user.home");
            return Paths.get(userHome).resolve(".levain/registry");
        }
        return Paths.get(configData.registryDir);
    }

    /**
     * Set the registry directory.
     */
    public void setRegistryDir(String registryDir) {
        configData.registryDir = registryDir;
    }

    /**
     * Get the cache directory (for downloaded artifacts).
     * Default: ~/.levain/cache
     */
    public Path getCacheDir() {
        String envCache = System.getenv("LEVAIN_CACHE_DIR");
        if ((configData.cacheDir == null || configData.cacheDir.isEmpty())
                && envCache != null && !envCache.isBlank()) {
            return Paths.get(envCache);
        }
        if (configData.cacheDir == null || configData.cacheDir.isEmpty()) {
            String userHome = System.getProperty("user.home");
            return Paths.get(userHome).resolve(".levain/cache");
        }
        return Paths.get(configData.cacheDir);
    }

    /**
     * Get the backup directory (for package backups).
     * Default: ~/.levain/backup
     */
    public Path getBackupDir() {
        String envBackup = System.getenv("LEVAIN_BACKUP_DIR");
        if ((configData.backupDir == null || configData.backupDir.isEmpty())
                && envBackup != null && !envBackup.isBlank()) {
            return Paths.get(envBackup);
        }
        if (configData.backupDir == null || configData.backupDir.isEmpty()) {
            String userHome = System.getProperty("user.home");
            return Paths.get(userHome).resolve(".levain/backup");
        }
        return Paths.get(configData.backupDir);
    }

    /**
     * Set the backup directory.
     */
    public void setBackupDir(String backupDir) {
        configData.backupDir = backupDir;
    }

    /**
     * Check if backup is enabled.
     * Default: true
     */
    public boolean isBackupEnabled() {
        return configData.backupEnabled != null ? configData.backupEnabled : true;
    }

    /**
     * Set backup enabled flag.
     */
    public void setBackupEnabled(boolean enabled) {
        configData.backupEnabled = enabled;
    }

    /**
     * Get the number of backups to keep per package.
     * Default: 5
     */
    public int getBackupKeepCount() {
        return configData.backupKeepCount != null ? configData.backupKeepCount : 5;
    }

    /**
     * Set the number of backups to keep per package.
     */
    public void setBackupKeepCount(int count) {
        configData.backupKeepCount = count;
    }

    /**
     * Get the maximum age of backups in days.
     * Default: 30
     */
    public int getBackupMaxAgeDays() {
        return configData.backupMaxAgeDays != null ? configData.backupMaxAgeDays : 30;
    }

    /**
     * Set the maximum age of backups in days.
     */
    public void setBackupMaxAgeDays(int days) {
        configData.backupMaxAgeDays = days;
    }

    /**
     * Set the cache directory.
     */
    public void setCacheDir(String cacheDir) {
        configData.cacheDir = cacheDir;
    }

    /**
     * Get the shell path (preferred shell executable).
     */
    public String getShellPath() {
        return configData.shellPath;
    }

    /**
     * Set the shell path.
     */
    public void setShellPath(String shellPath) {
        configData.shellPath = shellPath;
    }

    /**
     * Check if shell should check for package updates.
     * Default: true
     */
    public boolean isShellCheckForUpdate() {
        return configData.shellCheckForUpdate != null ? configData.shellCheckForUpdate : true;
    }

    /**
     * Set whether shell should check for package updates.
     */
    public void setShellCheckForUpdate(boolean enabled) {
        configData.shellCheckForUpdate = enabled;
    }

    /**
     * Get the default package name.
     * Default: "levain"
     */
    public String getDefaultPackage() {
        return configData.defaultPackage != null ? configData.defaultPackage : "levain";
    }

    /**
     * Set the default package name.
     */
    public void setDefaultPackage(String packageName) {
        configData.defaultPackage = packageName;
    }

    /**
     * Get custom variables/environment settings.
     */
    public Map<String, String> getVariables() {
        if (configData.variables == null) {
            configData.variables = new HashMap<>();
        }
        return configData.variables;
    }

    /**
     * Set a custom variable.
     */
    public void setVariable(String name, String value) {
        if (configData.variables == null) {
            configData.variables = new HashMap<>();
        }
        configData.variables.put(name, value);
    }

    /**
     * Get a custom variable.
     */
    public String getVariable(String name) {
        if (configData.variables == null) {
            return null;
        }
        return configData.variables.get(name);
    }

    /**
     * Get the configuration path.
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Get a description of this configuration.
     */
    public String describe() {
        return String.format(
                "Config[levainHome=%s, registry=%s, cache=%s, backup=%s (enabled=%s)]",
                getLevainHome(),
                getRegistryDir(),
                getCacheDir(),
                getBackupDir(),
                isBackupEnabled());
    }

    /**
     * Configuration data structure (matches config.json format).
     * This is a simple POJO that can be serialized/deserialized by Jackson.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigData {
        @JsonProperty("levainHome")
        public String levainHome;

        @JsonProperty("registryDir")
        public String registryDir;

        @JsonProperty("cacheDir")
        public String cacheDir;

        @JsonProperty("backupDir")
        public String backupDir;

        @JsonProperty("backupEnabled")
        public Boolean backupEnabled;

        @JsonProperty("backupKeepCount")
        public Integer backupKeepCount;

        @JsonProperty("backupMaxAgeDays")
        public Integer backupMaxAgeDays;

        @JsonProperty("shellPath")
        public String shellPath;

        @JsonProperty("shellCheckForUpdate")
        public Boolean shellCheckForUpdate;

        @JsonProperty("defaultPackage")
        public String defaultPackage;

        @JsonProperty("variables")
        public Map<String, String> variables;

        @JsonProperty("repositories")
        public List<RepositoryConfig> repositories;

        public ConfigData() {
            this.variables = new HashMap<>();
            this.repositories = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "ConfigData{" +
                    "levainHome='" + levainHome + '\'' +
                    ", registryDir='" + registryDir + '\'' +
                    ", cacheDir='" + cacheDir + '\'' +
                    ", backupDir='" + backupDir + '\'' +
                    ", backupEnabled=" + backupEnabled +
                    ", backupKeepCount=" + backupKeepCount +
                    ", backupMaxAgeDays=" + backupMaxAgeDays +
                    ", shellPath='" + shellPath + '\'' +
                    ", shellCheckForUpdate=" + shellCheckForUpdate +
                    ", defaultPackage='" + defaultPackage + '\'' +
                    ", variables=" + variables +
                    ", repositories=" + repositories +
                    '}';
        }
    }

    /**
     * Repository configuration.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryConfig {
        @JsonProperty("name")
        public String name;

        @JsonProperty("uri")
        public String uri;

        public RepositoryConfig() {
        }

        public RepositoryConfig(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        @Override
        public String toString() {
            return "RepositoryConfig{" +
                    "name='" + name + '\'' +
                    ", uri='" + uri + '\'' +
                    '}';
        }
    }
}
