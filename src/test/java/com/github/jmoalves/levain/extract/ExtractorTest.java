package com.github.jmoalves.levain.extract;

import com.github.jmoalves.levain.util.ProgressBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractWithoutStrip() throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("a.txt"), "hello", StandardCharsets.UTF_8);
        Path nested = Files.createDirectories(src.resolve("nested"));
        Files.writeString(nested.resolve("b.txt"), "world", StandardCharsets.UTF_8);

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        TestExtractor extractor = new TestExtractor();
        extractor.extract(false, src, dst, new ProgressBar("extract", 0));

        assertTrue(Files.exists(dst.resolve("a.txt")));
        assertTrue(Files.exists(dst.resolve("nested").resolve("b.txt")));
    }

    @Test
    void shouldExtractWithStripSingleDirectory() throws IOException {
        Path src = tempDir.resolve("src");
        Path root = Files.createDirectories(src.resolve("package"));
        Files.createDirectories(root.resolve("bin"));
        Files.writeString(root.resolve("bin/tool"), "tool", StandardCharsets.UTF_8);

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        TestExtractor extractor = new TestExtractor();
        extractor.extract(true, src, dst, new ProgressBar("extract", 0));

        assertTrue(Files.exists(dst.resolve("bin").resolve("tool")));
        assertTrue(Files.notExists(dst.resolve("package")));
    }

    @Test
    void shouldFailStripWhenMultipleRoots() throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src.resolve("one"));
        Files.createDirectories(src.resolve("two"));

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        TestExtractor extractor = new TestExtractor();

        IOException error = assertThrows(IOException.class,
                () -> extractor.extract(true, src, dst, new ProgressBar("extract", 0)));
        assertEquals("You should not ask for --strip if there are more than one directory", error.getMessage());
    }

    @Test
    void shouldHandleStripWithEmptyDirectory() throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        TestExtractor extractor = new TestExtractor();
        extractor.extract(true, src, dst, new ProgressBar("extract", 0));

        try (var stream = Files.list(dst)) {
            assertTrue(stream.findAny().isEmpty());
        }
    }

    @Test
    void shouldReportBytesWithoutProgress() throws IOException {
        Path src = tempDir.resolve("src.txt");
        Files.writeString(src, "data", StandardCharsets.UTF_8);
        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ReportingExtractor extractor = new ReportingExtractor();
        extractor.extract(false, src, dst);

        assertTrue(Files.exists(dst.resolve("payload.txt")));
    }

    @Test
    void shouldReportBytesWithProgress() throws IOException {
        Path src = tempDir.resolve("src.txt");
        Files.writeString(src, "data", StandardCharsets.UTF_8);
        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ReportingExtractor extractor = new ReportingExtractor();
        extractor.extract(false, src, dst, new ProgressBar("extract", 0));

        assertTrue(Files.exists(dst.resolve("payload.txt")));
    }

    @Test
    void shouldSkipDeleteWhenDirectoryMissing() throws Exception {
        TestExtractor extractor = new TestExtractor();
        var delete = Extractor.class.getDeclaredMethod("deleteDirectory", Path.class);
        delete.setAccessible(true);

        Path missing = tempDir.resolve("missing");
        delete.invoke(extractor, missing);

        assertTrue(Files.notExists(missing));
    }

    @Test
    void shouldIgnoreDeleteFailures() throws Exception {
        Path protectedDir = tempDir.resolve("protected");
        Files.createDirectories(protectedDir);
        Path child = protectedDir.resolve("file.txt");
        Files.writeString(child, "data", StandardCharsets.UTF_8);

        var delete = Extractor.class.getDeclaredMethod("deleteDirectory", Path.class);
        delete.setAccessible(true);

        boolean posix = Files.getFileStore(protectedDir).supportsFileAttributeView("posix");
        if (posix) {
            var perms = java.nio.file.attribute.PosixFilePermissions.fromString("r-xr-xr-x");
            Files.setPosixFilePermissions(protectedDir, perms);
        }

        try {
            delete.invoke(new TestExtractor(), protectedDir);
        } finally {
            if (posix) {
                var perms = java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(protectedDir, perms);
            }
            delete.invoke(new TestExtractor(), protectedDir);
        }
    }

    private static class TestExtractor extends Extractor {
        @Override
        protected void extractImpl(Path src, Path dst) throws IOException {
            try (var walk = Files.walk(src)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    Path target = dst.resolve(src.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target);
                    }
                }
            }
        }
    }

    private static class ReportingExtractor extends Extractor {
        @Override
        protected void extractImpl(Path src, Path dst) throws IOException {
            Files.createDirectories(dst);
            Path target = dst.resolve("payload.txt");
            Files.writeString(target, "payload", StandardCharsets.UTF_8);
            reportBytes(7);
        }
    }
}
