package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RemoveFromRegistryActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRemoveRegistryFiles() throws Exception {
        Config config = new Config();
        config.setRegistryDir(tempDir.toString());
        ActionContext context = new ActionContext(config, new Recipe(), tempDir, tempDir);

        Path recipe = tempDir.resolve("demo.levain.yaml");
        Path meta = tempDir.resolve("demo.levain.meta");
        Files.writeString(recipe, "name: demo");
        Files.writeString(meta, "{}");

        RemoveFromRegistryAction action = new RemoveFromRegistryAction();
        action.execute(context, List.of("demo"));

        assertFalse(Files.exists(recipe));
        assertFalse(Files.exists(meta));
    }

    @Test
    void shouldNoOpWhenMissing() {
        Config config = new Config();
        config.setRegistryDir(tempDir.toString());
        ActionContext context = new ActionContext(config, new Recipe(), tempDir, tempDir);

        RemoveFromRegistryAction action = new RemoveFromRegistryAction();
        assertDoesNotThrow(() -> action.execute(context, List.of("missing")));
    }
}
