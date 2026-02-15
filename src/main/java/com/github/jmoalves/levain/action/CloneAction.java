package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileUtils;
import com.github.jmoalves.levain.util.GitSupport;
import com.github.jmoalves.levain.util.GitUrlUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Clone a git repository into a directory.
 *
 * Usage:
 *   - clone <repo> <dest>
 *   - clone --branch <name> <repo> <dest>
 *   - clone --depth <n> <repo> <dest>
 *   - clone --recursive <repo> <dest>
 */
@ApplicationScoped
public class CloneAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CloneAction.class);

    @Override
    public String name() {
        return "clone";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        ParsedArgs parsed = parseArgs(args);
        if (parsed.positionals.size() != 2) {
            throw new IllegalArgumentException("clone requires a repository URL and destination directory");
        }

        String repoArg = parsed.positionals.get(0);
        String destArg = parsed.positionals.get(1);

        GitUrlUtils.ParsedGitUrl parsedUrl = GitUrlUtils.parse(repoArg);
        String effectiveUrl = parsedUrl != null ? parsedUrl.getUrl() : repoArg;
        String branch = parsed.branch != null ? parsed.branch
                : (parsedUrl != null ? parsedUrl.getBranch() : null);

        Path destination = FileUtils.resolve(context.getBaseDir(), destArg);
        Path gitDir = destination.resolve(".git");

        if (Files.exists(destination)) {
            if (!Files.isDirectory(destination)) {
                throw new IllegalArgumentException("Destination exists and is not a directory: " + destination);
            }
            if (Files.exists(gitDir)) {
                logger.debug("Repository already exists at {}, pulling latest changes", destination);
                GitSupport.pullRepository(destination);
                return;
            }
            throw new IllegalArgumentException("Destination exists but is not a git repository: " + destination);
        }

        Files.createDirectories(destination.getParent() != null ? destination.getParent() : destination);

        logger.info("Cloning repository {} into {}", effectiveUrl, destination);
        GitSupport.cloneRepository(effectiveUrl, destination, branch, parsed.depth, parsed.recursive);
    }

    private ParsedArgs parseArgs(List<String> args) {
        ParsedArgs parsed = new ParsedArgs();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--recursive".equals(arg)) {
                parsed.recursive = true;
                continue;
            }
            if ("--branch".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--branch requires a value");
                }
                parsed.branch = args.get(++i);
                continue;
            }
            if ("--depth".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--depth requires a value");
                }
                parsed.depth = Integer.parseInt(args.get(++i));
                continue;
            }
            parsed.positionals.add(arg);
        }
        return parsed;
    }

    private static class ParsedArgs {
        private final List<String> positionals = new ArrayList<>();
        private String branch;
        private Integer depth;
        private boolean recursive;
    }
}
