package com.github.jmoalves.levain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSplitAndJoinPath() {
        String sep = EnvironmentUtils.pathSeparator();
        String value = String.join(sep, List.of("/bin", "/usr/bin", " ", "/opt/bin"));

        List<String> parts = EnvironmentUtils.splitPath(value);

        assertEquals(List.of("/bin", "/usr/bin", "/opt/bin"), parts);
        assertEquals(String.join(sep, List.of("/bin", "/usr/bin", "/opt/bin")),
                EnvironmentUtils.joinPath(parts));
    }

    @Test
    void shouldHandleNullAndEmptyPathValues() {
        assertEquals(List.of(), EnvironmentUtils.splitPath(null));
        assertEquals(List.of(), EnvironmentUtils.splitPath(""));
        assertEquals("", EnvironmentUtils.joinPath(null));
        assertEquals("", EnvironmentUtils.joinPath(List.of()));
    }

    @Test
    void shouldUpdatePathWithPrependAndAppend() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            String sep = EnvironmentUtils.pathSeparator();
            String current = String.join(sep, List.of("a", "b"));
            List<String> additions = List.of("b", "c", " ");

            String prepended = EnvironmentUtils.updatePath(current, additions, true);
            String appended = EnvironmentUtils.updatePath(current, additions, false);
            String unchanged = EnvironmentUtils.updatePath(current, null, false);

            assertEquals(String.join(sep, List.of("b", "c", "a")), prepended);
            assertEquals(String.join(sep, List.of("a", "b", "c")), appended);
            assertEquals(current, unchanged);
        } finally {
            restoreOsName(originalOs);
        }
    }

    @Test
    void shouldDeduplicateCaseInsensitiveOnWindows() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");

            String sep = EnvironmentUtils.pathSeparator();
            String current = String.join(sep, List.of("Foo", "Bar"));
            List<String> additions = List.of("foo", "Baz");

            String updated = EnvironmentUtils.updatePath(current, additions, true);

            assertEquals(String.join(sep, List.of("foo", "Baz", "Bar")), updated);
        } finally {
            restoreOsName(originalOs);
        }
    }

    @Test
    void shouldResolveProfilePathFromOverride() {
        Path override = tempDir.resolve("custom.profile");
        String original = System.getProperty("levain.env.profile");
        try {
            System.setProperty("levain.env.profile", override.toString());

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(override, resolved);
        } finally {
            restoreProperty("levain.env.profile", original);
        }
    }

    @Test
    void shouldResolveProfilePathForZsh() throws Exception {
        String original = System.getenv("SHELL");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("SHELL", "/bin/zsh");
            System.setProperty("user.home", tempDir.toString());

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(tempDir.resolve(".zshrc"), resolved);
        } finally {
            restoreEnv("SHELL", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    void shouldResolveProfilePathForBash() throws Exception {
        String original = System.getenv("SHELL");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("SHELL", "/bin/bash");
            System.setProperty("user.home", tempDir.toString());

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(tempDir.resolve(".bashrc"), resolved);
        } finally {
            restoreEnv("SHELL", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    void shouldResolveProfilePathForOtherShells() throws Exception {
        String original = System.getenv("SHELL");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("SHELL", "/bin/fish");
            System.setProperty("user.home", tempDir.toString());

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(tempDir.resolve(".profile"), resolved);
        } finally {
            restoreEnv("SHELL", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    void shouldPersistUnixEnvAndReplaceExisting() throws IOException {
        Path profile = tempDir.resolve(".profile");
        Files.writeString(profile, "export TEST=old\n", StandardCharsets.UTF_8);

        EnvironmentUtils.persistUnixEnv(profile, "TEST", "new value");

        List<String> lines = Files.readAllLines(profile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("export TEST=\"new value\"", lines.get(0));
    }

    @Test
    void shouldPersistUnixEnvWhenFileMissing() throws IOException {
        Path profile = tempDir.resolve("nested").resolve(".profile");

        EnvironmentUtils.persistUnixEnv(profile, "TEST", "value");

        List<String> lines = Files.readAllLines(profile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("export TEST=value", lines.get(0));
    }

    @Test
    void shouldPersistEmptyUnixEnvValue() throws IOException {
        Path profile = tempDir.resolve("empty.profile");

        EnvironmentUtils.persistUnixEnv(profile, "EMPTY", "");

        List<String> lines = Files.readAllLines(profile, StandardCharsets.UTF_8);
        assertEquals("export EMPTY=\"\"", lines.get(0));
    }

    @Test
    void shouldPersistQuotedValueWithQuotes() throws IOException {
        Path profile = tempDir.resolve("quoted.profile");

        EnvironmentUtils.persistUnixEnv(profile, "QUOTED", "a\"b");

        List<String> lines = Files.readAllLines(profile, StandardCharsets.UTF_8);
        assertEquals("export QUOTED=\"a\\\"b\"", lines.get(0));
    }

    @Test
    void shouldReturnNullProfileWhenHomeMissing() {
        String original = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "");
            System.clearProperty("levain.env.profile");

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertNull(resolved);
        } finally {
            restoreProperty("user.home", original);
        }
    }

    @Test
    void shouldFailWhenProfileMissing() {
        assertThrows(IOException.class, () -> EnvironmentUtils.persistUnixEnv(null, "KEY", "value"));
    }

    @Test
    void shouldHandleNullOsName() {
        String original = System.getProperty("os.name");
        try {
            System.clearProperty("os.name");

            boolean value = EnvironmentUtils.isWindows();

            assertEquals(false, value);
        } finally {
            restoreProperty("os.name", original);
        }
    }

    @Test
    void shouldNormalizeNullPathValue() throws Exception {
        String original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            var normalize = EnvironmentUtils.class.getDeclaredMethod("normalizeForCompare", String.class);
            normalize.setAccessible(true);
            String value = (String) normalize.invoke(null, new Object[] { null });

            assertEquals("", value);
        } finally {
            restoreProperty("os.name", original);
        }
    }

    @Test
    void shouldQuoteNullValue() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, new Object[] { null });
        assertEquals("\"\"", value);
    }

    @Test
    void shouldQuoteEmptyValue() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, "");
        assertEquals("\"\"", value);
    }

    @Test
    void shouldQuoteValueWithSpaces() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, "hello world");
        assertEquals("\"hello world\"", value);
    }

    @Test
    void shouldQuoteValueWithTabs() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, "hello\tworld");
        assertEquals("\"hello\tworld\"", value);
    }

    @Test
    void shouldEscapeQuotesInValue() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, "say \"hello\"");
        assertEquals("\"say \\\"hello\\\"\"", value);
    }

    @Test
    void shouldNotQuoteSimpleValue() throws Exception {
        var quote = EnvironmentUtils.class.getDeclaredMethod("quote", String.class);
        quote.setAccessible(true);
        String value = (String) quote.invoke(null, "simple");
        assertEquals("simple", value);
    }

    @Test
    void shouldBuildExportLine() throws Exception {
        var buildExport = EnvironmentUtils.class.getDeclaredMethod("buildExportLine", String.class, String.class);
        buildExport.setAccessible(true);
        String line = (String) buildExport.invoke(null, "MY_VAR", "my value");
        assertEquals("export MY_VAR=\"my value\"", line);
    }

    @Test
    void shouldPersistUnixEnvToNewFile() throws IOException {
        Path profile = tempDir.resolve("new").resolve(".bashrc");
        
        EnvironmentUtils.persistUnixEnv(profile, "NEW_VAR", "test");

        List<String> lines = Files.readAllLines(profile);
        assertEquals(1, lines.size());
        assertEquals("export NEW_VAR=test", lines.get(0));
    }

    @Test
    void shouldUpdatePathWithEmptyCurrentPath() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            String result = EnvironmentUtils.updatePath("", List.of("a", "b"), true);

            assertEquals("a" + EnvironmentUtils.pathSeparator() + "b", result);
        } finally {
            restoreOsName(originalOs);
        }
    }

    @Test
    void shouldUpdatePathWithNullCurrentPath() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            String result = EnvironmentUtils.updatePath(null, List.of("a", "b"), false);

            assertEquals("a" + EnvironmentUtils.pathSeparator() + "b", result);
        } finally {
            restoreOsName(originalOs);
        }
    }

    @Test
    void shouldHandleBlankPathPartsInSplit() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            String sep = EnvironmentUtils.pathSeparator();

            List<String> parts = EnvironmentUtils.splitPath("a" + sep + "  " + sep + "b");

            assertEquals(List.of("a", "b"), parts);
        } finally {
            restoreOsName(originalOs);
        }
    }

    @Test
    void shouldResolveProfilePathWhenShellIsNull() throws Exception {
        String original = System.getenv("SHELL");
        String originalHome = System.getProperty("user.home");
        try {
            updateEnv("SHELL", null);
            System.setProperty("user.home", tempDir.toString());

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(tempDir.resolve(".profile"), resolved);
        } finally {
            restoreEnv("SHELL", original);
            restoreProperty("user.home", originalHome);
        }
    }

    @Test
    void shouldHandleBlankUserHomeInResolveProfile() {
        String original = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "   ");

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertNull(resolved);
        } finally {
            restoreProperty("user.home", original);
        }
    }

    @Test
    void shouldHandleBlankOverrideInResolveProfile() throws Exception {
        String original = System.getProperty("levain.env.profile");
        String originalHome = System.getProperty("user.home");
        String originalShell = System.getenv("SHELL");
        try {
            System.setProperty("levain.env.profile", "   ");
            System.setProperty("user.home", tempDir.toString());
            updateEnv("SHELL", null);

            Path resolved = EnvironmentUtils.resolveProfilePath();

            assertEquals(tempDir.resolve(".profile"), resolved);
        } finally {
            restoreProperty("levain.env.profile", original);
            restoreProperty("user.home", originalHome);
            restoreEnv("SHELL", originalShell);
        }
    }

    private void restoreOsName(String original) {
        restoreProperty("os.name", original);
    }

    private void restoreProperty(String key, String original) {
        if (original == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, original);
    }

    @SuppressWarnings("unchecked")
    private static void updateEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> mutable = (Map<String, String>) field.get(env);
        if (value == null) {
            mutable.remove(key);
        } else {
            mutable.put(key, value);
        }
    }

    private static void restoreEnv(String key, String original) throws Exception {
        updateEnv(key, original);
    }
}
