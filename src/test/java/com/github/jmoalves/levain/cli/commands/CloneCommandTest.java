package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.ShellService;
import com.github.jmoalves.levain.util.GitUrlUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CloneCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCloneWithOptionsAndOpenShell() throws Exception {
        Path originDir = tempDir.resolve("origin-test.git");
        Files.createDirectories(originDir);

        String branchName;
        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            branchName = origin.getRepository().getBranch();
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        doNothing().when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        String url = trimTrailingSlash(originDir.toUri().toString());
        int exit = cmd.execute("--branch", branchName, "--depth", "1", "--recursive", url);
        assertEquals(0, exit);

        Path expectedRepoDir = Path.of("").toAbsolutePath()
                .resolve(GitUrlUtils.deriveRepoName(url));
        assertTrue(Files.exists(expectedRepoDir.resolve(".git")));

        ArgumentCaptor<Path> repoCaptor = ArgumentCaptor.forClass(Path.class);
        verify(shellService).openShell(eq(List.of()), repoCaptor.capture());
        assertEquals(expectedRepoDir, repoCaptor.getValue());
        deleteDirectory(expectedRepoDir);
    }

    @Test
    void shouldRejectInvalidGitUrl() throws Exception {
        ShellService shellService = Mockito.mock(ShellService.class);
        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute("https://github.com/example/project");
        assertEquals(1, exit);

        verify(shellService, never()).openShell(any(), any(Path.class));
    }

    @Test
    void shouldUseDirsOptionWhenUserPresent() throws Exception {
        ShellService shellService = Mockito.mock(ShellService.class);
        doNothing().when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            Path repoDir = Path.of("").toAbsolutePath().resolve("example").resolve("project");
            Files.createDirectories(repoDir);
            int exit = cmd.execute("--dirs", "https://github.com/example/project.git");
            assertEquals(0, exit);

            verify(shellService).openShell(eq(List.of()), eq(repoDir));
        } finally {
            if (originalUserDir != null) {
                System.setProperty("user.dir", originalUserDir);
            }
        }
    }

    @Test
    void shouldUseUrlBranchWhenNoBranchOption() throws Exception {
        Path originDir = tempDir.resolve("origin-branch.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            origin.checkout().setCreateBranch(true).setName("dev").call();
            Files.writeString(originDir.resolve("DEV.md"), "dev\n");
            commitAll(origin, "dev commit");
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        doNothing().when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        Path repoDir = null;
        try {
            String url = trimTrailingSlash(originDir.toUri().toString()) + "#dev";
            int exit = cmd.execute(url);
            assertEquals(0, exit);

            repoDir = Path.of("").toAbsolutePath()
                    .resolve(GitUrlUtils.deriveRepoName(url));
            try (Git clone = Git.open(repoDir.toFile())) {
                assertEquals("dev", clone.getRepository().getBranch());
            }

            verify(shellService).openShell(eq(List.of()), eq(repoDir));
        } finally {
            if (originalUserDir != null) {
                System.setProperty("user.dir", originalUserDir);
            }
            deleteDirectory(repoDir);
        }
    }

    @Test
    void shouldSkipCloneWhenRepoDirExists() throws Exception {
        Path originDir = tempDir.resolve("origin-exists.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        doNothing().when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        Path repoDir = null;
        try {
            String url = trimTrailingSlash(originDir.toUri().toString());
            repoDir = Path.of("").toAbsolutePath()
                    .resolve(GitUrlUtils.deriveRepoName(url));
            Files.createDirectories(repoDir);

            int exit = cmd.execute(url);
            assertEquals(0, exit);
            assertTrue(Files.exists(repoDir));
            assertTrue(Files.notExists(repoDir.resolve(".git")));

            verify(shellService).openShell(eq(List.of()), eq(repoDir));
        } finally {
            if (originalUserDir != null) {
                System.setProperty("user.dir", originalUserDir);
            }
            deleteDirectory(repoDir);
        }
    }

    @Test
    void shouldReturnFailureWhenShellFails() throws Exception {
        Path originDir = tempDir.resolve("origin-shell.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        Mockito.doThrow(new IllegalStateException("boom"))
                .when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        Path repoDir = null;
        try {
            String url = trimTrailingSlash(originDir.toUri().toString());
            repoDir = Path.of("").toAbsolutePath()
                    .resolve(GitUrlUtils.deriveRepoName(url));

            int exit = cmd.execute(url);
            assertEquals(1, exit);
        } finally {
            if (originalUserDir != null) {
                System.setProperty("user.dir", originalUserDir);
            }
            deleteDirectory(repoDir);
        }
    }

    @Test
    void shouldTreatBlankBranchAsUnset() throws Exception {
        Path originDir = tempDir.resolve("origin-blank.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
            origin.checkout().setCreateBranch(true).setName("dev").call();
            Files.writeString(originDir.resolve("DEV.md"), "dev\n");
            commitAll(origin, "dev commit");
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        doNothing().when(shellService).openShell(any(), any(Path.class));

        CloneCommand command = new CloneCommand(shellService);

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        Path repoDir = null;
        try {
            String url = trimTrailingSlash(originDir.toUri().toString()) + "#dev";
            setField(command, "url", url);
            setField(command, "branch", " ");

            int exit = command.call();
            assertEquals(0, exit);

            repoDir = Path.of("").toAbsolutePath()
                    .resolve(GitUrlUtils.deriveRepoName(url));
            try (Git clone = Git.open(repoDir.toFile())) {
                assertEquals("dev", clone.getRepository().getBranch());
            }
        } finally {
            if (originalUserDir != null) {
                System.setProperty("user.dir", originalUserDir);
            }
            deleteDirectory(repoDir);
        }
    }

    @Test
    void shouldRejectDirsWhenUserBlank() {
        ShellService shellService = Mockito.mock(ShellService.class);
        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute("--dirs", "https://github.com/ /project.git");
        assertEquals(1, exit);
    }

    @Test
    void shouldRejectDirsWhenParentIsNotDirectory() {
        ShellService shellService = Mockito.mock(ShellService.class);
        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        Path parentDir = Path.of("").toAbsolutePath().resolve("example");
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.createDirectories(parentDir)).thenReturn(parentDir);
            files.when(() -> Files.isDirectory(parentDir)).thenReturn(false);

            int exit = cmd.execute("--dirs", "https://github.com/example/project.git");
            assertEquals(1, exit);
        }
    }

    @Test
    void shouldRejectDirsWhenUserMissing() throws Exception {
        Path originDir = tempDir.resolve("origin-dir.git");
        Files.createDirectories(originDir);

        try (Git origin = initRepository(originDir)) {
            Files.writeString(originDir.resolve("README.md"), "hello\n");
            commitAll(origin, "init");
        }

        ShellService shellService = Mockito.mock(ShellService.class);
        CloneCommand command = new CloneCommand(shellService);
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute("--dirs", trimTrailingSlash(originDir.toUri().toString()));
        assertEquals(1, exit);

        verify(shellService, never()).openShell(any(), any(Path.class));
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
        try (var walk = java.nio.file.Files.walk(dir)) {
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

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
