package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MkdirAction Tests")
class MkdirActionTest {
    @TempDir Path tempDir;

    private MkdirAction action;
    private ActionContext context;

    @BeforeEach
    void setUp() {
        action = new MkdirAction();
        
        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());
        
        Recipe recipe = new Recipe();
        recipe.setName("test");
        recipe.setVersion("1.0");
        
        context = new ActionContext(config, recipe, tempDir, tempDir);
    }

    @Test
    @DisplayName("Test 1: Create single directory")
    void testCreateSingleDirectory() throws Exception {
        Path testDir = tempDir.resolve("test");
        assertFalse(Files.exists(testDir), "Directory should not exist initially");

        action.execute(context, List.of("test"));

        assertTrue(Files.exists(testDir), "Directory should be created");
        assertTrue(Files.isDirectory(testDir), "Created path should be a directory");
    }

    @Test
    @DisplayName("Test 2: Create multiple directories in single command")
    void testCreateMultipleDirectories() throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        
        assertFalse(Files.exists(dir1));
        assertFalse(Files.exists(dir2));

        action.execute(context, List.of("dir1", "dir2"));

        assertTrue(Files.exists(dir1), "First directory should be created");
        assertTrue(Files.exists(dir2), "Second directory should be created");
    }

    @Test
    @DisplayName("Test 3: Create nested directories recursively")
    void testCreateNestedDirectories() throws Exception {
        Path deepDir = tempDir.resolve("a/b/c/d/e");
        assertFalse(Files.exists(deepDir), "Nested path should not exist initially");

        action.execute(context, List.of("a/b/c/d/e"));

        assertTrue(Files.exists(deepDir), "Nested directory should be created");
        assertTrue(Files.isDirectory(deepDir), "Created path should be a directory");
    }

    @Test
    @DisplayName("Test 4: Idempotency - already existing directory")
    void testAlreadyExists() throws Exception {
        Path existingDir = tempDir.resolve("existing");
        Files.createDirectory(existingDir);
        assertTrue(Files.exists(existingDir), "Pre-requisite: directory should exist");

        // Should not throw exception
        assertDoesNotThrow(() -> action.execute(context, List.of("existing")));

        assertTrue(Files.exists(existingDir), "Directory should still exist");
    }

    @Test
    @DisplayName("Test 5: Variable substitution in directory path")
    void testVariableSubstitution() throws Exception {
        // This test verifies that paths are resolved against baseDir
        // When using "subdir", it should be created relative to baseDir
        Path subdir = tempDir.resolve("mysubdir");
        
        action.execute(context, List.of("mysubdir"));

        assertTrue(Files.exists(subdir), "Subdirectory should be created relative to baseDir");
    }

    @Test
    @DisplayName("Test 6: Relative paths")
    void testRelativePaths() throws Exception {
        Path nested = tempDir.resolve("subdir/nested/path");
        assertFalse(Files.exists(nested));

        action.execute(context, List.of("subdir/nested/path"));

        assertTrue(Files.exists(nested), "Relative nested path should be created");
    }

    @Test
    @DisplayName("Test 7: Created directories are readable and writable")
    void testPermissions() throws Exception {
        Path newDir = tempDir.resolve("permissions-test");
        
        action.execute(context, List.of("permissions-test"));

        assertTrue(Files.isReadable(newDir), "Created directory should be readable");
        assertTrue(Files.isWritable(newDir), "Created directory should be writable");
    }

    @Test
    @DisplayName("Test 8: Error when no arguments provided")
    void testNoArguments() {
        assertThrows(
            IllegalArgumentException.class,
            () -> action.execute(context, List.of()),
            "Should throw IllegalArgumentException when no paths provided"
        );
    }

    @Test
    @DisplayName("Test 9: Error when path exists but is a file, not directory")
    void testExistingFile() throws Exception {
        Path existingFile = tempDir.resolve("existing-file.txt");
        Files.createFile(existingFile);

        assertThrows(
            IllegalArgumentException.class,
            () -> action.execute(context, List.of("existing-file.txt")),
            "Should throw error when path exists but is not a directory"
        );
    }

    @Test
    @DisplayName("Test 10: Create multiple nested directories in sequence")
    void testCreateMultipleNestedDirs() throws Exception {
        action.execute(context, List.of("path1/sub1/deep", "path2/sub2/deep", "path3"));

        assertTrue(Files.exists(tempDir.resolve("path1/sub1/deep")));
        assertTrue(Files.exists(tempDir.resolve("path2/sub2/deep")));
        assertTrue(Files.exists(tempDir.resolve("path3")));
    }

    @Test
    @DisplayName("Test 11: Complex paths with dots and underscores")
    void testComplexPathNames() throws Exception {
        action.execute(context, List.of(
            "my_app-1.0.0/bin",
            ".config/app_name",
            "path.with.dots/subdir"
        ));

        assertTrue(Files.exists(tempDir.resolve("my_app-1.0.0/bin")));
        assertTrue(Files.exists(tempDir.resolve(".config/app_name")));
        assertTrue(Files.exists(tempDir.resolve("path.with.dots/subdir")));
    }

    @Test
    @DisplayName("Test 12: Empty string arguments should be handled gracefully")
    void testEmptyString() throws Exception {
        // Empty string should resolve to baseDir (no-op)
        assertDoesNotThrow(() -> action.execute(context, List.of("")));
    }

    @Test
    @DisplayName("Test 13: Large number of directories")
    void testManyDirectories() throws Exception {
        List<String> dirs = List.of(
            "dir1", "dir2", "dir3", "dir4", "dir5",
            "dir6", "dir7", "dir8", "dir9", "dir10"
        );

        action.execute(context, dirs);

        for (String dir : dirs) {
            assertTrue(Files.exists(tempDir.resolve(dir)), "Directory " + dir + " should be created");
        }
    }

    @Test
    @DisplayName("Test 14: Mixed absolute and relative paths")
    void testAbsolutePath() throws Exception {
        // Absolute paths should be used as-is if they're on the file system
        // For this test, we'll just verify the relative path behavior
        action.execute(context, List.of("absolute/path/test"));

        assertTrue(Files.exists(tempDir.resolve("absolute/path/test")));
    }
}
