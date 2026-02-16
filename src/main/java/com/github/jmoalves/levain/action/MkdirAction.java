package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Create one or more directories recursively.
 *
 * Usage:
 *   - mkdir ${baseDir}
 *   - mkdir ${baseDir}/subdir/nested
 *   - mkdir ${home}/.m2 ${home}/.ssh (multiple directories)
 *
 * Based on original levain implementation:
 * https://github.com/jmoalves/levain/blob/main/src/action/os/mkdir.ts
 *
 * Features:
 *   - Recursive directory creation (like mkdir -p)
 *   - Multiple directories in single command
 *   - Idempotent (succeeds if directory already exists)
 *   - Error if path exists but is not a directory
 */
@ApplicationScoped
public class MkdirAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(MkdirAction.class);

    @Override
    public String name() {
        return "mkdir";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("mkdir requires at least one directory path");
        }

        List<String> paths = args.stream()
            .filter(arg -> arg != null)
            .filter(arg -> !"--compact".equals(arg))
            .toList();

        if (paths.isEmpty()) {
            throw new IllegalArgumentException("mkdir requires at least one directory path");
        }

        for (String dirPath : paths) {
            Path dir = FileUtils.resolve(context.getBaseDir(), dirPath);
            
            // Check if path already exists
            if (dirExists(dir)) {
                logger.debug("Directory already exists, skipping: {}", dir);
                continue;
            }
            
            logger.debug("Creating directory: {}", dir);
            Files.createDirectories(dir);
        }
    }

    /**
     * Check if a directory exists.
     * Throws error if path exists but is not a directory.
     *
     * @param dir the path to check
     * @return true if directory exists, false otherwise
     * @throws IllegalArgumentException if path exists but is not a directory
     */
    private boolean dirExists(Path dir) throws Exception {
        if (Files.exists(dir)) {
            if (!Files.isDirectory(dir)) {
                throw new IllegalArgumentException("Path exists but is not a directory: " + dir);
            }
            return true;
        }
        return false;
    }
}
