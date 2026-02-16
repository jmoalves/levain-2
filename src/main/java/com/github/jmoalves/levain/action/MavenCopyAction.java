package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Download an artifact from a Maven repository into a target directory.
 *
 * Usage:
 *   - mavenCopy <groupId:artifactId:version:packaging> <destDir>
 */
@ApplicationScoped
public class MavenCopyAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(MavenCopyAction.class);

    @Override
    public String name() {
        return "mavenCopy";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("mavenCopy requires an action context");
        }
        if (args == null || args.size() < 2) {
            throw new IllegalArgumentException("mavenCopy requires: <artifact> <destinationDir>");
        }

        String repo = null;
        String artifact = null;
        String destArg = null;

        List<String> remaining = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--repo".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--repo requires a value");
                }
                repo = args.get(++i);
                continue;
            }
            if (arg.startsWith("--repo=")) {
                repo = arg.substring("--repo=".length());
                continue;
            }
            remaining.add(arg);
        }

        if (remaining.size() < 2) {
            throw new IllegalArgumentException("mavenCopy requires: <artifact> <destinationDir>");
        }

        artifact = remaining.get(0);
        destArg = remaining.get(1);

        Path destDir = FileUtils.resolve(context.getBaseDir(), destArg);
        Files.createDirectories(destDir);

        if (repo == null || repo.isBlank()) {
            repo = resolveRepository(context);
        }

        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("dependency:copy");
        command.add("-Dartifact=" + artifact);
        command.add("-DoutputDirectory=" + destDir.toString());
        if (repo != null && !repo.isBlank()) {
            command.add("-DremoteRepositories=" + repo);
        }

        logger.debug("mavenCopy command: {}", command);
        int exit = runCommand(command, destDir);
        if (exit != 0) {
            throw new IOException("mavenCopy failed with exit code " + exit);
        }
    }

    protected int runCommand(List<String> command, Path workingDir) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }
        builder.inheritIO();
        return builder.start().waitFor();
    }

    private String resolveRepository(ActionContext context) {
        if (context.getRecipe() != null && context.getRecipe().getCustomAttributes() != null) {
            Map<String, Object> attrs = context.getRecipe().getCustomAttributes();
            Object value = attrs.get("nexusCentralRepo");
            if (value instanceof String && !((String) value).isBlank()) {
                return (String) value;
            }
            value = attrs.get("repository.url");
            if (value instanceof String && !((String) value).isBlank()) {
                return (String) value;
            }
        }

        String configValue = context.getConfig().getVariable("nexusCentralRepo");
        if (configValue != null && !configValue.isBlank()) {
            return configValue;
        }

        return null;
    }
}
