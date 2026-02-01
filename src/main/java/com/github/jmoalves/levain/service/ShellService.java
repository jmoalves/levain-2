package com.github.jmoalves.levain.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing shell environments.
 */
@ApplicationScoped
public class ShellService {
    private static final Logger logger = LoggerFactory.getLogger(ShellService.class);

    /**
     * Open a shell with specified packages in the environment.
     *
     * @param packages List of packages to include in shell environment
     * @throws IOException if shell cannot be opened
     */
    public void openShell(List<String> packages) throws IOException {
        logger.info("Opening shell with packages: {}", packages);

        // Determine OS and appropriate shell
        String os = System.getProperty("os.name").toLowerCase();
        List<String> command = new ArrayList<>();

        if (os.contains("win")) {
            command.add("cmd.exe");
            command.add("/k");
            command.add("echo Levain shell initialized with packages: " + String.join(", ", packages));
        } else {
            command.add("/bin/bash");
            command.add("-c");
            command.add("echo 'Levain shell initialized with packages: " + String.join(", ", packages) + "' && bash");
        }

        // TODO: Set up environment variables from packages
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();

        try {
            Process process = pb.start();
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Shell process interrupted", e);
        }
    }
}
