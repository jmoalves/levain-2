package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckFileExistsActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPassWhenFileExists() throws Exception {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "ok");

        CheckFileExistsAction action = new CheckFileExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertDoesNotThrow(() -> action.execute(context, List.of(file.toString())));
    }

    @Test
    void shouldThrowWhenFileMissing() {
        CheckFileExistsAction action = new CheckFileExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of(tempDir.resolve("missing.txt").toString())));
    }

    @Test
    void shouldThrowWhenPathIsDirectory() throws Exception {
        Path dir = tempDir.resolve("dir");
        Files.createDirectories(dir);

        CheckFileExistsAction action = new CheckFileExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of(dir.toString())));
    }

    @Test
    void shouldRejectMissingContext() {
        CheckFileExistsAction action = new CheckFileExistsAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("file.txt")));
    }
}
