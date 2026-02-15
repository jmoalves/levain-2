package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloneActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCloneRepositoryToDestination() throws Exception {
        Path originDir = tempDir.resolve("origin");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(baseDir);
        Path repoDir = baseDir.resolve("clone");

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        action.execute(context, List.of(originDir.toUri().toString(), "clone"));

        assertTrue(Files.exists(repoDir.resolve(".git")));
        assertTrue(Files.exists(repoDir.resolve("README.md")));
    }

    @Test
    void shouldRejectMissingArguments() {
        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("https://example.com/repo.git")));
    }

    @Test
    void shouldUseUrlBranchWhenNoOverride() throws Exception {
        Path originDir = tempDir.resolve("origin-branch.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            origin.checkout().setCreateBranch(true).setName("dev").call();
            Files.writeString(originDir.resolve("DEV.md"), "dev\n");
            commitAll(origin, "dev commit");
        }

        Path baseDir = tempDir.resolve("base-branch");
        Files.createDirectories(baseDir);
        Path repoDir = baseDir.resolve("clone");

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        String url = trimTrailingSlash(originDir.toUri().toString()) + "#dev";
        action.execute(context, List.of("--recursive", url, "clone"));

        try (Git clone = Git.open(repoDir.toFile())) {
            assertEquals("dev", clone.getRepository().getBranch());
        }
    }

    @Test
    void shouldPreferExplicitBranchOverUrlBranch() throws Exception {
        Path originDir = tempDir.resolve("origin-override.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            origin.checkout().setCreateBranch(true).setName("dev").call();
            Files.writeString(originDir.resolve("DEV.md"), "dev\n");
            commitAll(origin, "dev commit");
        }

        Path baseDir = tempDir.resolve("base-override");
        Files.createDirectories(baseDir);
        Path repoDir = baseDir.resolve("clone");

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        String url = trimTrailingSlash(originDir.toUri().toString()) + "#main";
        action.execute(context, List.of("--branch", "dev", url, "clone"));

        try (Git clone = Git.open(repoDir.toFile())) {
            assertEquals("dev", clone.getRepository().getBranch());
        }
    }

    @Test
    void shouldAcceptDepthValue() throws Exception {
        Path originDir = tempDir.resolve("origin-depth.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        Path baseDir = tempDir.resolve("base-depth");
        Files.createDirectories(baseDir);
        Path repoDir = baseDir.resolve("clone");

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        String url = trimTrailingSlash(originDir.toUri().toString());
        action.execute(context, List.of("--depth", "1", url, "clone"));

        assertTrue(Files.exists(repoDir.resolve(".git")));
    }

    @Test
    void shouldCreateDirectoriesWhenParentMissing() throws Exception {
        Path originDir = tempDir.resolve("origin-parent.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        Path baseDir = Path.of("");
        Path repoDir = Path.of("tmp-clone-parentless").toAbsolutePath();
        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        String url = trimTrailingSlash(originDir.toUri().toString());
        try {
            action.execute(context, List.of(url, "tmp-clone-parentless"));
            assertTrue(Files.exists(repoDir.resolve(".git")));
        } finally {
            deleteDirectory(repoDir);
        }
    }

    @Test
    void shouldPullWhenRepositoryAlreadyExists() throws Exception {
        Path originDir = tempDir.resolve("origin-pull");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        Path baseDir = tempDir.resolve("base-pull");
        Files.createDirectories(baseDir);
        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        action.execute(context, List.of(originDir.toUri().toString(), "repo"));

        try (Git origin = Git.open(originDir.toFile())) {
            Files.writeString(originDir.resolve("CHANGELOG.md"), "v1\n");
            commitAll(origin, "add changelog");
        }

        action.execute(context, List.of(originDir.toUri().toString(), "repo"));

        assertTrue(Files.exists(baseDir.resolve("repo").resolve("CHANGELOG.md")));
    }

    @Test
    void shouldRejectExistingNonGitDirectory() throws Exception {
        Path baseDir = tempDir.resolve("base-existing");
        Files.createDirectories(baseDir.resolve("target"));

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("https://example.com/repo.git", "target")));
    }

    @Test
    void shouldRejectDestinationFile() throws Exception {
        Path baseDir = tempDir.resolve("base-file");
        Files.createDirectories(baseDir);
        Files.writeString(baseDir.resolve("target"), "not a dir");

        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, baseDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("https://example.com/repo.git", "target")));
    }

    @Test
    void shouldRequireBranchValue() {
        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--branch")));
    }

    @Test
    void shouldRequireDepthValue() {
        CloneAction action = new CloneAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--depth")));
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

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Best-effort cleanup for test artifacts.
                        }
                    });
        } catch (Exception e) {
            // Best-effort cleanup for test artifacts.
        }
    }
}
