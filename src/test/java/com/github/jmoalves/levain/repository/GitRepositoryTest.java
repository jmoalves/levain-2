package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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
    void shouldReturnEmptyYamlContentWhenNotInitialized() {
        assertTrue(repository.getRecipeYamlContent("jdk-21").isEmpty());
    }

    @Test
    void shouldReturnEmptyFileNameForInvalidName() {
        assertTrue(repository.getRecipeFileName("bad.levain.yaml").isEmpty());
    }

    @Test
    void shouldReturnFileNameForValidName() {
        assertEquals(Optional.of("jdk-21.levain.yaml"), repository.getRecipeFileName("jdk-21"));
    }

    @Test
    void shouldInitializeFromLocalGitRepository() throws Exception {
        Path originDir = tempDir.resolve("origin");
        Files.createDirectories(originDir);
        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("jdk-21.levain.yaml"), "name: jdk-21\nversion: 21.0.0\n");
            commitAll(origin, "init");

            String originalCache = System.getProperty("levain.cache.dir");
            System.setProperty("levain.cache.dir", tempDir.resolve("cache").toString());
            try {
                GitRepository localRepo = new GitRepository(originDir.toUri().toString());
                localRepo.init();

                assertTrue(localRepo.isInitialized());
                assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("jdk-21")));
                assertTrue(localRepo.getRecipeYamlContent("jdk-21").isPresent());
                assertTrue(localRepo.describe().contains("->"));

                Files.writeString(originDir.resolve("maven-3.9.9.levain.yaml"), "name: maven-3.9.9\nversion: 3.9.9\n");
                commitAll(origin, "add maven");
                localRepo.init();

                assertTrue(localRepo.listRecipes().stream().anyMatch(r -> r.getName().equals("maven-3.9.9")));
            } finally {
                if (originalCache != null) {
                    System.setProperty("levain.cache.dir", originalCache);
                } else {
                    System.clearProperty("levain.cache.dir");
                }
            }
        }
    }

    @Test
    void shouldDeleteIncompleteCacheDirBeforeClone() throws Exception {
        Path originDir = tempDir.resolve("origin-delete");
        Files.createDirectories(originDir);
        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("deno.levain.yaml"), "name: deno\nversion: 1.0.0\n");
            commitAll(origin, "init");

            String originalCache = System.getProperty("levain.cache.dir");
            Path cacheRoot = tempDir.resolve("cache-delete");
            System.setProperty("levain.cache.dir", cacheRoot.toString());
            try {
                String repoName = String.valueOf(Math.abs(originDir.toUri().toString().hashCode()));
                Path cachePath = Paths.get(cacheRoot.toString(), "git", repoName);
                Files.createDirectories(cachePath);
                Path junkFile = cachePath.resolve("junk.txt");
                Files.writeString(junkFile, "junk");

                GitRepository localRepo = new GitRepository(originDir.toUri().toString());
                localRepo.init();

                assertFalse(Files.exists(junkFile));
                assertTrue(Files.exists(cachePath.resolve(".git")));
            } finally {
                if (originalCache != null) {
                    System.setProperty("levain.cache.dir", originalCache);
                } else {
                    System.clearProperty("levain.cache.dir");
                }
            }
        }
    }

    @Test
    void shouldHandleInitFailureGracefully() {
        GitRepository badRepo = new GitRepository("file:///nonexistent/levain-pkgs.git");
        badRepo.init();

        assertTrue(badRepo.isInitialized());
        assertTrue(badRepo.listRecipes().isEmpty());
    }

    @Test
    void shouldUseEnvCacheDirectoryWhenConfigured() throws Exception {
        String originalProp = System.getProperty("levain.cache.dir");
        String originalEnv = System.getenv("LEVAIN_CACHE_DIR");
        try {
            System.clearProperty("levain.cache.dir");
            updateEnv("LEVAIN_CACHE_DIR", tempDir.toString());

            String cacheDir = invokeGetDefaultCacheDirectory(repository, "https://example.com/repo");

            assertTrue(cacheDir.startsWith(tempDir.toString()));
        } finally {
            if (originalProp == null) {
                System.clearProperty("levain.cache.dir");
            } else {
                System.setProperty("levain.cache.dir", originalProp);
            }
            restoreEnv("LEVAIN_CACHE_DIR", originalEnv);
        }
    }

    @Test
    void shouldReturnEmptyRecipesWhenLocalRepositoryMissing() throws Exception {
        Map<String, Recipe> recipes = invokeLoadRecipesFromLocalRepository(repository);

        assertTrue(recipes.isEmpty());
    }

    private static Git initRepository(Path originDir) throws GitAPIException {
        return Git.init().setDirectory(originDir.toFile()).call();
    }

    private static void commitAll(Git git, String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.commit()
                .setMessage(message)
                .setAuthor("Test User", "test@example.com")
                .call();
    }

    private static String invokeGetDefaultCacheDirectory(GitRepository repo, String url) throws Exception {
        Method method = GitRepository.class.getDeclaredMethod("getDefaultCacheDirectory", String.class);
        method.setAccessible(true);
        return (String) method.invoke(repo, url);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Recipe> invokeLoadRecipesFromLocalRepository(GitRepository repo) throws Exception {
        Method method = GitRepository.class.getDeclaredMethod("loadRecipesFromLocalRepository");
        method.setAccessible(true);
        return (Map<String, Recipe>) method.invoke(repo);
    }

    @SuppressWarnings("unchecked")
    private static void updateEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
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
