package com.github.jmoalves.levain.util;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;

public class ProgressBar {
    private static final int BAR_WIDTH = 30;
    private static final char[] SPINNER = new char[] { '|', '/', '-', '\\' };
    private static final Duration MIN_RENDER_INTERVAL = Duration.ofMillis(150);
    private static final String EMPTY = "";

    private final PrintStream out;
    private final boolean inPlace;
    private String label;
    private long totalBytes;
    private final Instant startTime;
    private Instant lastRender;
    private int lastPercent = -1;
    private int spinnerIndex = 0;
    private long lastBytes = 0;
    private boolean finished = false;
    private int lastRenderLength = 0;

    public ProgressBar(String label, long totalBytes) {
        this(label, totalBytes, supportsInPlace());
    }

    ProgressBar(String label, long totalBytes, boolean inPlace) {
        this.out = System.out;
        this.inPlace = inPlace;
        this.label = label == null ? "Working" : label;
        this.totalBytes = totalBytes;
        this.startTime = Instant.now();
        this.lastRender = Instant.EPOCH;
        if (this.inPlace) {
            render(0);
        }
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
        if (!inPlace) {
            return;
        }
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
        if (inPlace) {
            render(lastBytes);
            out.print(System.lineSeparator());
        } else {
            out.print(buildMessage(lastBytes));
            out.print(System.lineSeparator());
        }
        out.flush();
        finished = true;
    }

    private void render(long processedBytes) {
        if (!inPlace) {
            return;
        }
        if (totalBytes > 0) {
            long capped = Math.min(processedBytes, totalBytes);
            int percent = (int) ((capped * 100) / totalBytes);
            if (percent == lastPercent && !shouldForceRender()) {
                return;
            }
            lastPercent = percent;
        }
        String msg = buildMessage(processedBytes);
        if (msg.isEmpty()) {
            return;
        }
        msg = padToClear(msg);
        out.print("\r" + msg);
        out.flush();
    }

    private String buildMessage(long processedBytes) {
        int maxWidth = getTerminalWidth();
        if (totalBytes > 0) {
            long capped = Math.min(processedBytes, totalBytes);
            int percent = (int) ((capped * 100) / totalBytes);
            int filled = (int) ((capped * BAR_WIDTH) / totalBytes);
            String bar = "[" + "#".repeat(filled) + "-".repeat(BAR_WIDTH - filled) + "]";
            String message = String.format("%s %s %3d%% (%s/%s)", label, bar, percent,
                formatBytes(capped), formatBytes(totalBytes));
            if (message.length() > maxWidth) {
                String resized = resizeLabel(label, message.length(), maxWidth);
                message = String.format("%s %s %3d%% (%s/%s)", resized, bar, percent,
                    formatBytes(capped), formatBytes(totalBytes));
            }
            return message;
        }

        char spinner = SPINNER[spinnerIndex++ % SPINNER.length];
        String message = String.format("%s %c %s", label, spinner, formatBytes(processedBytes));
        if (message.length() > maxWidth) {
            String resized = resizeLabel(label, message.length(), maxWidth);
            message = String.format("%s %c %s", resized, spinner, formatBytes(processedBytes));
        }
        return message;
    }

    private String padToClear(String msg) {
        if (msg.isEmpty()) {
            return msg;
        }
        int len = msg.length();
        if (len < lastRenderLength) {
            msg = msg + " ".repeat(lastRenderLength - len);
        }
        lastRenderLength = msg.length();
        return msg;
    }

    private boolean shouldForceRender() {
        return Duration.between(startTime, Instant.now()).toSeconds() < 2;
    }

    private static boolean supportsInPlace() {
        String term = System.getenv("TERM");
        if (term == null || term.isBlank() || "dumb".equals(term)) {
            return false;
        }
        return true;
    }

    private static int getTerminalWidth() {
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            try {
                int width = Integer.parseInt(columns.trim());
                if (width > 0) {
                    return width;
                }
            } catch (NumberFormatException e) {
                // Ignore invalid values.
            }
        }
        return 80;
    }

    private static String resizeLabel(String original, int messageLength, int maxWidth) {
        int overflow = messageLength - maxWidth;
        if (overflow <= 0) {
            return original;
        }
        int targetLength = original.length() - overflow;
        if (targetLength <= 3) {
            return original.substring(0, Math.max(0, targetLength));
        }
        return original.substring(0, targetLength - 3) + "...";
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