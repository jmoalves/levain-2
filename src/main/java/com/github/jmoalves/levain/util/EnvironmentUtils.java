package com.github.jmoalves.levain.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilities for environment variable persistence across OSes.
 */
public class EnvironmentUtils {
    private static final Logger logger = LogManager.getLogger(EnvironmentUtils.class);

    private EnvironmentUtils() {
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    public static Path resolveProfilePath() {
        String override = System.getProperty("levain.env.profile");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }

        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            return null;
        }

        String shell = System.getenv("SHELL");
        String filename;
        if (shell != null && shell.contains("zsh")) {
            filename = ".zshrc";
        } else if (shell != null && shell.contains("bash")) {
            filename = ".bashrc";
        } else {
            filename = ".profile";
        }

        return Paths.get(userHome).resolve(filename);
    }

    public static void persistUnixEnv(Path profile, String key, String value) throws IOException {
        if (profile == null) {
            throw new IOException("Profile path could not be resolved");
        }

        String exportLine = buildExportLine(key, value);
        Pattern exportPattern = Pattern.compile(String.format("^export\\s+%s=.*$", Pattern.quote(key)));

        List<String> lines = new ArrayList<>();
        if (Files.exists(profile)) {
            lines.addAll(Files.readAllLines(profile, StandardCharsets.UTF_8));
        } else {
            if (profile.getParent() != null) {
                Files.createDirectories(profile.getParent());
            }
        }

        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (exportPattern.matcher(line).matches()) {
                lines.set(i, exportLine);
                replaced = true;
            }
        }

        if (!replaced) {
            lines.add(exportLine);
        }

        Files.write(profile, lines, StandardCharsets.UTF_8);
        logger.debug("Persisted env var {} to {}", key, profile);
    }

    public static void persistWindowsEnv(String key, String value) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "setx", key, value);
        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Failed to persist env var using setx (exit " + exit + ")");
        }
    }

    private static String buildExportLine(String key, String value) {
        return "export " + key + "=" + quote(value);
    }

    private static String quote(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }

        boolean needsQuotes = value.contains(" ") || value.contains("\t") || value.contains("\"");
        if (!needsQuotes) {
            return value;
        }

        String escaped = value.replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}