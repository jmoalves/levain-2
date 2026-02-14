package com.github.jmoalves.levain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void shouldUpdatePathWithPrependAndAppend() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            String sep = EnvironmentUtils.pathSeparator();
            String current = String.join(sep, List.of("a", "b"));
            List<String> additions = List.of("b", "c", " ");

            String prepended = EnvironmentUtils.updatePath(current, additions, true);
            String appended = EnvironmentUtils.updatePath(current, additions, false);

            assertEquals(String.join(sep, List.of("b", "c", "a")), prepended);
            assertEquals(String.join(sep, List.of("a", "b", "c")), appended);
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
}
