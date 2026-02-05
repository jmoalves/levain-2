package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Set an environment variable for the current recipe.
 *
 * Usage:
 *   - setEnv JAVA_HOME /opt/jdk-21
 *   - setEnv --permanent JAVA_HOME /opt/jdk-21
 */
@ApplicationScoped
public class SetEnvAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(SetEnvAction.class);

    @Override
    public String name() {
        return "setEnv";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("setEnv requires arguments: [--permanent] <name> <value>");
        }

        boolean permanent = false;
        int index = 0;
        if ("--permanent".equals(args.get(0))) {
            permanent = true;
            index = 1;
        }

        if (args.size() - index < 2) {
            throw new IllegalArgumentException("setEnv requires a name and value. Usage: setEnv [--permanent] <name> <value>");
        }

        String key = args.get(index);
        String value = args.get(index + 1);

        // Save to config variables for later substitutions.
        context.getConfig().setVariable(key, value);

        logger.debug("Set env var: {} = {} (permanent={})", key, value, permanent);

        if (permanent) {
            if (EnvironmentUtils.isWindows()) {
                EnvironmentUtils.persistWindowsEnv(key, value);
            } else {
                Path profile = EnvironmentUtils.resolveProfilePath();
                EnvironmentUtils.persistUnixEnv(profile, key, value);
            }
        }
    }
}