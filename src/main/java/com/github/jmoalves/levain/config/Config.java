package com.github.jmoalves.levain.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
        if (configData.cacheDir == null || configData.cacheDir.isEmpty()) {
            String userHome = System.getProperty("user.home");
            return Paths.get(userHome).resolve(".levain/cache");
        }
        return Paths.get(configData.cacheDir);
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
                "Config[levainHome=%s, registry=%s, cache=%s]",
                getLevainHome(),
                getRegistryDir(),
                getCacheDir());
    }

    /**
     * Configuration data structure (matches config.json format).
     * This is a simple POJO that can be serialized/deserialized by Jackson.
     */
    public static class ConfigData {
        @JsonProperty("levainHome")
        public String levainHome;

        @JsonProperty("registryDir")
        public String registryDir;

        @JsonProperty("cacheDir")
        public String cacheDir;

        @JsonProperty("shellPath")
        public String shellPath;

        @JsonProperty("defaultPackage")
        public String defaultPackage;

        @JsonProperty("variables")
        public Map<String, String> variables;

        public ConfigData() {
            this.variables = new HashMap<>();
        }

        @Override
        public String toString() {
            return "ConfigData{" +
                    "levainHome='" + levainHome + '\'' +
                    ", registryDir='" + registryDir + '\'' +
                    ", cacheDir='" + cacheDir + '\'' +
                    ", shellPath='" + shellPath + '\'' +
                    ", defaultPackage='" + defaultPackage + '\'' +
                    ", variables=" + variables +
                    '}';
        }
    }
}
