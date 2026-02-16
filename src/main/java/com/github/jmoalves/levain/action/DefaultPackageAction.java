package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Set the default package name used by Levain.
 *
 * Usage:
 *   - defaultPackage levain
 */
@ApplicationScoped
public class DefaultPackageAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPackageAction.class);

    @Override
    public String name() {
        return "defaultPackage";
    }

    @Override
    public void execute(ActionContext context, List<String> args) {
        if (args == null || args.size() != 1) {
            throw new IllegalArgumentException("defaultPackage requires exactly one argument: <packageName>");
        }

        String packageName = args.get(0);
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("defaultPackage requires a non-blank package name");
        }

        context.getConfig().setDefaultPackage(packageName);
        context.getConfig().save();
        logger.debug("Set default package to: {}", packageName);
    }
}
