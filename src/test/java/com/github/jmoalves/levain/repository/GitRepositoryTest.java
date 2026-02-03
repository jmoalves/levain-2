package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitRepositoryTest {
    private GitRepository repository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Using a small test repository (only initialize if git is available)
        repository = new GitRepository("https://github.com/jmoalves/levain-pkgs.git");
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertTrue(repository.getName().contains("Git"));
        assertTrue(repository.getUri().contains("levain-pkgs"));
    }

    @Test
    void shouldDescribeRepository() {
        String description = repository.describe();
        assertTrue(description.contains("GitRepository"));
        assertTrue(description.contains("levain-pkgs"));
    }

    @Test
    void shouldBeInitializable() {
        assertFalse(repository.isInitialized());
        // Don't actually initialize in unit tests since it requires git and network
    }

    @Test
    void shouldReturnEmptyListWhenNotInitialized() {
        List<Recipe> recipes = repository.listRecipes();
        assertNotNull(recipes);
    }

    @Test
    void shouldReturnEmptyForUnknownRecipe() {
        Optional<Recipe> recipe = repository.resolveRecipe("unknown-recipe");
        // Should be empty since we didn't initialize
        assertTrue(recipe.isEmpty());
    }

    @Test
    void shouldReturnEmptyFileNameForInvalidName() {
        assertTrue(repository.getRecipeFileName("bad.levain.yaml").isEmpty());
    }

    @Test
    void shouldInitializeFromLocalGitRepository() throws Exception {
        Path originDir = tempDir.resolve("origin");
        Files.createDirectories(originDir);
        runGit(originDir, "init");
        runGit(originDir, "config", "user.email", "test@example.com");
        runGit(originDir, "config", "user.name", "Test User");

        Files.writeString(originDir.resolve("jdk-21.levain.yaml"), "name: jdk-21\nversion: 21.0.0\n");
        runGit(originDir, "add", ".");
        runGit(originDir, "commit", "-m", "init");

        String originalCache = System.getProperty("levain.cache.dir");
        System.setProperty("levain.cache.dir", tempDir.resolve("cache").toString());
        try {
            GitRepository localRepo = new GitRepository(originDir.toString());
            localRepo.init();

            assertTrue(localRepo.isInitialized());
            assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("jdk-21")));
            assertTrue(localRepo.getRecipeYamlContent("jdk-21").isPresent());
            assertTrue(localRepo.describe().contains("->"));
        } finally {
            if (originalCache != null) {
                System.setProperty("levain.cache.dir", originalCache);
            } else {
                System.clearProperty("levain.cache.dir");
            }
        }
    }

    private static void runGit(Path workingDir, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        Process process = new ProcessBuilder(command)
                .directory(new File(workingDir.toString()))
                .redirectErrorStream(true)
                .start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Git command failed: " + String.join(" ", command));
        }
    }
}
