package com.github.jmoalves.levain.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProgressBar Tests")
class ProgressBarTest {

    @Test
    @DisplayName("Should render and finish in-place")
    void testRenderAndFinishInPlace() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out));
            ProgressBar bar = new ProgressBar("Test", 10);
            bar.update(5);
            bar.update(10);
            bar.finish();
        } finally {
            System.setOut(originalOut);
        }
        assertTrue(out.toString().contains("Test"));
    }

    @Test
    @DisplayName("Should render spinner for unknown total")
    void testSpinnerRendering() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out));
            ProgressBar bar = new ProgressBar("Spin", -1);
            bar.update(1);
            bar.finish();
        } finally {
            System.setOut(originalOut);
        }
        assertTrue(out.toString().contains("Spin"));
    }

    @Test
    @DisplayName("Should resize long labels")
    void testResizeLabel() throws Exception {
        Method resize = ProgressBar.class.getDeclaredMethod("resizeLabel", String.class, int.class, int.class);
        resize.setAccessible(true);
        String resized = (String) resize.invoke(null, "LongLabelName", 40, 10);
        assertTrue(resized.length() <= 10);
    }

    @Test
    @DisplayName("Should compute terminal width")
    void testGetTerminalWidth() throws Exception {
        Method width = ProgressBar.class.getDeclaredMethod("getTerminalWidth");
        width.setAccessible(true);
        int value = (int) width.invoke(null);
        assertTrue(value > 0);
    }

    @Test
    @DisplayName("Should not throw on reset")
    void testReset() {
        ProgressBar bar = new ProgressBar("Reset", 5);
        assertDoesNotThrow(() -> bar.reset("Reset", 5));
    }
}
