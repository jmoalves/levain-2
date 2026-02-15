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
}
