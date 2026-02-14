package com.github.jmoalves.levain.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class Extractor {
    private static final Logger logger = LoggerFactory.getLogger(Extractor.class);
    private com.github.jmoalves.levain.util.ProgressBar progress;
    private long extractedBytes = 0;

    public void extract(boolean strip, Path src, Path dst) throws IOException {
        extract(strip, src, dst, null);
    }

    public void extract(boolean strip, Path src, Path dst, com.github.jmoalves.levain.util.ProgressBar progress)
            throws IOException {
        this.progress = progress;
        this.extractedBytes = 0;
        Path tempDir = null;
        try {
            tempDir = extractToTemp(src, dst);
            move(strip, tempDir, dst);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
            if (this.progress != null) {
                this.progress.finish();
            }
        }
    }

    protected abstract void extractImpl(Path src, Path dst) throws IOException;

    protected void reportBytes(long bytes) {
        if (progress == null) {
            return;
        }
        extractedBytes += bytes;
        progress.update(extractedBytes);
    }

    private Path extractToTemp(Path src, Path dst) throws IOException {
        Path safeTempDir = dst.toAbsolutePath().normalize().getParent();
        if (safeTempDir == null) {
            safeTempDir = dst.toAbsolutePath().normalize();
        }
        Files.createDirectories(safeTempDir);
        Path tempDir = Files.createTempDirectory(safeTempDir, "extract-");
        logger.debug("EXTRACT {} => {}", src, tempDir);
        extractImpl(src, tempDir);
        return tempDir;
    }

    private void move(boolean strip, Path srcDir, Path dstDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(srcDir)) {
            Path onlyChild = null;
            int count = 0;
            for (Path child : stream) {
                count++;
                if (strip) {
                    if (count > 1) {
                        throw new IOException("You should not ask for --strip if there are more than one directory");
                    }
                    onlyChild = child;
                } else {
                    Path target = dstDir.resolve(child.getFileName());
                    movePath(child, target);
                }
            }
            if (strip && onlyChild != null) {
                move(false, onlyChild, dstDir);
            }
        }
    }

    private void movePath(Path src, Path dst) throws IOException {
        if (Files.isDirectory(src)) {
            Files.createDirectories(dst);
            try (var walk = Files.walk(src)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    Path target = dst.resolve(src.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            deleteDirectory(src);
            return;
        }

        Files.createDirectories(dst.getParent());
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(src);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    logger.debug("Failed to delete {}: {}", p, e.getMessage());
                }
            });
        }
    }
}
