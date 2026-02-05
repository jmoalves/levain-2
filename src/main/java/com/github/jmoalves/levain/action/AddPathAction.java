package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Add one or more paths to PATH.
 *
 * Usage:
 *   - addPath /opt/maven/bin
 *   - addPath --permanent /opt/maven/bin
 *   - addPath --append /opt/legacy/bin
 */
@ApplicationScoped
public class AddPathAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(AddPathAction.class);

    @Override
    public String name() {
        return "addPath";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("addPath requires arguments: [--permanent] [--append|--prepend] <path> [...]");
        }

        boolean permanent = false;
        boolean prepend = true; // default: prepend for higher priority

        List<String> remaining = new ArrayList<>();
        for (String arg : args) {
            if ("--permanent".equals(arg)) {
                permanent = true;
            } else if ("--append".equals(arg)) {
                prepend = false;
            } else if ("--prepend".equals(arg)) {
                prepend = true;
            } else {
                remaining.add(arg);
            }
        }

        if (remaining.isEmpty()) {
            throw new IllegalArgumentException("addPath requires at least one path argument");
        }

        List<String> resolved = new ArrayList<>();
        for (String pathArg : remaining) {
            Path path = FileUtils.resolve(context.getBaseDir(), pathArg);
            resolved.add(path.toString());
        }

        String currentPath = context.getConfig().getVariable("PATH");
        if (currentPath == null || currentPath.isBlank()) {
            currentPath = System.getenv("PATH");
        }

        String updated = EnvironmentUtils.updatePath(currentPath, resolved, prepend);
        context.getConfig().setVariable("PATH", updated);

        logger.debug("Updated PATH (permanent={} prepend={})", permanent, prepend);

        if (permanent) {
            if (EnvironmentUtils.isWindows()) {
                EnvironmentUtils.persistWindowsEnv("PATH", updated);
            } else {
                EnvironmentUtils.persistUnixEnv(EnvironmentUtils.resolveProfilePath(), "PATH", updated);
            }
        }
    }
}