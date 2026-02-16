package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckChainDirExistsActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSelectFirstExistingDirAndSaveVariable() throws Exception {
        Path first = Files.createDirectories(tempDir.resolve("first"));
        Path second = Files.createDirectories(tempDir.resolve("second"));

        CheckChainDirExistsAction action = new CheckChainDirExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of("--saveVar=found", first.toString(), second.toString()));

        assertEquals(first.toString(), context.getRecipeVariable("found"));
        assertEquals(first.toString(), context.getConfig().getVariable("found"));
    }

    @Test
    void shouldUseDefaultWhenNoDirExists() throws Exception {
        CheckChainDirExistsAction action = new CheckChainDirExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of("--saveVar", "selected", "--default", "fallback", "missing1", "missing2"));

        Path expected = tempDir.resolve("fallback").toAbsolutePath().normalize();
        assertEquals(expected.toString(), context.getRecipeVariable("selected"));
    }

    @Test
    void shouldThrowWhenNoDirAndNoDefault() {
        CheckChainDirExistsAction action = new CheckChainDirExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("missing1", "missing2")));
    }

    @Test
    void shouldRejectMissingSaveVarValue() {
        CheckChainDirExistsAction action = new CheckChainDirExistsAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--saveVar")));
    }
}
