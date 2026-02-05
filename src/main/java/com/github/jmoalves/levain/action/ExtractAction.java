package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.extract.Extractor;
import com.github.jmoalves.levain.extract.ExtractorFactory;
import com.github.jmoalves.levain.util.FileCache;
import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract action implementation.
 *
 * Usage: extract [--strip] [--type <type>] <src> <dst>
 */
@ApplicationScoped
public class ExtractAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(ExtractAction.class);

    private final FileCache fileCache;
    private final ExtractorFactory extractorFactory;

    @Inject
    public ExtractAction(FileCache fileCache, ExtractorFactory extractorFactory) {
        this.fileCache = fileCache;
        this.extractorFactory = extractorFactory;
    }

    @Override
    public String name() {
        return "extract";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionals.size() != 2) {
            throw new IllegalArgumentException("You must inform the file to extract and the destination directory");
        }

        String srcArg = parsed.positionals.get(0);
        String dstArg = parsed.positionals.get(1);

        boolean isLocalSource = FileUtils.isFileSystemUrl(srcArg);
        Path srcResolved = isLocalSource ? FileUtils.resolve(context.getRecipeDir(), srcArg) : null;
        Path dstResolved = FileUtils.resolve(context.getBaseDir(), dstArg);

        if (isLocalSource) {
            FileUtils.throwIfNotExists(srcResolved);
        }

        if (!Files.exists(dstResolved)) {
            throw new IllegalArgumentException("Destination directory does not exist: " + dstResolved);
        }

        if (parsed.type != null && !extractorFactory.isTypeSupported(parsed.type)) {
            throw new IllegalArgumentException("Unknown type '" + parsed.type + "'");
        }

        logger.debug("EXTRACT {} => {}", isLocalSource ? srcResolved : srcArg, dstResolved);

        String cacheKey = isLocalSource ? srcResolved.toString() : srcArg;
        Path cachedSrc = fileCache.get(cacheKey);
        Extractor extractor = extractorFactory.createExtractor(cachedSrc, parsed.type);
        extractor.extract(parsed.strip, cachedSrc, dstResolved);
    }

    private ParsedArgs parseArgs(List<String> args) {
        ParsedArgs parsed = new ParsedArgs();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--strip".equals(arg)) {
                parsed.strip = true;
                continue;
            }
            if (arg.startsWith("--type=")) {
                parsed.type = arg.substring("--type=".length());
                continue;
            }
            if ("--type".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--type requires a value");
                }
                parsed.type = args.get(++i);
                continue;
            }
            parsed.positionals.add(arg);
        }
        return parsed;
    }

    private static class ParsedArgs {
        private final List<String> positionals = new ArrayList<>();
        private boolean strip;
        private String type;
    }
}
