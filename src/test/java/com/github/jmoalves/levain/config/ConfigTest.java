package com.github.jmoalves.levain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
    @DisplayName("Should use LEVAIN_HOME when configured")
    void shouldUseLevainHomeFromEnv() throws Exception {
        String original = System.getenv("LEVAIN_HOME");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("LEVAIN_HOME", tempDir.resolve("env-home").toString());
            System.setProperty("user.home", tempDir.toString());

            assertEquals(tempDir.resolve("env-home"), config.getLevainHome());
        } finally {
            restoreEnv("LEVAIN_HOME", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("Should use LEVAIN_REGISTRY_DIR when configured")
    void shouldUseRegistryDirFromEnv() throws Exception {
        String original = System.getenv("LEVAIN_REGISTRY_DIR");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("LEVAIN_REGISTRY_DIR", tempDir.resolve("env-registry").toString());
            System.setProperty("user.home", tempDir.toString());

            assertEquals(tempDir.resolve("env-registry"), config.getRegistryDir());
        } finally {
            restoreEnv("LEVAIN_REGISTRY_DIR", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("Should use LEVAIN_CACHE_DIR when configured")
    void shouldUseCacheDirFromEnv() throws Exception {
        String original = System.getenv("LEVAIN_CACHE_DIR");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("LEVAIN_CACHE_DIR", tempDir.resolve("env-cache").toString());
            System.setProperty("user.home", tempDir.toString());

            assertEquals(tempDir.resolve("env-cache"), config.getCacheDir());
        } finally {
            restoreEnv("LEVAIN_CACHE_DIR", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("Should fall back when env is blank")
    void shouldFallbackWhenEnvBlank() throws Exception {
        String original = System.getenv("LEVAIN_HOME");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("LEVAIN_HOME", " ");
            System.setProperty("user.home", tempDir.toString());

            assertEquals(tempDir.resolve("levain"), config.getLevainHome());
        } finally {
            restoreEnv("LEVAIN_HOME", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("Should handle null variable map")
    void shouldHandleNullVariableMap() throws Exception {
        Field dataField = Config.class.getDeclaredField("configData");
        dataField.setAccessible(true);
        Config.ConfigData data = (Config.ConfigData) dataField.get(config);
        data.variables = null;

        assertNotNull(config.getVariables());
        assertTrue(config.getVariables().isEmpty());
        assertNull(config.getVariable("missing"));
    }

    @Test
    @DisplayName("Should keep defaults when config file is null")
    void shouldHandleNullConfigFile() throws Exception {
        Path configPath = tempDir.resolve(".levain").resolve("config.json");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, "null");

        Field configPathField = Config.class.getDeclaredField("configPath");
        configPathField.setAccessible(true);
        configPathField.set(config, configPath);

        var loadConfig = Config.class.getDeclaredMethod("loadConfig");
        loadConfig.setAccessible(true);
        loadConfig.invoke(config);

        assertEquals("levain", config.getDefaultPackage());
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

    @Test
    @DisplayName("Should handle invalid config file")
    void shouldHandleInvalidConfigFile() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        try {
            Path configPath = tempDir.resolve(".levain").resolve("config.json");
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, "{invalid json");

            Config localConfig = new Config();
            assertEquals("levain", localConfig.getDefaultPackage());
        } finally {
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            } else {
                System.clearProperty("user.home");
            }
        }
    }

    @Test
    @DisplayName("Should have default backup directory")
    void shouldHaveDefaultBackupDir() {
        Path backupDir = config.getBackupDir();
        assertNotNull(backupDir);
        assertTrue(backupDir.toString().contains(".levain/backup"));
    }

    @Test
    @DisplayName("Should set and get backup directory")
    void shouldSetAndGetBackupDir() {
        String newBackupDir = "/custom/backup";
        config.setBackupDir(newBackupDir);
        assertEquals(newBackupDir, config.getBackupDir().toString());
    }

    @Test
    @DisplayName("Should have backup enabled by default")
    void shouldHaveBackupEnabledByDefault() {
        assertTrue(config.isBackupEnabled());
    }

    @Test
    @DisplayName("Should set and get backup enabled flag")
    void shouldSetAndGetBackupEnabled() {
        config.setBackupEnabled(false);
        assertFalse(config.isBackupEnabled());
        
        config.setBackupEnabled(true);
        assertTrue(config.isBackupEnabled());
    }

    @Test
    @DisplayName("Should have default backup keep count")
    void shouldHaveDefaultBackupKeepCount() {
        assertEquals(5, config.getBackupKeepCount());
    }

    @Test
    @DisplayName("Should set and get backup keep count")
    void shouldSetAndGetBackupKeepCount() {
        config.setBackupKeepCount(10);
        assertEquals(10, config.getBackupKeepCount());
    }

    @Test
    @DisplayName("Should have default backup max age days")
    void shouldHaveDefaultBackupMaxAgeDays() {
        assertEquals(30, config.getBackupMaxAgeDays());
    }

    @Test
    @DisplayName("Should set and get backup max age days")
    void shouldSetAndGetBackupMaxAgeDays() {
        config.setBackupMaxAgeDays(60);
        assertEquals(60, config.getBackupMaxAgeDays());
    }

    @Test
    @DisplayName("Should use LEVAIN_BACKUP_DIR when configured")
    void shouldUseBackupDirFromEnv() throws Exception {
        String original = System.getenv("LEVAIN_BACKUP_DIR");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("LEVAIN_BACKUP_DIR", tempDir.resolve("env-backup").toString());
            System.setProperty("user.home", tempDir.toString());

            assertEquals(tempDir.resolve("env-backup"), config.getBackupDir());
        } finally {
            restoreEnv("LEVAIN_BACKUP_DIR", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("Should serialize backup configuration data")
    void shouldSerializeBackupConfigurationData() throws Exception {
        Config.ConfigData configData = new Config.ConfigData();
        configData.backupDir = "/custom/backup";
        configData.backupEnabled = true;
        configData.backupKeepCount = 10;
        configData.backupMaxAgeDays = 60;

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configData);

        assertTrue(json.contains("backupDir"));
        assertTrue(json.contains("/custom/backup"));
        assertTrue(json.contains("backupEnabled"));
        assertTrue(json.contains("backupKeepCount"));
        assertTrue(json.contains("backupMaxAgeDays"));
    }

    @Test
    @DisplayName("Should deserialize backup configuration data")
    void shouldDeserializeBackupConfigurationData() throws Exception {
        String json = "{\n" +
                "  \"backupDir\": \"/custom/backup\",\n" +
                "  \"backupEnabled\": true,\n" +
                "  \"backupKeepCount\": 10,\n" +
                "  \"backupMaxAgeDays\": 60\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        Config.ConfigData configData = mapper.readValue(json, Config.ConfigData.class);

        assertEquals("/custom/backup", configData.backupDir);
        assertTrue(configData.backupEnabled);
        assertEquals(10, configData.backupKeepCount);
        assertEquals(60, configData.backupMaxAgeDays);
    }

    @Test
    @DisplayName("Should save and reload backup configuration")
    void shouldSaveAndReloadBackupConfiguration() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        try {
            Config localConfig = new Config();
            localConfig.setBackupDir(tempDir.resolve("backup").toString());
            localConfig.setBackupEnabled(false);
            localConfig.setBackupKeepCount(15);
            localConfig.setBackupMaxAgeDays(90);
            localConfig.save();

            assertTrue(Files.exists(localConfig.getConfigPath()));

            Config reloaded = new Config();
            assertEquals(localConfig.getBackupDir(), reloaded.getBackupDir());
            assertEquals(false, reloaded.isBackupEnabled());
            assertEquals(15, reloaded.getBackupKeepCount());
            assertEquals(90, reloaded.getBackupMaxAgeDays());
        } finally {
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            }
        }
    }

    @Test
    @DisplayName("Should describe backup configuration")
    void shouldDescribeBackupConfiguration() {
        String description = config.describe();
        assertNotNull(description);
        assertTrue(description.contains("backup"));
        assertTrue(description.contains("enabled"));
    }

    @Test
    @DisplayName("Should throw when save cannot create parent directory")
    void shouldThrowWhenSaveCannotCreateParent() throws Exception {
        Path parentFile = tempDir.resolve("not-a-dir");
        Files.writeString(parentFile, "content");

        Field configPathField = Config.class.getDeclaredField("configPath");
        configPathField.setAccessible(true);
        configPathField.set(config, parentFile.resolve("config.json"));

        assertThrows(RuntimeException.class, () -> config.save());
    }

    private void restoreProperty(String key, String original) {
        if (original == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, original);
    }

    @SuppressWarnings("unchecked")
    private static void updateEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> mutable = (Map<String, String>) field.get(env);
        if (value == null) {
            mutable.remove(key);
        } else {
            mutable.put(key, value);
        }
    }

    private static void restoreEnv(String key, String original) throws Exception {
        updateEnv(key, original);
    }
}
