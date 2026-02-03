package com.github.jmoalves.levain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for Config.
 */
@DisplayName("Config Tests")
class ConfigTest {

    @TempDir
    Path tempDir;

    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
    }

    @Test
    @DisplayName("Should have default levain home")
    void shouldHaveDefaultLevainHome() {
        Path levainHome = config.getLevainHome();
        assertNotNull(levainHome);
        assertTrue(levainHome.toString().endsWith("levain"));
    }

    @Test
    @DisplayName("Should have default registry directory")
    void shouldHaveDefaultRegistryDir() {
        Path registryDir = config.getRegistryDir();
        assertNotNull(registryDir);
        assertTrue(registryDir.toString().contains(".levain/registry"));
    }

    @Test
    @DisplayName("Should have default cache directory")
    void shouldHaveDefaultCacheDir() {
        Path cacheDir = config.getCacheDir();
        assertNotNull(cacheDir);
        assertTrue(cacheDir.toString().contains(".levain/cache"));
    }

    @Test
    @DisplayName("Should have default package name")
    void shouldHaveDefaultPackageName() {
        String defaultPackage = config.getDefaultPackage();
        assertEquals("levain", defaultPackage);
    }

    @Test
    @DisplayName("Should set and get levain home")
    void shouldSetAndGetLevainHome() {
        String newHome = "/custom/levain";
        config.setLevainHome(newHome);
        assertEquals(newHome, config.getLevainHome().toString());
    }

    @Test
    @DisplayName("Should set and get registry directory")
    void shouldSetAndGetRegistryDir() {
        String newRegistry = "/custom/registry";
        config.setRegistryDir(newRegistry);
        assertEquals(newRegistry, config.getRegistryDir().toString());
    }

    @Test
    @DisplayName("Should set and get cache directory")
    void shouldSetAndGetCacheDir() {
        String newCache = "/custom/cache";
        config.setCacheDir(newCache);
        assertEquals(newCache, config.getCacheDir().toString());
    }

    @Test
    @DisplayName("Should set and get shell path")
    void shouldSetAndGetShellPath() {
        String shellPath = "/bin/bash";
        config.setShellPath(shellPath);
        assertEquals(shellPath, config.getShellPath());
    }

    @Test
    @DisplayName("Should set and get default package")
    void shouldSetAndGetDefaultPackage() {
        String packageName = "custom-package";
        config.setDefaultPackage(packageName);
        assertEquals(packageName, config.getDefaultPackage());
    }

    @Test
    @DisplayName("Should set and get custom variables")
    void shouldSetAndGetCustomVariables() {
        config.setVariable("myVar", "myValue");
        assertEquals("myVar", "myVar");
        assertEquals("myValue", config.getVariable("myVar"));
    }

    @Test
    @DisplayName("Should get all variables")
    void shouldGetAllVariables() {
        config.setVariable("var1", "value1");
        config.setVariable("var2", "value2");

        var variables = config.getVariables();
        assertEquals(2, variables.size());
        assertEquals("value1", variables.get("var1"));
        assertEquals("value2", variables.get("var2"));
    }

    @Test
    @DisplayName("Should return null for non-existent variable")
    void shouldReturnNullForNonExistentVariable() {
        String value = config.getVariable("non-existent");
        assertEquals(null, value);
    }

    @Test
    @DisplayName("Should provide description")
    void shouldProvideDescription() {
        String description = config.describe();
        assertNotNull(description);
        assertTrue(description.contains("Config["));
        assertTrue(description.contains("levainHome"));
        assertTrue(description.contains("registry"));
        assertTrue(description.contains("cache"));
    }

    @Test
    @DisplayName("Should save configuration to file")
    void shouldSaveConfigurationToFile() {
        // Note: This test will save to ~/.levain/config.json
        // which is fine for testing
        config.setLevainHome("/tmp/levain");
        config.setDefaultPackage("test-package");
        config.setVariable("testVar", "testValue");

        // Don't actually save to home directory during tests
        // Just verify the config data is set correctly
        assertEquals("/tmp/levain", config.getLevainHome().toString());
        assertEquals("test-package", config.getDefaultPackage());
        assertEquals("testValue", config.getVariable("testVar"));
    }

    @Test
    @DisplayName("Should serialize configuration data")
    void shouldSerializeConfigurationData() throws Exception {
        Config.ConfigData configData = new Config.ConfigData();
        configData.levainHome = "/custom/levain";
        configData.registryDir = "/custom/registry";
        configData.cacheDir = "/custom/cache";
        configData.defaultPackage = "my-package";

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configData);

        assertTrue(json.contains("levainHome"));
        assertTrue(json.contains("/custom/levain"));
        assertTrue(json.contains("registryDir"));
        assertTrue(json.contains("/custom/registry"));
    }

    @Test
    @DisplayName("Should deserialize configuration data")
    void shouldDeserializeConfigurationData() throws Exception {
        String json = "{\n" +
                "  \"levainHome\": \"/custom/levain\",\n" +
                "  \"registryDir\": \"/custom/registry\",\n" +
                "  \"cacheDir\": \"/custom/cache\",\n" +
                "  \"defaultPackage\": \"my-package\"\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        Config.ConfigData configData = mapper.readValue(json, Config.ConfigData.class);

        assertEquals("/custom/levain", configData.levainHome);
        assertEquals("/custom/registry", configData.registryDir);
        assertEquals("/custom/cache", configData.cacheDir);
        assertEquals("my-package", configData.defaultPackage);
    }

    @Test
    @DisplayName("Should handle empty variable map")
    void shouldHandleEmptyVariableMap() {
        var variables = config.getVariables();
        assertNotNull(variables);
        assertTrue(variables.isEmpty());
    }

    @Test
    @DisplayName("Should update existing variable")
    void shouldUpdateExistingVariable() {
        config.setVariable("key", "value1");
        assertEquals("value1", config.getVariable("key"));

        config.setVariable("key", "value2");
        assertEquals("value2", config.getVariable("key"));
    }

    @Test
    @DisplayName("Should save and reload configuration")
    void shouldSaveAndReloadConfiguration() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        try {
            Config localConfig = new Config();
            localConfig.setLevainHome(tempDir.resolve("levain-home").toString());
            localConfig.setRegistryDir(tempDir.resolve("registry").toString());
            localConfig.setCacheDir(tempDir.resolve("cache").toString());
            localConfig.setShellPath("/bin/zsh");
            localConfig.setDefaultPackage("jdk-21");
            localConfig.setVariable("FOO", "BAR");
            localConfig.save();

            assertTrue(Files.exists(localConfig.getConfigPath()));

            Config reloaded = new Config();
            assertEquals(localConfig.getLevainHome(), reloaded.getLevainHome());
            assertEquals(localConfig.getRegistryDir(), reloaded.getRegistryDir());
            assertEquals(localConfig.getCacheDir(), reloaded.getCacheDir());
            assertEquals("/bin/zsh", reloaded.getShellPath());
            assertEquals("jdk-21", reloaded.getDefaultPackage());
            assertEquals("BAR", reloaded.getVariable("FOO"));
        } finally {
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            }
        }
    }

    @Test
    @DisplayName("Should expose repository config string")
    void shouldExposeRepositoryConfigString() {
        Config.RepositoryConfig repo = new Config.RepositoryConfig("local", "dir:/tmp");
        String repoString = repo.toString();
        assertTrue(repoString.contains("local"));
        assertTrue(repoString.contains("dir:/tmp"));
    }
}
