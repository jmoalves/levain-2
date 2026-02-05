package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.extract.ExtractorFactory;
import com.github.jmoalves.levain.util.FileCache;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExtractActionTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldExtractZipWithStrip() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZip(src, "pkg/file.txt", "hello");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(new ActionContext(createConfig(), null, tempDir, tempDir),
                List.of("--strip", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("file.txt")));
        assertFalse(Files.exists(dst.resolve("pkg")));
    }

    @Test
    void shouldExtractTarGzWithTypeOverride() throws Exception {
        Path src = tempDir.resolve("archive.bin");
        createTarGz(src, "folder/hello.txt", "hello");

        Path dst = tempDir.resolve("dst-tar");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(new ActionContext(createConfig(), null, tempDir, tempDir),
                List.of("--type", "tar.gz", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("folder/hello.txt")));
    }

    @Test
    void shouldExtractSevenZip() throws Exception {
        Path src = tempDir.resolve("archive.7z");
        createSevenZip(src, "content.txt", "hello");

        Path dst = tempDir.resolve("dst-7z");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(new ActionContext(createConfig(), null, tempDir, tempDir),
                List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("content.txt")));
    }

    private ExtractAction createAction() {
        Config config = createConfig();
        return new ExtractAction(new FileCache(config), new ExtractorFactory());
    }

    private Config createConfig() {
        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());
        return config;
    }

    private void createZip(Path target, String entryName, String content) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void createTarGz(Path target, String entryName, String content) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(out);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(data.length);
            taos.putArchiveEntry(entry);
            taos.write(data);
            taos.closeArchiveEntry();
        }
    }

    private void createSevenZip(Path target, String entryName, String content) throws IOException {
        try (SevenZOutputFile sevenZ = new SevenZOutputFile(target.toFile())) {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName(entryName);
            entry.setSize(data.length);
            sevenZ.putArchiveEntry(entry);
            sevenZ.write(data);
            sevenZ.closeArchiveEntry();
        }
    }
}
