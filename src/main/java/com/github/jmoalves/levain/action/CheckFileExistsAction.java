package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Check if one or more files exist.
 *
 * Usage:
 *   - checkFileExists <path1> [path2] ...
 */
@ApplicationScoped
public class CheckFileExistsAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CheckFileExistsAction.class);

    @Override
    public String name() {
        return "checkFileExists";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("checkFileExists requires an action context");
        }
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("checkFileExists requires at least one path");
        }

        for (String arg : args) {
            Path path = FileUtils.resolve(context.getBaseDir(), arg);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IllegalArgumentException("File does not exist: " + path);
            }
            logger.debug("checkFileExists OK: {}", path);
        }
    }
}
