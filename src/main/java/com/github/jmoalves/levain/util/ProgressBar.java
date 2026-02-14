package com.github.jmoalves.levain.util;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;

public class ProgressBar {
    private static final int BAR_WIDTH = 30;
    private static final char[] SPINNER = new char[] { '|', '/', '-', '\\' };
    private static final Duration MIN_RENDER_INTERVAL = Duration.ofMillis(150);

    private final PrintStream out;
    private String label;
    private long totalBytes;
    private final Instant startTime;
    private Instant lastRender;
    private int lastPercent = -1;
    private int spinnerIndex = 0;
    private long lastBytes = 0;
    private boolean finished = false;

    public ProgressBar(String label, long totalBytes) {
        this.out = System.out;
        this.label = label == null ? "Working" : label;
        this.totalBytes = totalBytes;
        this.startTime = Instant.now();
        this.lastRender = Instant.EPOCH;
        render(0);
    }

    public void reset(String label, long totalBytes) {
        if (finished) {
            return;
        }
        this.label = label == null ? this.label : label;
        this.totalBytes = totalBytes;
        this.lastPercent = -1;
        this.spinnerIndex = 0;
        this.lastBytes = 0;
        render(0);
    }

    public void update(long processedBytes) {
        if (finished) {
            return;
        }
        this.lastBytes = processedBytes;
        Instant now = Instant.now();
        if (now.minus(MIN_RENDER_INTERVAL).isAfter(lastRender)) {
            render(processedBytes);
            lastRender = now;
        }
    }

    public void finish() {
        if (finished) {
            return;
        }
        render(lastBytes);
        out.print(System.lineSeparator());
        out.flush();
        finished = true;
    }

    private void render(long processedBytes) {
        if (totalBytes > 0) {
            long capped = Math.min(processedBytes, totalBytes);
            int percent = (int) ((capped * 100) / totalBytes);
            if (percent == lastPercent && !shouldForceRender()) {
                return;
            }
            lastPercent = percent;
            int filled = (int) ((capped * BAR_WIDTH) / totalBytes);
            String bar = "[" + "#".repeat(filled) + "-".repeat(BAR_WIDTH - filled) + "]";
                String msg = String.format("\r%s %s %3d%% (%s/%s)", label, bar, percent,
                    formatBytes(capped), formatBytes(totalBytes));
            out.print(msg);
            out.flush();
            return;
        }

        char spinner = SPINNER[spinnerIndex++ % SPINNER.length];
        String msg = String.format("\r%s %c %s", label, spinner, formatBytes(processedBytes));
        out.print(msg);
        out.flush();
    }

    private boolean shouldForceRender() {
        return Duration.between(startTime, Instant.now()).toSeconds() < 2;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.1f GB", gb);
    }
}