package com.github.jmoalves.levain.util;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private FileUtils() {
    }

    public static Path resolve(Path parent, String path) {
        if (!isFileSystemUrl(path)) {
            return Paths.get(path);
        }

        Path p = Paths.get(path);
        if (!p.isAbsolute() && parent != null) {
            return parent.resolve(path).normalize();
        }
        return p.toAbsolutePath().normalize();
    }

    public static boolean isFileSystemUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return false;
        }
        return true;
    }

    public static void throwIfNotExists(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }
    }

    public static String getFileNameFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "download";
            }
            Path p = Paths.get(path);
            String name = p.getFileName().toString();
            if (name.isBlank()) {
                return "download";
            }
            return name;
        } catch (Exception e) {
            return "download";
        }
    }
}
