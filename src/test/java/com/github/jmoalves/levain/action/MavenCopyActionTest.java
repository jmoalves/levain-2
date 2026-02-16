package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenCopyActionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBuildCommandWithRepoFromRecipeAttributes() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setCustomAttributes(Map.of("nexusCentralRepo", "http://repo.example/central"));
        ActionContext context = new ActionContext(new Config(), recipe, tempDir, tempDir);

        RecordingMavenCopyAction action = new RecordingMavenCopyAction();
        action.execute(context, List.of("org.example:demo:1.0.0:jar", "lib"));

        List<String> command = action.getLastCommand();
        assertTrue(command.contains("-DremoteRepositories=http://repo.example/central"));
        assertTrue(command.contains("-Dartifact=org.example:demo:1.0.0:jar"));
        assertTrue(Files.isDirectory(tempDir.resolve("lib")));
    }

    @Test
    void shouldOverrideRepoWithFlag() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setCustomAttributes(Map.of("nexusCentralRepo", "http://repo.example/central"));
        ActionContext context = new ActionContext(new Config(), recipe, tempDir, tempDir);

        RecordingMavenCopyAction action = new RecordingMavenCopyAction();
        action.execute(context, List.of("--repo=http://repo.example/releases", "org.example:demo:1.0.0:jar", "out"));

        List<String> command = action.getLastCommand();
        assertTrue(command.contains("-DremoteRepositories=http://repo.example/releases"));
    }

    @Test
    void shouldUseRepoFromConfigWhenMissingInRecipe() throws Exception {
        Config config = new Config();
        config.setVariable("nexusCentralRepo", "http://repo.example/config");

        ActionContext context = new ActionContext(config, new Recipe(), tempDir, tempDir);
        RecordingMavenCopyAction action = new RecordingMavenCopyAction();

        action.execute(context, List.of("org.example:demo:1.0.0:jar", "lib"));

        assertTrue(action.getLastCommand().contains("-DremoteRepositories=http://repo.example/config"));
    }

    @Test
    void shouldThrowWhenCommandFails() {
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);
        RecordingMavenCopyAction action = new RecordingMavenCopyAction();
        action.exitCode = 1;

        assertThrows(Exception.class,
                () -> action.execute(context, List.of("org.example:demo:1.0.0:jar", "lib")));
    }

    @Test
    void shouldUseRepositoryUrlFromRecipeAttributes() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setCustomAttributes(Map.of("repository.url", "http://repo.example/alt"));
        ActionContext context = new ActionContext(new Config(), recipe, tempDir, tempDir);

        RecordingMavenCopyAction action = new RecordingMavenCopyAction();
        action.execute(context, List.of("org.example:demo:1.0.0:jar", "lib"));

        assertTrue(action.getLastCommand().contains("-DremoteRepositories=http://repo.example/alt"));
    }

    @Test
    void shouldRejectMissingRepoValue() {
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);
        RecordingMavenCopyAction action = new RecordingMavenCopyAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("--repo", "org.example:demo:1.0.0:jar", "lib")));
    }

    @Test
    void shouldRejectMissingArgs() {
        ActionContext context = new ActionContext(new Config(), new Recipe(), tempDir, tempDir);
        RecordingMavenCopyAction action = new RecordingMavenCopyAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(context, List.of("org.example:demo:1.0.0:jar")));
    }

    @Test
    void shouldRejectMissingContext() {
        RecordingMavenCopyAction action = new RecordingMavenCopyAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("org.example:demo:1.0.0:jar", "lib")));
    }

    private static class RecordingMavenCopyAction extends MavenCopyAction {
        private List<String> lastCommand = new ArrayList<>();
        private int exitCode = 0;

        @Override
        protected int runCommand(List<String> command, Path workingDir) {
            lastCommand = new ArrayList<>(command);
            return exitCode;
        }

        List<String> getLastCommand() {
            return lastCommand;
        }
    }
}
