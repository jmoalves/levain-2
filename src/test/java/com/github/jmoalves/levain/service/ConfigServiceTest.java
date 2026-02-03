package com.github.jmoalves.levain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jmoalves.levain.model.LevainConfig;
import com.github.jmoalves.levain.model.RepositoryConfig;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        // Override user.home for testing
        System.setProperty("user.home", tempDir.toString());
        configService = new ConfigService();
    }

    @Test
    void testAddRepositoryWithName() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "my-repo");

        List<RepositoryConfig> repos = configService.getRepositories();
        assertEquals(1, repos.size());
        assertEquals("my-repo", repos.get(0).getName());
        assertEquals("https://github.com/user/repo.git", repos.get(0).getUri());
    }

    @Test
    void testAddRepositoryWithoutName() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", null);

        List<RepositoryConfig> repos = configService.getRepositories();
        assertEquals(1, repos.size());
        assertNotNull(repos.get(0).getName());
        assertTrue(repos.get(0).getName().contains("user/repo"));
    }

    @Test
    void testAddRepositoryWithEmptyName() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "");

        List<RepositoryConfig> repos = configService.getRepositories();
        assertEquals(1, repos.size());
        assertFalse(repos.get(0).getName().isEmpty());
    }

    @Test
    void testAddRepositoryDuplicateUri() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "repo1");

        assertThrows(IllegalArgumentException.class, () -> {
            configService.addRepository("https://github.com/user/repo.git", "repo2");
        });
    }

    @Test
    void testAddRepositoryDuplicateName() throws IOException {
        configService.addRepository("https://github.com/user/repo1.git", "my-repo");

        assertThrows(IllegalArgumentException.class, () -> {
            configService.addRepository("https://github.com/user/repo2.git", "my-repo");
        });
    }

    @Test
    void testRemoveRepositoryByName() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "my-repo");

        boolean removed = configService.removeRepository("my-repo");

        assertTrue(removed);
        assertTrue(configService.getRepositories().isEmpty());
    }

    @Test
    void testRemoveRepositoryByUri() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "my-repo");

        boolean removed = configService.removeRepository("https://github.com/user/repo.git");

        assertTrue(removed);
        assertTrue(configService.getRepositories().isEmpty());
    }

    @Test
    void testRemoveRepositoryNotFound() throws IOException {
        boolean removed = configService.removeRepository("nonexistent");

        assertFalse(removed);
    }

    @Test
    void testGetRepositoriesReturnsACopy() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "repo1");

        List<RepositoryConfig> repos = configService.getRepositories();
        repos.clear();

        // Original list should not be affected
        assertEquals(1, configService.getRepositories().size());
    }

    @Test
    void testGetConfig() {
        LevainConfig config = configService.getConfig();

        assertNotNull(config);
        assertNotNull(config.getRepositories());
    }

    @Test
    void testConfigPersistence() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", "my-repo");

        // Create new service instance to test loading
        ConfigService newService = new ConfigService();
        List<RepositoryConfig> repos = newService.getRepositories();

        assertEquals(1, repos.size());
        assertEquals("my-repo", repos.get(0).getName());
        assertEquals("https://github.com/user/repo.git", repos.get(0).getUri());
    }

    @Test
    void testLoadExistingConfig() throws IOException {
        // Create a config file manually
        Path levainDir = tempDir.resolve(".levain");
        Files.createDirectories(levainDir);
        Path configFile = levainDir.resolve("config.json");

        LevainConfig config = new LevainConfig();
        List<RepositoryConfig> repos = new ArrayList<>();
        RepositoryConfig repo = new RepositoryConfig();
        repo.setName("existing-repo");
        repo.setUri("https://github.com/existing/repo.git");
        repos.add(repo);
        config.setRepositories(repos);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);

        // Create service which should load existing config
        ConfigService service = new ConfigService();
        List<RepositoryConfig> loadedRepos = service.getRepositories();

        assertEquals(1, loadedRepos.size());
        assertEquals("existing-repo", loadedRepos.get(0).getName());
    }

    @Test
    void testGenerateRepositoryNameFromGitHub() throws IOException {
        configService.addRepository("https://github.com/user/repo.git", null);

        List<RepositoryConfig> repos = configService.getRepositories();
        assertTrue(repos.get(0).getName().contains("user/repo"));
    }

    @Test
    void testGenerateRepositoryNameFromPlainUrl() throws IOException {
        configService.addRepository("http://example.com/path/to/repo.git", null);

        List<RepositoryConfig> repos = configService.getRepositories();
        assertNotNull(repos.get(0).getName());
        assertFalse(repos.get(0).getName().isEmpty());
    }

    @Test
    void testMultipleRepositories() throws IOException {
        configService.addRepository("https://github.com/user/repo1.git", "repo1");
        configService.addRepository("https://github.com/user/repo2.git", "repo2");
        configService.addRepository("https://github.com/user/repo3.git", "repo3");

        List<RepositoryConfig> repos = configService.getRepositories();
        assertEquals(3, repos.size());
    }

    @Test
    void testLoadConfigWithNullRepositories() throws IOException {
        // Create a config file with null repositories
        Path levainDir = tempDir.resolve(".levain");
        Files.createDirectories(levainDir);
        Path configFile = levainDir.resolve("config.json");

        Files.writeString(configFile, "{}");

        // Should not throw and should initialize empty list
        ConfigService service = new ConfigService();
        assertNotNull(service.getRepositories());
        assertTrue(service.getRepositories().isEmpty());
    }

    @Test
    void testLoadCorruptedConfig() throws IOException {
        // Create a corrupted config file
        Path levainDir = tempDir.resolve(".levain");
        Files.createDirectories(levainDir);
        Path configFile = levainDir.resolve("config.json");

        Files.writeString(configFile, "{ invalid json }");

        // Should not throw and should use default config
        ConfigService service = new ConfigService();
        assertNotNull(service.getRepositories());
        assertTrue(service.getRepositories().isEmpty());
    }
}
