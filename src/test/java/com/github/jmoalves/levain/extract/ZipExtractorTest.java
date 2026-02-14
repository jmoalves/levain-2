package com.github.jmoalves.levain.extract;

import com.github.jmoalves.levain.util.ProgressBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractZipWithDirectoryAndFile() throws IOException {
        Path zip = tempDir.resolve("archive.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            ZipEntry dirEntry = new ZipEntry("dir/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();

            ZipEntry fileEntry = new ZipEntry("dir/file.txt");
            zos.putNextEntry(fileEntry);
            zos.write("data".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        Path dst = tempDir.resolve("out");
        Files.createDirectories(dst);

        ZipExtractor extractor = new ZipExtractor();
        extractor.extract(false, zip, dst, new ProgressBar("zip", 0));

        assertTrue(Files.exists(dst.resolve("dir")));
        assertTrue(Files.exists(dst.resolve("dir").resolve("file.txt")));
    }
}
