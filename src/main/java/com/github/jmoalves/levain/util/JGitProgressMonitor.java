package com.github.jmoalves.levain.util;

import org.eclipse.jgit.lib.ProgressMonitor;

public class JGitProgressMonitor implements ProgressMonitor {
    private ProgressBar bar;
    private long totalWork = -1;
    private long worked = 0;
    private boolean active = false;

    @Override
    public void start(int totalTasks) {
        // No-op
    }

    @Override
    public void beginTask(String title, int totalWork) {
        if (bar == null) {
            bar = new ProgressBar("Git sync", totalWork > 0 ? totalWork : -1);
        } else if (totalWork > 0 && this.totalWork <= 0) {
            bar.reset("Git sync", totalWork);
        }
        this.totalWork = totalWork > 0 ? totalWork : this.totalWork;
        this.worked = 0;
        this.active = true;
    }

    @Override
    public void update(int completed) {
        worked += completed;
        if (bar != null) {
            bar.update(worked);
        }
    }

    @Override
    public void endTask() {
        this.active = false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(boolean enabled) {
        // No-op
    }

    public void finish() {
        if (bar != null) {
            bar.finish();
            bar = null;
        }
    }
}