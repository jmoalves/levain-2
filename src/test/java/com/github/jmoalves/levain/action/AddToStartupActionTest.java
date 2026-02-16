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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddToStartupActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldNoOpOnNonWindows() throws Exception {
        Assumptions.assumeFalse(EnvironmentUtils.isWindows());

        AddToStartupAction action = new AddToStartupAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        Path target = tempDir.resolve("tool.cmd");
        Files.writeString(target, "echo test");

        assertDoesNotThrow(() -> action.execute(context, List.of(target.toString())));
    }

    @Test
    void shouldCreateShortcutOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startup = tempDir.resolve("Startup");
            TestAddToStartupAction action = new TestAddToStartupAction(startup);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.cmd");
            Files.writeString(target, "echo test");

            action.execute(context, List.of(target.toString(), "Boot Tool"));

            Path shortcut = startup.resolve("Boot Tool.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldCopyLnkShortcut() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            Path startup = tempDir.resolve("Startup");
            TestAddToStartupAction action = new TestAddToStartupAction(startup);
            ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

            Path target = tempDir.resolve("tool.lnk");
            Files.writeString(target, "shortcut");

            action.execute(context, List.of(target.toString(), "MyShortcut"));

            Path shortcut = startup.resolve("MyShortcut.lnk");
            assertTrue(Files.exists(shortcut));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    private static class TestAddToStartupAction extends AddToStartupAction {
        private final Path startupDir;

        private TestAddToStartupAction(Path startupDir) {
            this.startupDir = startupDir;
        }

        @Override
        protected Path resolveStartupDir() {
            return startupDir;
        }

        @Override
        protected void createShortcut(Path target, Path shortcut) throws IOException {
            Files.createDirectories(shortcut.getParent());
            Files.writeString(shortcut, "shortcut");
        }
    }
}
