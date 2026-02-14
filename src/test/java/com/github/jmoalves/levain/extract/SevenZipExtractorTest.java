package com.github.jmoalves.levain.extract;

import com.github.jmoalves.levain.util.ProgressBar;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SevenZipExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractSevenZipWithDirectoryAndFile() throws IOException {
        Path archive = tempDir.resolve("archive.7z");
        try (SevenZOutputFile out = new SevenZOutputFile(archive.toFile())) {
            SevenZArchiveEntry dirEntry = new SevenZArchiveEntry();
            dirEntry.setName("dir/");
            dirEntry.setDirectory(true);
            out.putArchiveEntry(dirEntry);
            out.closeArchiveEntry();

            byte[] data = "data".getBytes(StandardCharsets.UTF_8);
            SevenZArchiveEntry fileEntry = new SevenZArchiveEntry();
            fileEntry.setName("dir/file.txt");
            fileEntry.setSize(data.length);
            out.putArchiveEntry(fileEntry);
            out.write(data);
            out.closeArchiveEntry();
        }

        Path dst = tempDir.resolve("out");
        Files.createDirectories(dst);

        SevenZipExtractor extractor = new SevenZipExtractor();
        extractor.extract(false, archive, dst, new ProgressBar("7z", 0));

        assertTrue(Files.exists(dst.resolve("dir")));
        assertTrue(Files.exists(dst.resolve("dir").resolve("file.txt")));
    }
}
