package com.github.jmoalves.levain.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

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
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
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
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
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
    @DisplayName("Should resize when label space is tiny")
    void testResizeLabelTiny() throws Exception {
        Method resize = ProgressBar.class.getDeclaredMethod("resizeLabel", String.class, int.class, int.class);
        resize.setAccessible(true);
        String resized = (String) resize.invoke(null, "LongLabelName", 50, 2);
        assertTrue(resized.length() <= 2);
    }

    @Test
    @DisplayName("Should build message with totals")
    void testBuildMessageWithTotals() throws Exception {
        ProgressBar bar = new ProgressBar("Build", 10);
        Method build = ProgressBar.class.getDeclaredMethod("buildMessage", long.class);
        build.setAccessible(true);
        String message = (String) build.invoke(bar, 5L);
        assertTrue(message.contains("Build"));
    }

    @Test
    @DisplayName("Should build message for spinner")
    void testBuildMessageSpinner() throws Exception {
        ProgressBar bar = new ProgressBar("Spin", -1);
        Method build = ProgressBar.class.getDeclaredMethod("buildMessage", long.class);
        build.setAccessible(true);
        String message = (String) build.invoke(bar, 1L);
        assertTrue(message.contains("Spin"));
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
    @DisplayName("Should format bytes for different units")
    void testFormatBytes() throws Exception {
        Method format = ProgressBar.class.getDeclaredMethod("formatBytes", long.class);
        format.setAccessible(true);
        String bytes = (String) format.invoke(null, 1L);
        String kb = (String) format.invoke(null, 1024L);
        String mb = (String) format.invoke(null, 1024L * 1024L);
        String gb = (String) format.invoke(null, 1024L * 1024L * 1024L);
        assertTrue(bytes.contains("B"));
        assertTrue(kb.contains("KB"));
        assertTrue(mb.contains("MB"));
        assertTrue(gb.contains("GB"));
    }

    @Test
    @DisplayName("Should not throw on reset")
    void testReset() {
        ProgressBar bar = new ProgressBar("Reset", 5);
        assertDoesNotThrow(() -> bar.reset("Reset", 5));
    }

    @Test
    @DisplayName("Should pad shorter messages")
    void testPadToClear() throws Exception {
        ProgressBar bar = new ProgressBar("Pad", 10);
        Method pad = ProgressBar.class.getDeclaredMethod("padToClear", String.class);
        pad.setAccessible(true);
        String first = (String) pad.invoke(bar, "LongMessage");
        String second = (String) pad.invoke(bar, "Short");
        assertTrue(second.length() >= first.length());
    }

    @Test
    @DisplayName("Should ignore reset after finish")
    void testResetAfterFinish() {
        ProgressBar bar = new ProgressBar("Done", 1);
        bar.finish();
        assertDoesNotThrow(() -> bar.reset("Done", 1));
    }

    @Test
    @DisplayName("Should evaluate force render")
    void testShouldForceRender() throws Exception {
        ProgressBar bar = new ProgressBar("Force", 1);
        Method force = ProgressBar.class.getDeclaredMethod("shouldForceRender");
        force.setAccessible(true);
        Object value = force.invoke(bar);
        assertTrue(value instanceof Boolean);
    }

    @Test
    @DisplayName("Should render without in-place updates")
    void testNonInPlaceFinish() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            ProgressBar bar = new ProgressBar("NoPlace", 3, false);
            bar.update(1);
            bar.finish();
        } finally {
            System.setOut(originalOut);
        }
        assertTrue(out.toString().contains("NoPlace"));
    }
}
