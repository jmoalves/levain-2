package com.github.jmoalves.levain.util;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class GitSupport {
    private GitSupport() {
    }

    public static void cloneRepository(String url, Path destination, String branch, Integer depth, boolean recursive)
            throws IOException {
        JGitProgressMonitor monitor = new JGitProgressMonitor();
        try {
            CloneCommand command = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination.toFile())
                    .setProgressMonitor(monitor);

            if (branch != null && !branch.isBlank()) {
                command.setBranch(branch);
            }
            if (depth != null && depth > 0) {
                command.setDepth(depth);
            }
            if (recursive) {
                command.setCloneSubmodules(true);
            }

            command.call().close();
        } catch (GitAPIException e) {
            throw new IOException("Failed to clone git repository: " + url + ": " + e.getMessage(), e);
        } finally {
            monitor.finish();
        }
    }

    public static void pullRepository(Path repositoryDir) throws IOException {
        File gitDir = repositoryDir.resolve(".git").toFile();
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        JGitProgressMonitor monitor = new JGitProgressMonitor();
        try (Repository repository = builder.setGitDir(gitDir)
                .setWorkTree(repositoryDir.toFile())
                .readEnvironment()
                .findGitDir()
                .build();
             Git git = new Git(repository)) {
            git.pull().setProgressMonitor(monitor).call();
        } catch (GitAPIException e) {
            throw new IOException("Failed to pull git repository: " + repositoryDir + ": " + e.getMessage(), e);
        } finally {
            monitor.finish();
        }
    }
}
