package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteMessageToStdout() {
        EchoAction action = new EchoAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        try {
            action.execute(context, List.of("hello", "world"));
        } finally {
            System.setOut(original);
        }

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertEquals("hello world" + System.lineSeparator(), output);
    }

    @Test
    void shouldHandleEmptyArgs() {
        EchoAction action = new EchoAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        try {
            action.execute(context, List.of());
        } finally {
            System.setOut(original);
        }

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(System.lineSeparator(), output);
    }
}
