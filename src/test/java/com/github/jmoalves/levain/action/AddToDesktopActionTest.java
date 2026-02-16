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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
