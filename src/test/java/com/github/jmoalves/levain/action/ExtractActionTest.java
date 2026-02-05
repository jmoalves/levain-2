package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.extract.ExtractorFactory;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.FileCache;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
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
    
    private Config config;
    
    @BeforeEach
    void setUp() {
        config = createConfig();
    }

    // ========================================
    // Basic Extraction Tests
    // ========================================

    @Test
    void testExtractZipBasic() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZip(src, "file.txt", "hello world");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("file.txt")));
        assertEquals("hello world", Files.readString(dst.resolve("file.txt")));
    }

    @Test
    void testExtractZipWithDirectory() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZipWithMultipleFiles(src);

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("dir/file1.txt")));
        assertTrue(Files.exists(dst.resolve("dir/file2.txt")));
        assertEquals("content1", Files.readString(dst.resolve("dir/file1.txt")));
        assertEquals("content2", Files.readString(dst.resolve("dir/file2.txt")));
    }

    @Test
    void testExtractTarGz() throws Exception {
        Path src = tempDir.resolve("archive.tar.gz");
        createTarGz(src, "folder/hello.txt", "hello tar.gz");

        Path dst = tempDir.resolve("dst-tar");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("folder/hello.txt")));
        assertEquals("hello tar.gz", Files.readString(dst.resolve("folder/hello.txt")));
    }

    @Test
    void testExtractSevenZip() throws Exception {
        Path src = tempDir.resolve("archive.7z");
        createSevenZip(src, "content.txt", "hello 7z");

        Path dst = tempDir.resolve("dst-7z");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("content.txt")));
        assertEquals("hello 7z", Files.readString(dst.resolve("content.txt")));
    }

    // ========================================
    // Strip Directory Tests
    // ========================================

    @Test
    void testExtractZipWithStrip() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZip(src, "pkg/file.txt", "hello");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), 
                List.of("--strip", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("file.txt")));
        assertFalse(Files.exists(dst.resolve("pkg")));
    }

    @Test
    void testExtractZipStripMultipleLevels() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        try (OutputStream out = Files.newOutputStream(src);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            addZipEntry(zos, "root/sub1/file1.txt", "content1");
            addZipEntry(zos, "root/sub2/file2.txt", "content2");
        }

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--strip", src.toString(), dst.toString()));

        // After stripping first level, files should be in sub1/ and sub2/
        assertTrue(Files.exists(dst.resolve("sub1/file1.txt")));
        assertTrue(Files.exists(dst.resolve("sub2/file2.txt")));
        assertFalse(Files.exists(dst.resolve("root")));
    }

    @Test
    void testExtractTarGzWithStrip() throws Exception {
        Path src = tempDir.resolve("archive.tar.gz");
        try (OutputStream out = Files.newOutputStream(src);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(out);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            addTarEntry(taos, "package-1.0/README.md", "readme content");
            addTarEntry(taos, "package-1.0/bin/app.sh", "#!/bin/bash");
        }

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--strip", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("README.md")));
        assertTrue(Files.exists(dst.resolve("bin/app.sh")));
        assertFalse(Files.exists(dst.resolve("package-1.0")));
    }

    // ========================================
    // Type Override Tests
    // ========================================

    @Test
    void testExtractTarGzWithTypeOverride() throws Exception {
        Path src = tempDir.resolve("archive.bin");
        createTarGz(src, "folder/hello.txt", "hello");

        Path dst = tempDir.resolve("dst-tar");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--type", "tar.gz", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("folder/hello.txt")));
    }

    @Test
    void testExtractZipWithTypeOverrideEquals() throws Exception {
        Path src = tempDir.resolve("archive.dat");
        createZip(src, "file.txt", "content");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--type=zip", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("file.txt")));
    }

    @Test
    void testExtract7zWithTypeOverride() throws Exception {
        Path src = tempDir.resolve("archive.bin");
        createSevenZip(src, "data.txt", "7zip data");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--type", "7z", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("data.txt")));
    }

    // ========================================
    // Combined Options Tests
    // ========================================

    @Test
    void testExtractWithStripAndTypeOverride() throws Exception {
        Path src = tempDir.resolve("archive.unknown");
        createZip(src, "package/lib/library.jar", "jar content");

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir),
                List.of("--strip", "--type=zip", src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("lib/library.jar")));
        assertFalse(Files.exists(dst.resolve("package")));
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void testExtractThrowsWhenNoArguments() {
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir), List.of()));
        assertTrue(ex.getMessage().contains("You must inform the file to extract and the destination directory"));
    }

    @Test
    void testExtractThrowsWhenOnlyOneArgument() {
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir), List.of("archive.zip")));
        assertTrue(ex.getMessage().contains("You must inform the file to extract and the destination directory"));
    }

    @Test
    void testExtractThrowsWhenThreeArguments() {
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir), 
                        List.of("archive.zip", "dst", "extra")));
        assertTrue(ex.getMessage().contains("You must inform the file to extract and the destination directory"));
    }

    @Test
    void testExtractThrowsWhenSourceDoesNotExist() {
        Path dst = tempDir.resolve("dst");
        ExtractAction action = createAction();
        assertThrows(Exception.class,
                () -> action.execute(createContext(tempDir, tempDir), 
                        List.of("nonexistent.zip", dst.toString())));
    }

    @Test
    void testExtractThrowsWhenDestinationDoesNotExist() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZip(src, "file.txt", "content");
        
        Path dst = tempDir.resolve("nonexistent");
        
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir), 
                        List.of(src.toString(), dst.toString())));
        assertTrue(ex.getMessage().contains("Destination directory does not exist"));
    }

    @Test
    void testExtractThrowsWhenTypeRequiresValue() {
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir), 
                        List.of("--type")));
        assertTrue(ex.getMessage().contains("--type requires a value"));
    }

    @Test
    void testExtractThrowsWhenTypeIsUnknown() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        createZip(src, "file.txt", "content");
        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);
        
        ExtractAction action = createAction();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> action.execute(createContext(tempDir, tempDir),
                        List.of("--type", "unknown", src.toString(), dst.toString())));
        assertTrue(ex.getMessage().contains("Unknown type"));
    }

    // ========================================
    // Binary File Tests
    // ========================================

    @Test
    void testExtractZipWithBinaryContent() throws Exception {
        Path src = tempDir.resolve("archive.zip");
        byte[] binaryData = new byte[]{0x00, 0x01, 0x02, (byte)0xFF, (byte)0xFE};
        createZipBinary(src, "binary.dat", binaryData);

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst.resolve("binary.dat")));
        assertArrayEquals(binaryData, Files.readAllBytes(dst.resolve("binary.dat")));
    }

    // ========================================
    // Empty Archive Tests
    // ========================================

    @Test
    void testExtractEmptyZip() throws Exception {
        Path src = tempDir.resolve("empty.zip");
        try (OutputStream out = Files.newOutputStream(src);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            // Create empty zip
        }

        Path dst = tempDir.resolve("dst");
        Files.createDirectories(dst);

        ExtractAction action = createAction();
        action.execute(createContext(tempDir, tempDir), List.of(src.toString(), dst.toString()));

        // Should complete without error
        assertTrue(Files.exists(dst));
        assertEquals(0, Files.list(dst).count());
    }

    // ========================================
    // Action Interface Tests
    // ========================================

    @Test
    void testActionName() {
        ExtractAction action = createAction();
        assertEquals("extract", action.name());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private ExtractAction createAction() {
        return new ExtractAction(new FileCache(config), new ExtractorFactory());
    }

    private Config createConfig() {
        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());
        return config;
    }

    private ActionContext createContext(Path recipeDir, Path baseDir) {
        return new ActionContext(config, createTestRecipe(), baseDir, recipeDir);
    }

    private Recipe createTestRecipe() {
        Recipe recipe = new Recipe();
        recipe.setName("test-recipe");
        recipe.setVersion("1.0.0");
        return recipe;
    }

    private void createZip(Path target, String entryName, String content) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            addZipEntry(zos, entryName, content);
        }
    }

    private void createZipBinary(Path target, String entryName, byte[] content) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
    }

    private void createZipWithMultipleFiles(Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            addZipEntry(zos, "dir/file1.txt", "content1");
            addZipEntry(zos, "dir/file2.txt", "content2");
        }
    }

    private void addZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void createTarGz(Path target, String entryName, String content) throws IOException {
        try (OutputStream out = Files.newOutputStream(target);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(out);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            addTarEntry(taos, entryName, content);
        }
    }

    private void addTarEntry(TarArchiveOutputStream taos, String entryName, String content) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(entryName);
        entry.setSize(data.length);
        taos.putArchiveEntry(entry);
        taos.write(data);
        taos.closeArchiveEntry();
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
