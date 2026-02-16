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

class AssertContainsActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPassWhenContentMatches() throws Exception {
        Path file = tempDir.resolve("note.txt");
        Files.writeString(file, "hello world");

        AssertContainsAction action = new AssertContainsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertDoesNotThrow(() -> action.execute(context, List.of(file.toString(), "hello")));
    }

    @Test
    void shouldThrowWhenMissingText() throws Exception {
        Path file = tempDir.resolve("note.txt");
        Files.writeString(file, "hello world");

        AssertContainsAction action = new AssertContainsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of(file.toString(), "missing")));
    }
}
