package com.github.jmoalves.levain.extract;

import com.github.jmoalves.levain.util.ProgressBar;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TarGzExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractTarGzWithDirectoryAndFile() throws IOException {
        Path archive = tempDir.resolve("archive.tar.gz");
        try (var fos = Files.newOutputStream(archive);
             var gzos = new GzipCompressorOutputStream(fos);
             var tos = new TarArchiveOutputStream(gzos)) {
            TarArchiveEntry dirEntry = new TarArchiveEntry("dir/");
            tos.putArchiveEntry(dirEntry);
            tos.closeArchiveEntry();

            byte[] data = "data".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry fileEntry = new TarArchiveEntry("dir/file.txt");
            fileEntry.setSize(data.length);
            tos.putArchiveEntry(fileEntry);
            tos.write(data);
            tos.closeArchiveEntry();
        }

        Path dst = tempDir.resolve("out");
        Files.createDirectories(dst);

        TarGzExtractor extractor = new TarGzExtractor();
        extractor.extract(false, archive, dst, new ProgressBar("tar", 0));

        assertTrue(Files.exists(dst.resolve("dir")));
        assertTrue(Files.exists(dst.resolve("dir").resolve("file.txt")));
    }
}
