package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template action implementation.
 *
 * Usage:
 *   - template --replace=/@@NAME@@/g --with=value src dst
 *   - template --doubleBackslash --replace=foo --with=bar src dst
 */
@ApplicationScoped
public class TemplateAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(TemplateAction.class);

    @Override
    public String name() {
        return "template";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        ParsedArgs parsed = parseArgs(args);
        if (parsed.positionals.size() != 2) {
            throw new IllegalArgumentException("template requires a source and destination file");
        }
        if (!parsed.pendingReplace.isEmpty()) {
            throw new IllegalArgumentException("template requires --with for every --replace");
        }
        if (parsed.replacements.isEmpty()) {
            throw new IllegalArgumentException("template requires at least one --replace/--with pair");
        }

        String srcArg = parsed.positionals.get(0);
        String dstArg = parsed.positionals.get(1);

        Path srcResolved = FileUtils.resolve(context.getRecipeDir(), srcArg);
        Path dstResolved = FileUtils.resolve(context.getBaseDir(), dstArg);
        FileUtils.throwIfNotExists(srcResolved);

        Path parent = dstResolved.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String content = Files.readString(srcResolved, StandardCharsets.UTF_8);
        for (Replacement replacement : parsed.replacements) {
            Pattern pattern = Pattern.compile(replacement.pattern);
            Matcher matcher = pattern.matcher(content);
            String replacementValue = Matcher.quoteReplacement(replacement.value);
            content = matcher.replaceAll(replacementValue);
        }

        Files.writeString(dstResolved, content, StandardCharsets.UTF_8);
        logger.debug("TEMPLATE {} => {}", srcResolved, dstResolved);
    }

    private ParsedArgs parseArgs(List<String> args) {
        ParsedArgs parsed = new ParsedArgs();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--doubleBackslash".equals(arg)) {
                parsed.doubleBackslash = true;
                continue;
            }
            if (arg.startsWith("--replace=")) {
                parsed.pendingReplace.add(normalizePattern(arg.substring("--replace=".length())));
                continue;
            }
            if ("--replace".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--replace requires a value");
                }
                parsed.pendingReplace.add(normalizePattern(args.get(++i)));
                continue;
            }
            if (arg.startsWith("--with=")) {
                String value = arg.substring("--with=".length());
                parsed.addReplacement(value);
                continue;
            }
            if ("--with".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--with requires a value");
                }
                parsed.addReplacement(args.get(++i));
                continue;
            }
            parsed.positionals.add(arg);
        }
        return parsed;
    }

    private String normalizePattern(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        if (raw.startsWith("/") && raw.length() > 1) {
            int lastSlash = raw.lastIndexOf('/');
            if (lastSlash > 0) {
                return raw.substring(1, lastSlash);
            }
        }
        return raw;
    }

    private static class ParsedArgs {
        private final List<String> positionals = new ArrayList<>();
        private final List<String> pendingReplace = new ArrayList<>();
        private final List<Replacement> replacements = new ArrayList<>();
        private boolean doubleBackslash;

        void addReplacement(String value) {
            if (pendingReplace.isEmpty()) {
                throw new IllegalArgumentException("--with requires a preceding --replace");
            }
            String pattern = pendingReplace.remove(0);
            String replacementValue = value;
            if (doubleBackslash) {
                replacementValue = replacementValue.replace("\\", "\\\\");
            }
            replacements.add(new Replacement(pattern, replacementValue));
        }
    }

    private static class Replacement {
        private final String pattern;
        private final String value;

        private Replacement(String pattern, String value) {
            this.pattern = pattern;
            this.value = value;
        }
    }
}
