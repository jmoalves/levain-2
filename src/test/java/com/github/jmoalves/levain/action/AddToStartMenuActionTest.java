package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
}
