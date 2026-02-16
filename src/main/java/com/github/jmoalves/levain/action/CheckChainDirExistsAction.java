package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Check a list of directories and return the first that exists.
 *
 * Usage:
 *   - checkChainDirExists <path1> <path2> ...
 *   - checkChainDirExists --saveVar=varName <path1> <path2>
 *   - checkChainDirExists --default=/fallback/path <path1> <path2>
 */
@ApplicationScoped
public class CheckChainDirExistsAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CheckChainDirExistsAction.class);

    @Override
    public String name() {
        return "checkChainDirExists";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("checkChainDirExists requires an action context");
        }
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("checkChainDirExists requires at least one path");
        }

        String saveVar = null;
        String defaultValue = null;
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--saveVar".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--saveVar requires a variable name");
                }
                saveVar = args.get(++i);
                continue;
            }
            if (arg.startsWith("--saveVar=")) {
                saveVar = arg.substring("--saveVar=".length());
                if (saveVar.isBlank()) {
                    throw new IllegalArgumentException("--saveVar requires a variable name");
                }
                continue;
            }
            if ("--default".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--default requires a path");
                }
                defaultValue = args.get(++i);
                continue;
            }
            if (arg.startsWith("--default=")) {
                defaultValue = arg.substring("--default=".length());
                continue;
            }
            paths.add(arg);
        }

        if (paths.isEmpty() && (defaultValue == null || defaultValue.isBlank())) {
            throw new IllegalArgumentException("checkChainDirExists requires at least one path or a default value");
        }

        String selected = null;
        for (String pathArg : paths) {
            Path resolved = FileUtils.resolve(context.getBaseDir(), pathArg);
            if (Files.exists(resolved) && Files.isDirectory(resolved)) {
                selected = resolved.toString();
                break;
            }
        }

        if (selected == null) {
            if (defaultValue != null && !defaultValue.isBlank()) {
                selected = FileUtils.resolve(context.getBaseDir(), defaultValue).toString();
            } else {
                throw new IllegalArgumentException("No directory found in chain: " + String.join(", ", paths));
            }
        }

        if (saveVar != null && !saveVar.isBlank()) {
            context.setRecipeVariable(saveVar, selected);
            context.getConfig().setVariable(saveVar, selected);
        }

        logger.debug("checkChainDirExists selected: {}", selected);
    }
}
