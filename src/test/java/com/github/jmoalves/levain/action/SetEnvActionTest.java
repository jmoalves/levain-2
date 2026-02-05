package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SetEnvAction Tests")
class SetEnvActionTest {
    @TempDir
    Path tempDir;

    private SetEnvAction action;
    private ActionContext context;
    private String previousProfileOverride;

    @BeforeEach
    void setUp() {
        action = new SetEnvAction();

        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());

        Recipe recipe = new Recipe();
        recipe.setName("test");
        recipe.setVersion("1.0");

        context = new ActionContext(config, recipe, tempDir, tempDir);

        previousProfileOverride = System.getProperty("levain.env.profile");
        System.setProperty("levain.env.profile", tempDir.resolve("profile").toString());
    }

    @AfterEach
    void tearDown() {
        if (previousProfileOverride == null) {
            System.clearProperty("levain.env.profile");
        } else {
            System.setProperty("levain.env.profile", previousProfileOverride);
        }
    }

    @Test
    @DisplayName("Test 1: setEnv stores variable in config")
    void testSetEnvStoresInConfig() throws Exception {
        action.execute(context, List.of("MY_ENV", "value"));

        assertEquals("value", context.getConfig().getVariable("MY_ENV"));
    }

    @Test
    @DisplayName("Test 2: setEnv overwrites existing variable")
    void testSetEnvOverwrites() throws Exception {
        action.execute(context, List.of("MY_ENV", "old"));
        action.execute(context, List.of("MY_ENV", "new"));

        assertEquals("new", context.getConfig().getVariable("MY_ENV"));
    }

    @Test
    @DisplayName("Test 3: setEnv --permanent writes to profile file")
    void testPermanentWritesProfile() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        action.execute(context, List.of("--permanent", "JAVA_HOME", "/opt/jdk-21"));

        Path profile = EnvironmentUtils.resolveProfilePath();
        assertNotNull(profile);
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export JAVA_HOME=/opt/jdk-21"));
    }

    @Test
    @DisplayName("Test 4: setEnv --permanent replaces existing value")
    void testPermanentReplacesExisting() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        Path profile = EnvironmentUtils.resolveProfilePath();
        Files.writeString(profile, "export PATH=/bin\nexport JAVA_HOME=/old\n", StandardCharsets.UTF_8);

        action.execute(context, List.of("--permanent", "JAVA_HOME", "/new"));

        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export JAVA_HOME=/new"));
        assertFalse(content.contains("export JAVA_HOME=/old"));
        assertTrue(content.contains("export PATH=/bin"));
    }

    @Test
    @DisplayName("Test 5: setEnv --permanent preserves other lines")
    void testPermanentPreservesOtherLines() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        Path profile = EnvironmentUtils.resolveProfilePath();
        Files.writeString(profile, "# existing\nexport PATH=/bin\n", StandardCharsets.UTF_8);

        action.execute(context, List.of("--permanent", "M2_HOME", "/opt/maven"));

        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("# existing"));
        assertTrue(content.contains("export PATH=/bin"));
        assertTrue(content.contains("export M2_HOME=/opt/maven"));
    }

    @Test
    @DisplayName("Test 6: setEnv fails with no args")
    void testNoArgs() {
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    @Test
    @DisplayName("Test 7: setEnv fails when value is missing")
    void testMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("VAR_ONLY")));
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--permanent", "VAR_ONLY")));
    }

    @Test
    @DisplayName("Test 8: setEnv --permanent quotes value with spaces")
    void testPermanentQuotesSpaces() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        action.execute(context, List.of("--permanent", "MY_VAR", "with spaces"));

        Path profile = EnvironmentUtils.resolveProfilePath();
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export MY_VAR=\"with spaces\""));
    }

    @Test
    @DisplayName("Test 9: setEnv --permanent escapes quotes")
    void testPermanentEscapesQuotes() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        action.execute(context, List.of("--permanent", "MY_VAR", "value\"with\"quotes"));

        Path profile = EnvironmentUtils.resolveProfilePath();
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export MY_VAR=\"value\\\"with\\\"quotes\""));
    }

    @Test
    @DisplayName("Test 10: setEnv --permanent allows empty value")
    void testPermanentEmptyValue() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        action.execute(context, List.of("--permanent", "EMPTY_VAR", ""));

        Path profile = EnvironmentUtils.resolveProfilePath();
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export EMPTY_VAR=\"\""));
    }
}