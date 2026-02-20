package com.github.jmoalves.levain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config.ConfigData Tests")
class ConfigDataTest {
    private Config.ConfigData configData;

    @BeforeEach
    void setUp() {
        configData = new Config.ConfigData();
    }

    @Test
    @DisplayName("Should initialize with empty collections")
    void testInitialization() {
        assertNotNull(configData.variables, "variables should be initialized");
        assertNotNull(configData.repositories, "repositories should be initialized");
        assertTrue(configData.variables.isEmpty(), "variables should be empty");
        assertTrue(configData.repositories.isEmpty(), "repositories should be empty");
    }

    @Test
    @DisplayName("Should set and get levainHome")
    void testLevainHome() {
        String home = "/home/user/.levain";
        configData.levainHome = home;
        assertEquals(home, configData.levainHome, "levainHome should be set and retrievable");
    }

    @Test
    @DisplayName("Should set and get registryDir")
    void testRegistryDir() {
        String dir = "/home/user/.levain/registry";
        configData.registryDir = dir;
        assertEquals(dir, configData.registryDir, "registryDir should be set and retrievable");
    }

    @Test
    @DisplayName("Should set and get cacheDir")
    void testCacheDir() {
        String dir = "/home/user/.levain/cache";
        configData.cacheDir = dir;
        assertEquals(dir, configData.cacheDir, "cacheDir should be set and retrievable");
    }

    @Test
    @DisplayName("Should set and get shellPath")
    void testShellPath() {
        String path = "/bin/bash";
        configData.shellPath = path;
        assertEquals(path, configData.shellPath, "shellPath should be set and retrievable");
    }

    @Test
    @DisplayName("Should set and get shellCheckForUpdate")
    void testShellCheckForUpdate() {
        configData.shellCheckForUpdate = Boolean.FALSE;
        assertEquals(Boolean.FALSE, configData.shellCheckForUpdate, "shellCheckForUpdate should be set and retrievable");
    }

    @Test
    @DisplayName("Should set and get defaultPackage")
    void testDefaultPackage() {
        String pkg = "jdk-21";
        configData.defaultPackage = pkg;
        assertEquals(pkg, configData.defaultPackage, "defaultPackage should be set and retrievable");
    }

    @Test
    @DisplayName("Should add variables")
    void testAddVariables() {
        configData.variables.put("JAVA_HOME", "/usr/lib/jvm/java-21");
        configData.variables.put("M2_HOME", "/usr/share/maven");

        assertEquals(2, configData.variables.size(), "Should have 2 variables");
        assertEquals("/usr/lib/jvm/java-21", configData.variables.get("JAVA_HOME"));
        assertEquals("/usr/share/maven", configData.variables.get("M2_HOME"));
    }

    @Test
    @DisplayName("Should add repositories")
    void testAddRepositories() {
        Config.RepositoryConfig repo1 = new Config.RepositoryConfig();
        repo1.name = "repo1";
        repo1.uri = "https://github.com/user/repo1.git";

        configData.repositories.add(repo1);
        assertEquals(1, configData.repositories.size(), "Should have 1 repository");
        assertEquals("repo1", configData.repositories.get(0).name);
    }

    @Test
    @DisplayName("Should have proper toString")
    void testToString() {
        configData.levainHome = "/test";
        configData.registryDir = "/test/registry";

        String str = configData.toString();
        assertNotNull(str, "toString should not be null");
        assertTrue(str.contains("ConfigData"), "toString should contain class name");
        assertTrue(str.contains("levainHome"), "toString should contain levainHome");
        assertTrue(str.contains("/test"), "toString should contain value");
    }

    @Test
    @DisplayName("Should handle null collections")
    void testNullCollections() {
        Config.ConfigData data = new Config.ConfigData();
        data.variables = null;
        data.repositories = null;

        assertNull(data.variables, "variables can be null");
        assertNull(data.repositories, "repositories can be null");
    }

    @Test
    @DisplayName("Should be JsonIgnoreProperties")
    void testJsonIgnorePropertiesAnnotation() {
        assertTrue(Config.ConfigData.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "ConfigData should have @JsonIgnoreProperties");
    }

    @Test
    @DisplayName("Should have JsonProperty annotations")
    void testJsonPropertyAnnotations() {
        assertDoesNotThrow(() -> {
            Config.ConfigData.class.getDeclaredField("levainHome");
            assertTrue(Config.ConfigData.class.getDeclaredField("levainHome")
                    .isAnnotationPresent(JsonProperty.class));
        }, "levainHome should have @JsonProperty");
    }

    @Test
    @DisplayName("Should support multiple repositories")
    void testMultipleRepositories() {
        Config.RepositoryConfig repo1 = new Config.RepositoryConfig();
        repo1.name = "repo1";

        Config.RepositoryConfig repo2 = new Config.RepositoryConfig();
        repo2.name = "repo2";

        configData.repositories.add(repo1);
        configData.repositories.add(repo2);

        assertEquals(2, configData.repositories.size());
        assertEquals("repo1", configData.repositories.get(0).name);
        assertEquals("repo2", configData.repositories.get(1).name);
    }

    @Test
    @DisplayName("Should support multiple variables")
    void testMultipleVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("VAR1", "value1");
        vars.put("VAR2", "value2");
        vars.put("VAR3", "value3");

        configData.variables.putAll(vars);
        assertEquals(3, configData.variables.size());
    }

    @Test
    @DisplayName("ConfigData should be serializable by Jackson")
    void testSerializableByJackson() {
        assertTrue(Config.ConfigData.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "ConfigData should be annotated for JSON serialization");
    }
}
