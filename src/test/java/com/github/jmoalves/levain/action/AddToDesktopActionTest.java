package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddToDesktopActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldNoOpOnNonWindows() throws Exception {
        Assumptions.assumeFalse(EnvironmentUtils.isWindows());

        AddToDesktopAction action = new AddToDesktopAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        Path target = tempDir.resolve("tool.cmd");
        Files.writeString(target, "echo test");

        assertDoesNotThrow(() -> action.execute(context, List.of(target.toString(), "Dev Tool")));
    }

    @Test
    void shouldCreateShortcutOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path desktop = tempDir.resolve("Desktop");
            TestAddToDesktopAction action = new TestAddToDesktopAction(desktop);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.cmd");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString(), "My Tool"));

            Path shortcut = desktop.resolve("My Tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyUrlShortcutWithCustomName() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path desktop = tempDir.resolve("Desktop");
            TestAddToDesktopAction action = new TestAddToDesktopAction(desktop);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("site.url");
            Files.writeString(target, "[InternetShortcut]\nURL=https://example.com");

            action.execute(context, List.of(target.toString(), "MyLink"));

            Path shortcut = desktop.resolve("MyLink.url");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingArgs() {
        AddToDesktopAction action = new AddToDesktopAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    @Test
    void shouldRejectMissingTargetOnWindows() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            AddToDesktopAction action = new AddToDesktopAction();
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(context, List.of("missing.exe")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyUrlShortcutWithBlankName() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path desktop = tempDir.resolve("Desktop");
            TestAddToDesktopAction action = new TestAddToDesktopAction(desktop);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("site.url");
            Files.writeString(target, "[InternetShortcut]\nURL=https://example.com");

            action.execute(context, List.of(target.toString(), " "));

            Path shortcut = desktop.resolve("site.url");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyUrlShortcutWhenNameHasExtension() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path desktop = tempDir.resolve("Desktop");
            TestAddToDesktopAction action = new TestAddToDesktopAction(desktop);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("site.url");
            Files.writeString(target, "[InternetShortcut]\nURL=https://example.com");

            action.execute(context, List.of(target.toString(), "Custom.url"));

            Path shortcut = desktop.resolve("Custom.url");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldResolveDesktopDirFromUserProfile() throws Exception {
        Map<String, String> original = snapshotEnv("USERPROFILE");
        String originalHome = System.getProperty("user.home");
        try {
            setEnvVar("USERPROFILE", tempDir.toString());
            System.setProperty("user.home", tempDir.resolve("other-home").toString());

            AddToDesktopAction action = new AddToDesktopAction();
            Path resolved = action.resolveDesktopDir();

            assertEquals(tempDir.resolve("Desktop"), resolved);
        } finally {
            restoreEnv(original);
            if (originalHome == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", originalHome);
            }
        }
    }

    @Test
    void shouldCreateShortcutWhenTargetHasNoExtension() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path desktop = tempDir.resolve("Desktop");
            TestAddToDesktopAction action = new TestAddToDesktopAction(desktop);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString()));

            Path shortcut = desktop.resolve("tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    private static class TestAddToDesktopAction extends AddToDesktopAction {
        private final Path desktopDir;

        private TestAddToDesktopAction(Path desktopDir) {
            this.desktopDir = desktopDir;
        }

        @Override
        protected Path resolveDesktopDir() {
            return desktopDir;
        }

        @Override
        protected void createShortcut(Path target, Path shortcut) throws IOException {
            Files.createDirectories(shortcut.getParent());
            Files.writeString(shortcut, "shortcut");
        }
    }

    private static Map<String, String> snapshotEnv(String... keys) {
        Map<String, String> snapshot = new java.util.LinkedHashMap<>();
        if (keys == null) {
            return snapshot;
        }
        for (String key : keys) {
            if (key == null || snapshot.containsKey(key)) {
                continue;
            }
            snapshot.put(key, System.getenv(key));
        }
        return snapshot;
    }

    private static void restoreEnv(Map<String, String> snapshot) {
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            if (entry.getValue() == null) {
                setEnvVar(entry.getKey(), null);
            } else {
                setEnvVar(entry.getKey(), entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean setEnvVar(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            if (value == null) {
                writableEnv.remove(key);
            } else {
                writableEnv.put(key, value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
