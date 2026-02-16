package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Add a context menu entry on Windows.
 *
 * Usage:
 *   - contextMenu <label> <command>
 *   - contextMenu --target=files|directories|both|background --icon=<path> <label> <command>
 */
@ApplicationScoped
public class ContextMenuAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(ContextMenuAction.class);

    @Override
    public String name() {
        return "contextMenu";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("contextMenu requires a label and command");
        }

        if (!EnvironmentUtils.isWindows()) {
            logger.debug("contextMenu is only supported on Windows");
            return;
        }

        String target = "both";
        String icon = null;
        List<String> positionals = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("--target=")) {
                target = arg.substring("--target=".length()).trim();
                continue;
            }
            if ("--target".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--target requires a value");
                }
                target = args.get(++i).trim();
                continue;
            }
            if (arg.startsWith("--icon=")) {
                icon = arg.substring("--icon=".length()).trim();
                continue;
            }
            if ("--icon".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--icon requires a value");
                }
                icon = args.get(++i).trim();
                continue;
            }
            positionals.add(arg);
        }

        if (positionals.size() < 2) {
            throw new IllegalArgumentException("contextMenu requires a label and command");
        }

        String label = positionals.get(0);
        String command = positionals.size() == 2
                ? positionals.get(1)
                : String.join(" ", positionals.subList(1, positionals.size()));

        for (String baseKey : resolveTargets(target)) {
            String entryKey = baseKey + "\\" + label;
            runRegAdd(List.of("reg", "add", entryKey, "/f"));
            if (icon != null && !icon.isBlank()) {
                runRegAdd(List.of("reg", "add", entryKey, "/v", "Icon", "/d", icon, "/f"));
            }
            String commandKey = entryKey + "\\command";
            runRegAdd(List.of("reg", "add", commandKey, "/ve", "/d", command, "/f"));
        }

        logger.debug("contextMenu added: {} -> {}", label, command);
    }

    private List<String> resolveTargets(String target) {
        String normalized = target == null ? "both" : target.trim().toLowerCase();
        List<String> keys = new ArrayList<>();
        switch (normalized) {
            case "files":
            case "file":
                keys.add("HKCU\\Software\\Classes\\*\\shell");
                break;
            case "directories":
            case "directory":
                keys.add("HKCU\\Software\\Classes\\Directory\\shell");
                break;
            case "background":
                keys.add("HKCU\\Software\\Classes\\Directory\\Background\\shell");
                break;
            case "both":
            case "all":
                keys.add("HKCU\\Software\\Classes\\*\\shell");
                keys.add("HKCU\\Software\\Classes\\Directory\\shell");
                break;
            default:
                throw new IllegalArgumentException("Unsupported --target value: " + target);
        }
        return keys;
    }

    private void runRegAdd(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Failed to update registry: " + String.join(" ", command));
        }
    }
}
