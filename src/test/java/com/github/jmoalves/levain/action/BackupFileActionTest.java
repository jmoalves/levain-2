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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupFileActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateBackupAndOverwrite() throws Exception {
        Path file = tempDir.resolve("config.txt");
        Files.writeString(file, "one");

        BackupFileAction action = new BackupFileAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of(file.toString()));

        Path backup = tempDir.resolve("config.txt.bak");
        assertTrue(Files.exists(backup));
        assertEquals("one", Files.readString(backup));

        Files.writeString(file, "two");
        action.execute(context, List.of(file.toString()));
        assertEquals("two", Files.readString(backup));
    }

    @Test
    void shouldCreateBackupWithCustomSuffix() throws Exception {
        Path file = tempDir.resolve("settings.ini");
        Files.writeString(file, "data");

        BackupFileAction action = new BackupFileAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of("--suffix=.orig", file.toString()));

        Path backup = tempDir.resolve("settings.ini.orig");
        assertTrue(Files.exists(backup));
    }

    @Test
    void shouldRejectExistingBackupWhenNoOverwrite() throws Exception {
        Path file = tempDir.resolve("settings.ini");
        Files.writeString(file, "data");

        BackupFileAction action = new BackupFileAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of(file.toString()));

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--no-overwrite", file.toString())));
    }
}
