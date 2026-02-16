package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Add a shortcut to the Desktop.
 *
 * Usage:
 *   - addToDesktop <target> [shortcutName]
 */
@ApplicationScoped
public class AddToDesktopAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(AddToDesktopAction.class);

    @Override
    public String name() {
        return "addToDesktop";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("addToDesktop requires a target file");
        }

        if (!EnvironmentUtils.isWindows()) {
            logger.debug("addToDesktop is only supported on Windows");
            return;
        }

        String targetArg = args.get(0);
        String shortcutName = args.size() > 1 ? args.get(1) : null;

        Path target = FileUtils.resolve(context.getBaseDir(), targetArg);
        if (!Files.exists(target)) {
            throw new IllegalArgumentException("Target not found: " + target);
        }

        Path desktopDir = resolveDesktopDir();
        Files.createDirectories(desktopDir);

        String fileName = target.getFileName().toString();
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".url") || lower.endsWith(".lnk")) {
            String dstName = shortcutName != null ? ensureExtensionFromSource(shortcutName, fileName) : fileName;
            Path dst = desktopDir.resolve(dstName);
            Files.copy(target, dst, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Copied shortcut {} -> {}", target, dst);
            return;
        }

        String finalName = shortcutName != null && !shortcutName.isBlank()
                ? ensureExtension(shortcutName, ".lnk")
                : stripExtension(fileName) + ".lnk";
        Path shortcut = desktopDir.resolve(finalName);
        createShortcut(target, shortcut);
        logger.debug("Created shortcut {} -> {}", target, shortcut);
    }

    protected Path resolveDesktopDir() {
        String userProfile = System.getenv("USERPROFILE");
        String userHome = System.getProperty("user.home");
        String base = userProfile != null && !userProfile.isBlank() ? userProfile : userHome;
        if (base == null || base.isBlank()) {
            base = System.getProperty("user.dir");
        }
        return Paths.get(base, "Desktop");
    }

    protected void createShortcut(Path target, Path shortcut) throws IOException, InterruptedException {
        String targetPath = escapePowerShell(target.toString());
        String shortcutPath = escapePowerShell(shortcut.toString());
        String workingDir = target.getParent() != null ? escapePowerShell(target.getParent().toString()) : "";

        StringBuilder script = new StringBuilder();
        script.append("$WshShell = New-Object -ComObject WScript.Shell; ");
        script.append("$Shortcut = $WshShell.CreateShortcut('").append(shortcutPath).append("'); ");
        script.append("$Shortcut.TargetPath = '").append(targetPath).append("'; ");
        if (!workingDir.isBlank()) {
            script.append("$Shortcut.WorkingDirectory = '").append(workingDir).append("'; ");
        }
        script.append("$Shortcut.Save();");

        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                script.toString());
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IOException("Failed to create shortcut (exit " + exit + ")");
        }
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    private String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private String ensureExtension(String name, String defaultExtension) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return defaultExtension;
        }
        String lower = trimmed.toLowerCase();
        if (lower.endsWith(".lnk") || lower.endsWith(".url")) {
            return trimmed;
        }
        if (defaultExtension.startsWith(".")) {
            return trimmed + defaultExtension;
        }
        return trimmed + defaultExtension;
    }

    private String ensureExtensionFromSource(String name, String sourceFileName) {
        if (name == null || name.isBlank()) {
            return sourceFileName;
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".lnk") || lower.endsWith(".url")) {
            return name;
        }
        int dot = sourceFileName.lastIndexOf('.');
        if (dot <= 0) {
            return name;
        }
        return name + sourceFileName.substring(dot);
    }
}
