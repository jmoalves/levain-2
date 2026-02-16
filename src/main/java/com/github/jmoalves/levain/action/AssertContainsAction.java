package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Assert that a file contains a specific substring.
 *
 * Usage:
 *   - assertContains <path> <text>
 */
@ApplicationScoped
public class AssertContainsAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(AssertContainsAction.class);

    @Override
    public String name() {
        return "assertContains";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("assertContains requires an action context");
        }
        if (args == null || args.size() < 2) {
            throw new IllegalArgumentException("assertContains requires a path and search text");
        }

        String pathArg = args.get(0);
        String needle = args.size() == 2 ? args.get(1) : String.join(" ", args.subList(1, args.size()));

        Path target = FileUtils.resolve(context.getBaseDir(), pathArg);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("File does not exist: " + target);
        }

        String content = Files.readString(target, StandardCharsets.UTF_8);
        if (!content.contains(needle)) {
            throw new IllegalArgumentException("assertContains failed: '" + needle + "' not found in " + target);
        }

        logger.debug("assertContains OK: {}", target);
    }
}
