package com.github.jmoalves.levain.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSetActionTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateFileAndSetNestedArrayValues() throws Exception {
        Path settings = tempDir.resolve("settings.json");
        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of(
                settings.toString(),
                "[java.configuration.runtimes][0][name]",
                "JavaSE-21"));

        JsonNode root = mapper.readTree(settings.toFile());
        JsonNode name = root.path("java.configuration.runtimes").path(0).path("name");
        assertEquals("JavaSE-21", name.asText());
    }

    @Test
    void shouldSetBooleanValues() throws Exception {
        Path settings = tempDir.resolve("settings.json");
        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of(
                settings.toString(),
                "[http.proxyStrictSSL]",
                "false"));

        JsonNode root = mapper.readTree(settings.toFile());
        JsonNode value = root.path("http.proxyStrictSSL");
        assertNotNull(value);
        assertTrue(value.isBoolean());
        assertEquals(false, value.asBoolean());
    }

    @Test
    void shouldResetToObjectWhenExistingIsArray() throws Exception {
        Path settings = tempDir.resolve("settings.json");
        Files.writeString(settings, "[]");

        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        action.execute(context, List.of(
                settings.toString(),
                "[name]",
                "demo"));

        JsonNode root = mapper.readTree(settings.toFile());
        assertEquals("demo", root.path("name").asText());
    }

    @Test
    void shouldRejectInvalidPath() {
        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("file.json", "invalid", "value")));
    }

    @Test
    void shouldRejectMissingContext() {
        JsonSetAction action = new JsonSetAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("file.json", "[name]", "value")));
    }

    @Test
    void shouldRejectArrayIndexOnObjectRoot() {
        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("settings.json", "[0]", "value")));
    }

    @Test
    void shouldRejectArrayIndexWhenExistingIsObject() throws Exception {
        Path settings = tempDir.resolve("settings.json");
        Files.writeString(settings, "{\"arr\": {}}\n");

        JsonSetAction action = new JsonSetAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of(settings.toString(), "[arr][0]", "value")));
    }
}
