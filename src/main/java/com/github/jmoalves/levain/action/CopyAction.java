package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.FileCache;
import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Copy action implementation.
 *
 * Copies files from source to destination.
 * Handles both local and remote sources.
 *
 * Usage: copy [--verbose] <src> <dst>
 */
@ApplicationScoped
public class CopyAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CopyAction.class);

    private final FileCache fileCache;

    @Inject
    public CopyAction(FileCache fileCache) {
        this.fileCache = fileCache;
    }

    @Override
    public String name() {
        return "copy";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        boolean verbose = false;
        int positionalStart = 0;

        // Parse optional flags
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--verbose".equals(arg)) {
                verbose = true;
                positionalStart = i + 1;
            } else if (!arg.startsWith("--")) {
                break;
            }
        }

        List<String> positionals = args.subList(positionalStart, args.size());

        if (positionals.size() != 2) {
            throw new IllegalArgumentException("You must inform the file to copy and the destination directory");
        }

        String srcArg = positionals.get(0);
        String dstArg = positionals.get(1);

        boolean isLocalSource = FileUtils.isFileSystemUrl(srcArg);
        Path srcResolved = isLocalSource ? FileUtils.resolve(context.getRecipeDir(), srcArg) : null;
        Path dstResolved = FileUtils.resolve(context.getBaseDir(), dstArg);

        if (isLocalSource) {
            FileUtils.throwIfNotExists(srcResolved);
        }

        if (!Files.exists(dstResolved.getParent())) {
            Files.createDirectories(dstResolved.getParent());
        }

        if (verbose) {
            logger.info("COPY {} => {}", isLocalSource ? srcResolved : srcArg, dstResolved);
        } else {
            logger.debug("COPY {} => {}", isLocalSource ? srcResolved : srcArg, dstResolved);
        }

        // For local sources, use direct file copy
        if (isLocalSource) {
            Files.copy(srcResolved, dstResolved, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // For remote sources, use FileCache to download, then copy
            Path cachedSrc = fileCache.get(srcArg);
            Files.copy(cachedSrc, dstResolved, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
