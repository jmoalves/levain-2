package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddToStartMenuActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldNoOpOnNonWindows() throws Exception {
        AddToStartMenuAction action = new AddToStartMenuAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        Path target = tempDir.resolve("tool.cmd");
        Files.writeString(target, "echo test");

        if (!EnvironmentUtils.isWindows()) {
            assertDoesNotThrow(() -> action.execute(context, List.of(target.toString(), "dev-env")));
        }
    }

    @Test
    void shouldCreateShortcutOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startMenu = tempDir.resolve("Programs");
            TestAddToStartMenuAction action = new TestAddToStartMenuAction(startMenu);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.cmd");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString(), "dev-env"));

            Path shortcut = startMenu.resolve("dev-env").resolve("tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyUrlShortcutOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startMenu = tempDir.resolve("Programs");
            TestAddToStartMenuAction action = new TestAddToStartMenuAction(startMenu);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.url");
            Files.writeString(target, "[InternetShortcut]\nURL=https://example.com");

            action.execute(context, List.of(target.toString(), "dev-env"));

            Path shortcut = startMenu.resolve("dev-env").resolve("tool.url");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingArgs() {
        AddToStartMenuAction action = new AddToStartMenuAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    @Test
    void shouldRejectMissingTargetOnWindows() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            AddToStartMenuAction action = new AddToStartMenuAction();
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(context, List.of("missing.exe")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyLnkShortcutOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startMenu = tempDir.resolve("Programs");
            TestAddToStartMenuAction action = new TestAddToStartMenuAction(startMenu);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.lnk");
            Files.writeString(target, "shortcut");

            action.execute(context, List.of(target.toString()));

            Path shortcut = startMenu.resolve("tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldUseBaseDirWhenGroupBlank() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startMenu = tempDir.resolve("Programs");
            TestAddToStartMenuAction action = new TestAddToStartMenuAction(startMenu);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.cmd");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString(), " "));

            Path shortcut = startMenu.resolve("tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCreateShortcutWhenTargetHasNoExtension() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startMenu = tempDir.resolve("Programs");
            TestAddToStartMenuAction action = new TestAddToStartMenuAction(startMenu);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString()));

            Path shortcut = startMenu.resolve("tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    private static class TestAddToStartMenuAction extends AddToStartMenuAction {
        private final Path startMenuDir;

        private TestAddToStartMenuAction(Path startMenuDir) {
            this.startMenuDir = startMenuDir;
        }

        @Override
        protected Path resolveStartMenuDir(String group) {
            if (group == null || group.isBlank()) {
                return startMenuDir;
            }
            return startMenuDir.resolve(group);
        }

        @Override
        protected void createShortcut(Path target, Path shortcut) throws IOException {
            Files.createDirectories(shortcut.getParent());
            Files.writeString(shortcut, "shortcut");
        }
    }
}
