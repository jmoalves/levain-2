package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CopyAction.
 * 
 * This is a critical action for recipe installation (especially bootstrap).
 * Must handle:
 * - Local file copying (absolute and relative paths)
 * - Remote file downloading and copying
 * - Directory creation
 * - File replacement
 * - Verbose logging
 * - Error handling
 */
class CopyActionTest {
    @TempDir
    Path tempDir;

    private CopyAction action;
    private Config config;

    @BeforeEach
    void setUp() {
        config = createConfig();
        action = new CopyAction(new FileCache(config));
    }

    // ========== Basic Local File Copy Tests ==========

    @Test
    void testCopyLocalFileAbsolutePaths() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "test content");

        Path dst = tempDir.resolve("dest.txt");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst), "Destination file should exist");
        assertEquals("test content", Files.readString(dst), "Content should match");
    }

    @Test
    void testCopyLocalFileRelativePaths() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Files.createDirectories(recipeDir);
        
        Path src = recipeDir.resolve("source.txt");
        Files.writeString(src, "relative content");

        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(baseDir);

        ActionContext context = createContext(recipeDir, baseDir);
        action.execute(context, List.of("source.txt", "destination.txt"));

        Path dst = baseDir.resolve("destination.txt");
        assertTrue(Files.exists(dst), "Destination file should exist");
        assertEquals("relative content", Files.readString(dst), "Content should match");
    }

    @Test
    void testCopyFileToSubdirectory() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "nested content");

        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(baseDir);

        ActionContext context = createContext(tempDir, baseDir);
        action.execute(context, List.of(src.toString(), "subdir/nested/file.txt"));

        Path dst = baseDir.resolve("subdir/nested/file.txt");
        assertTrue(Files.exists(dst), "Destination file should exist in nested directory");
        assertEquals("nested content", Files.readString(dst));
    }

    @Test
    void testCopyCreatesParentDirectories() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "content");

        Path baseDir = tempDir.resolve("base");
        Path nestedDst = baseDir.resolve("a/b/c/d/file.txt");

        ActionContext context = createContext(tempDir, baseDir);
        action.execute(context, List.of(src.toString(), nestedDst.toString()));

        assertTrue(Files.exists(nestedDst), "File should exist");
        assertTrue(Files.isDirectory(baseDir.resolve("a/b/c/d")), "All parent directories should be created");
    }

    @Test
    void testCopyReplacesExistingFile() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "new content");

        Path dst = tempDir.resolve("existing.txt");
        Files.writeString(dst, "old content");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertEquals("new content", Files.readString(dst), "Should replace existing file");
    }

    // ========== Binary File Tests ==========

    @Test
    void testCopyBinaryFile() throws Exception {
        Path src = tempDir.resolve("binary.dat");
        byte[] binaryData = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE };
        Files.write(src, binaryData);

        Path dst = tempDir.resolve("binary-copy.dat");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertArrayEquals(binaryData, Files.readAllBytes(dst), "Binary content should match exactly");
    }

    @Test
    void testCopyLargeFile() throws Exception {
        Path src = tempDir.resolve("large.bin");
        
        // Create a 1MB file
        byte[] chunk = new byte[1024]; // 1KB
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = (byte) (i % 256);
        }
        
        Files.write(src, new byte[0]); // Create empty file
        for (int i = 0; i < 1024; i++) { // Write 1MB
            Files.write(src, chunk, java.nio.file.StandardOpenOption.APPEND);
        }

        Path dst = tempDir.resolve("large-copy.bin");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertEquals(Files.size(src), Files.size(dst), "File size should match");
        assertArrayEquals(Files.readAllBytes(src), Files.readAllBytes(dst), "Content should match");
    }

    // ========== File with Special Characters ==========

    @Test
    void testCopyFileWithSpacesInName() throws Exception {
        Path recipeDir = tempDir.resolve("recipe");
        Files.createDirectories(recipeDir);
        
        Path src = recipeDir.resolve("file with spaces.txt");
        Files.writeString(src, "spaces content");

        Path baseDir = tempDir.resolve("base");
        Files.createDirectories(baseDir);

        ActionContext context = createContext(recipeDir, baseDir);
        action.execute(context, List.of("file with spaces.txt", "output with spaces.txt"));

        Path dst = baseDir.resolve("output with spaces.txt");
        assertTrue(Files.exists(dst));
        assertEquals("spaces content", Files.readString(dst));
    }

    @Test
    void testCopyFileWithSpecialCharacters() throws Exception {
        Path src = tempDir.resolve("file-with_special.chars.txt");
        Files.writeString(src, "special content");

        Path dst = tempDir.resolve("dest-special_chars.txt");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertEquals("special content", Files.readString(dst));
    }

    // ========== Verbose Flag Tests ==========

    @Test
    void testCopyWithVerboseFlag() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "verbose test");

        Path dst = tempDir.resolve("dest.txt");

        ActionContext context = createContext(tempDir, tempDir);
        
        // Should not throw exception with --verbose flag
        assertDoesNotThrow(() -> {
            action.execute(context, List.of("--verbose", src.toString(), dst.toString()));
        });

        assertTrue(Files.exists(dst));
        assertEquals("verbose test", Files.readString(dst));
    }

    @Test
    void testCopyWithoutVerboseFlag() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "quiet test");

        Path dst = tempDir.resolve("dest.txt");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertEquals("quiet test", Files.readString(dst));
    }

    // ========== Error Handling Tests ==========

    @Test
    void testCopyThrowsWhenSourceNotExists() {
        Path nonexistent = tempDir.resolve("nonexistent.txt");
        Path dst = tempDir.resolve("dest.txt");

        ActionContext context = createContext(tempDir, tempDir);

        Exception ex = assertThrows(Exception.class, () -> {
            action.execute(context, List.of(nonexistent.toString(), dst.toString()));
        });

        // Should throw an exception about file not existing
        assertNotNull(ex);
    }

    @Test
    void testCopyThrowsWhenTooFewArguments() {
        ActionContext context = createContext(tempDir, tempDir);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            action.execute(context, List.of("onlyOneArgument"));
        });

        assertTrue(ex.getMessage().contains("must inform"));
    }

    @Test
    void testCopyThrowsWhenTooManyArguments() {
        ActionContext context = createContext(tempDir, tempDir);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            action.execute(context, List.of("arg1", "arg2", "arg3"));
        });

        assertTrue(ex.getMessage().contains("must inform"));
    }

    @Test
    void testCopyThrowsWhenNoArguments() {
        ActionContext context = createContext(tempDir, tempDir);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            action.execute(context, List.of());
        });

        assertTrue(ex.getMessage().contains("must inform"));
    }

    // ========== Variable Substitution Tests ==========

    @Test
    void testCopyWithBaseDirectoryVariable() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "base dir test");

        Path baseDir = tempDir.resolve("installation");
        Files.createDirectories(baseDir);

        ActionContext context = createContext(tempDir, baseDir);
        
        // The ${baseDir} would be substituted by VariableSubstitutionService in real usage
        // Here we test that relative paths work correctly
        action.execute(context, List.of(src.toString(), "output.txt"));

        Path dst = baseDir.resolve("output.txt");
        assertTrue(Files.exists(dst));
    }

    // ========== Empty File Tests ==========

    @Test
    void testCopyEmptyFile() throws Exception {
        Path src = tempDir.resolve("empty.txt");
        Files.createFile(src); // Create empty file

        Path dst = tempDir.resolve("empty-copy.txt");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertTrue(Files.exists(dst));
        assertEquals(0, Files.size(dst), "Copied file should be empty");
    }

    // ========== Preserve File Attributes Tests ==========

    @Test
    void testCopyPreservesFileContent() throws Exception {
        Path src = tempDir.resolve("original.txt");
        String originalContent = "Line 1\nLine 2\nLine 3\n";
        Files.writeString(src, originalContent, StandardCharsets.UTF_8);

        Path dst = tempDir.resolve("copied.txt");

        ActionContext context = createContext(tempDir, tempDir);
        action.execute(context, List.of(src.toString(), dst.toString()));

        assertEquals(originalContent, Files.readString(dst, StandardCharsets.UTF_8));
    }

    // ========== Multiple File Operations ==========

    @Test
    void testMultipleCopiesFromSameSource() throws Exception {
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "shared content");

        ActionContext context = createContext(tempDir, tempDir);

        // Copy to first destination
        Path dst1 = tempDir.resolve("copy1.txt");
        action.execute(context, List.of(src.toString(), dst1.toString()));

        // Copy to second destination
        Path dst2 = tempDir.resolve("copy2.txt");
        action.execute(context, List.of(src.toString(), dst2.toString()));

        // Both should exist with same content
        assertTrue(Files.exists(dst1));
        assertTrue(Files.exists(dst2));
        assertEquals("shared content", Files.readString(dst1));
        assertEquals("shared content", Files.readString(dst2));
    }

    @Test
    void testCopyChainSourceToDstThenDstToFinal() throws Exception {
        // Create source
        Path src = tempDir.resolve("source.txt");
        Files.writeString(src, "chain content");

        ActionContext context = createContext(tempDir, tempDir);

        // Copy to intermediate
        Path intermediate = tempDir.resolve("intermediate.txt");
        action.execute(context, List.of(src.toString(), intermediate.toString()));

        // Copy intermediate to final
        Path finalDst = tempDir.resolve("final.txt");
        action.execute(context, List.of(intermediate.toString(), finalDst.toString()));

        // All three should have same content
        assertEquals("chain content", Files.readString(src));
        assertEquals("chain content", Files.readString(intermediate));
        assertEquals("chain content", Files.readString(finalDst));
    }

    // ========== Action Interface Tests ==========

    @Test
    void testActionName() {
        assertEquals("copy", action.name());
    }

    // ========== Helper Methods ==========

    private Config createConfig() {
        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());
        return config;
    }

    private Recipe createTestRecipe() {
        Recipe recipe = new Recipe();
        recipe.setName("test-recipe");
        recipe.setVersion("1.0.0");
        recipe.setDescription("Test recipe for CopyAction tests");
        return recipe;
    }

    private ActionContext createContext(Path recipeDir, Path baseDir) {
        return new ActionContext(config, createTestRecipe(), baseDir, recipeDir);
    }
}
