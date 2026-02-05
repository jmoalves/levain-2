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

@DisplayName("AddPathAction Tests")
class AddPathActionTest {
    @TempDir
    Path tempDir;

    private AddPathAction action;
    private ActionContext context;
    private String previousProfileOverride;

    @BeforeEach
    void setUp() {
        action = new AddPathAction();

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
    @DisplayName("Test 1: addPath prepends by default")
    void testPrependDefault() throws Exception {
        String sep = EnvironmentUtils.pathSeparator();
        String a = tempDir.resolve("a").toString();
        String b = tempDir.resolve("b").toString();
        String c = tempDir.resolve("c").toString();

        context.getConfig().setVariable("PATH", a + sep + b);

        action.execute(context, List.of(c));

        assertEquals(c + sep + a + sep + b, context.getConfig().getVariable("PATH"));
    }

    @Test
    @DisplayName("Test 2: addPath --append appends to PATH")
    void testAppend() throws Exception {
        String sep = EnvironmentUtils.pathSeparator();
        String a = tempDir.resolve("a").toString();
        String b = tempDir.resolve("b").toString();
        String c = tempDir.resolve("c").toString();

        context.getConfig().setVariable("PATH", a + sep + b);

        action.execute(context, List.of("--append", c));

        assertEquals(a + sep + b + sep + c, context.getConfig().getVariable("PATH"));
    }

    @Test
    @DisplayName("Test 3: addPath removes duplicates")
    void testDeduplicate() throws Exception {
        String sep = EnvironmentUtils.pathSeparator();
        String a = tempDir.resolve("a").toString();
        String b = tempDir.resolve("b").toString();

        context.getConfig().setVariable("PATH", a + sep + b);

        action.execute(context, List.of(b));

        assertEquals(b + sep + a, context.getConfig().getVariable("PATH"));
    }

    @Test
    @DisplayName("Test 4: addPath supports multiple paths")
    void testMultiplePaths() throws Exception {
        String sep = EnvironmentUtils.pathSeparator();
        String base = tempDir.resolve("base").toString();
        String p1 = tempDir.resolve("p1").toString();
        String p2 = tempDir.resolve("p2").toString();

        context.getConfig().setVariable("PATH", base);

        action.execute(context, List.of(p1, p2));

        assertEquals(p1 + sep + p2 + sep + base, context.getConfig().getVariable("PATH"));
    }

    @Test
    @DisplayName("Test 5: addPath resolves relative paths against baseDir")
    void testRelativePath() throws Exception {
        String sep = EnvironmentUtils.pathSeparator();
        String base = tempDir.resolve("base").toString();
        context.getConfig().setVariable("PATH", base);

        action.execute(context, List.of("bin"));

        String expected = tempDir.resolve("bin").toString() + sep + base;
        assertEquals(expected, context.getConfig().getVariable("PATH"));
    }

    @Test
    @DisplayName("Test 6: addPath --permanent writes to profile file")
    void testPermanentWritesProfile() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        action.execute(context, List.of("--permanent", "bin"));

        Path profile = EnvironmentUtils.resolveProfilePath();
        assertNotNull(profile);
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export PATH="));
        assertTrue(content.contains(tempDir.resolve("bin").toString()));
    }

    @Test
    @DisplayName("Test 7: addPath fails with no args")
    void testNoArgs() {
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    @Test
    @DisplayName("Test 8: addPath fails when only flags are provided")
    void testOnlyFlags() {
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--permanent")));
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--append")));
        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of("--prepend")));
    }

    @Test
    @DisplayName("Test 9: addPath works even if PATH not set in config")
    void testNoConfigPath() throws Exception {
        String newPath = tempDir.resolve("new").toString();
        action.execute(context, List.of(newPath));

        String updated = context.getConfig().getVariable("PATH");
        assertNotNull(updated);
        assertTrue(updated.contains(newPath));
    }

    @Test
    @DisplayName("Test 10: addPath --permanent quotes values with spaces")
    void testPermanentQuotesSpaces() throws Exception {
        if (EnvironmentUtils.isWindows()) {
            return;
        }

        Path pathWithSpaces = tempDir.resolve("my tools/bin");
        action.execute(context, List.of("--permanent", pathWithSpaces.toString()));

        Path profile = EnvironmentUtils.resolveProfilePath();
        String content = Files.readString(profile, StandardCharsets.UTF_8);
        assertTrue(content.contains("export PATH=\""));
    }
}