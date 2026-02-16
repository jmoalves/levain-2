package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Create a backup copy of a file.
 *
 * Usage:
 *   - backupFile <path>
 *   - backupFile --suffix=.orig <path>
 */
@ApplicationScoped
public class BackupFileAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(BackupFileAction.class);

    @Override
    public String name() {
        return "backupFile";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("backupFile requires an action context");
        }
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("backupFile requires a file path");
        }

        String suffix = ".bak";
        boolean overwrite = true;
        String pathArg = null;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("--suffix=")) {
                suffix = arg.substring("--suffix=".length());
                continue;
            }
            if ("--suffix".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--suffix requires a value");
                }
                suffix = args.get(++i);
                continue;
            }
            if ("--no-overwrite".equals(arg)) {
                overwrite = false;
                continue;
            }
            if ("--overwrite".equals(arg)) {
                overwrite = true;
                continue;
            }
            if (pathArg == null) {
                pathArg = arg;
                continue;
            }
            throw new IllegalArgumentException("backupFile supports a single file path");
        }

        if (pathArg == null || pathArg.isBlank()) {
            throw new IllegalArgumentException("backupFile requires a file path");
        }

        Path source = FileUtils.resolve(context.getBaseDir(), pathArg);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("File does not exist: " + source);
        }

        Path backup = source.resolveSibling(source.getFileName().toString() + suffix);
        if (!overwrite && Files.exists(backup)) {
            throw new IllegalArgumentException("Backup already exists: " + backup);
        }

        CopyOption[] options = overwrite
                ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }
                : new CopyOption[0];
        Files.copy(source, backup, options);
        logger.debug("backupFile {} -> {}", source, backup);
    }
}
