package com.github.jmoalves.levain;

import com.github.jmoalves.levain.cli.LevainCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Main entry point for Levain 2 - A Java console application for development environment installation.
 * This is a drop-in replacement for the original levain (TypeScript/Deno) version.
 */
public class Levain {
    private static final Logger logger = LoggerFactory.getLogger(Levain.class);

    public static void main(String[] args) {
        logger.info("Starting Levain 2.0...");
        
        int exitCode = new CommandLine(new LevainCommand()).execute(args);
        
        logger.info("Levain finished with exit code: {}", exitCode);
        System.exit(exitCode);
    }
}
