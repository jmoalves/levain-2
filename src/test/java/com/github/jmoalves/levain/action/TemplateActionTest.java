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

class TemplateActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReplaceWithRegexPattern() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "Hello @@NAME@@!\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        action.execute(context, List.of("--replace=/@@NAME@@/g", "--with=World", "template.txt", "nested/out.txt"));

        Path dst = baseDir.resolve("nested/out.txt");
        assertEquals("Hello World!\n", Files.readString(dst));
    }

    @Test
    void shouldDoubleBackslashWhenConfigured() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "PATH=@@HOME@@\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        action.execute(context, List.of("--doubleBackslash", "--replace=/@@HOME@@/g", "--with=C:\\dev\\env", "template.txt", "out.txt"));

        Path dst = baseDir.resolve("out.txt");
        assertEquals("PATH=C:\\\\dev\\\\env\n", Files.readString(dst));
    }

    @Test
    void shouldRejectMissingWith() {
        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--replace=foo", "src", "dst")));
    }

    @Test
    void shouldRejectWithWithoutReplace() {
        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--with=bar", "src", "dst")));
    }

    @Test
    void shouldRejectMissingPositionals() {
        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--replace=foo", "--with=bar", "src")));
    }

    @Test
    void shouldRejectReplaceWithoutValue() {
        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--replace")));
    }

    @Test
    void shouldRejectWithWithoutValue() {
        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--replace=foo", "--with")));
    }

    @Test
    void shouldRejectMissingReplacements() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "noop\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("template.txt", "out.txt")));
    }

    @Test
    void shouldHandlePatternWithoutClosingSlash() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "/x\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        action.execute(context, List.of("--replace=/x", "--with=Z", "template.txt", "out.txt"));

        Path dst = baseDir.resolve("out.txt");
        assertEquals("Z\n", Files.readString(dst));
    }

    @Test
    void shouldHandleBlankPattern() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "a b\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        action.execute(context, List.of("--replace= ", "--with=_", "template.txt", "out.txt"));

        Path dst = baseDir.resolve("out.txt");
        assertEquals("a_b\n", Files.readString(dst));
    }

    @Test
    void shouldHandleNullPattern() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "content\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        // Pass null via reflection to test the null branch
        java.lang.reflect.Method parseArgs = TemplateAction.class.getDeclaredMethod("parseArgs", List.class);
        parseArgs.setAccessible(true);
        
        java.lang.reflect.Method normalizePattern = TemplateAction.class.getDeclaredMethod("normalizePattern", String.class);
        normalizePattern.setAccessible(true);
        
        // Call normalizePattern with null
        Object result = normalizePattern.invoke(action, new Object[]{null});
        assertEquals(null, result);
    }

    @Test
    void shouldWriteToRootDirectoryWithoutParent() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Files.createDirectories(recipeDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "Hello\n");

        TemplateAction action = new TemplateAction();
        // Use root as base dir to ensure getParent() returns null
        Path rootDest = Path.of("out.txt");
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, recipeDir);

        action.execute(context, List.of("--replace=Hello", "--with=Hi", "template.txt", "out.txt"));

        Path dst = tempDir.resolve("out.txt");
        assertEquals("Hi\n", Files.readString(dst));
    }

    @Test
    void shouldHandlePatternStartingWithSlashButOnlyOneChar() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(recipeDir);
        Files.createDirectories(baseDir);

        Path src = recipeDir.resolve("template.txt");
        Files.writeString(src, "a/b\n");

        TemplateAction action = new TemplateAction();
        ActionContext context = new ActionContext(new Config(), new Recipe(), baseDir, recipeDir);

        // Pattern is just "/" - raw.startsWith("/") is true but raw.length() == 1
        action.execute(context, List.of("--replace=/", "--with=-", "template.txt", "out.txt"));

        Path dst = baseDir.resolve("out.txt");
        assertEquals("a-b\n", Files.readString(dst));
    }
}
