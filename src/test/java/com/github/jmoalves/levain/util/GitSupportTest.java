package com.github.jmoalves.levain.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCloneWithBranchDepthRecursive() throws Exception {
        Path originDir = tempDir.resolve("origin");
        Files.createDirectories(originDir);

        String branchName;
        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            branchName = origin.getRepository().getBranch();
        }

        Path cloneDir = tempDir.resolve("clone");
        GitSupport.cloneRepository(originDir.toUri().toString(), cloneDir, branchName, 1, true);

        assertTrue(Files.exists(cloneDir.resolve(".git")));
        assertTrue(Files.exists(cloneDir.resolve("README.md")));
    }

    @Test
    void shouldCloneWithoutOptionsAndPull() throws Exception {
        Path originDir = tempDir.resolve("origin-pull");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        Path cloneDir = tempDir.resolve("clone-pull");
        GitSupport.cloneRepository(originDir.toUri().toString(), cloneDir, " ", 0, false);

        try (Git origin = Git.open(originDir.toFile())) {
            Files.writeString(originDir.resolve("CHANGELOG.md"), "v1\n");
            commitAll(origin, "add changelog");
        }

        GitSupport.pullRepository(cloneDir);

        assertTrue(Files.exists(cloneDir.resolve("CHANGELOG.md")));
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
