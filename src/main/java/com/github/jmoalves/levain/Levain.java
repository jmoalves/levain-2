package com.github.jmoalves.levain;

import com.github.jmoalves.levain.cli.CdiCommandFactory;
import com.github.jmoalves.levain.cli.LevainCommand;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main entry point for Levain 2 - A Java console application for development
 * environment installation.
 * This is a drop-in replacement for the original levain (TypeScript/Deno)
 * version.
 */
public class Levain {
    private static final Logger logger = LoggerFactory.getLogger(Levain.class);

    public static void main(String[] args) {
        String version = getVersion();
        logger.info("Starting Levain {}...", version);

        // Initialize CDI container
        Weld weld = new Weld();
        try (WeldContainer container = weld.initialize()) {
            // Get LevainCommand instance from CDI with all dependencies injected
            LevainCommand command = container.select(LevainCommand.class).get();

            // Create CommandLine with CDI factory for subcommands
            int exitCode = new CommandLine(command, new CdiCommandFactory()).execute(args);

            logger.info("Levain finished with exit code: {}", exitCode);
            System.exit(exitCode);
        }
    }

    /**
     * Get the application version from Maven properties.
     * 
     * @return version string, or "unknown" if not available
     */
    private static String getVersion() {
        try (InputStream input = Levain.class
                .getResourceAsStream("/META-INF/maven/com.github.jmoalves/levain/pom.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            logger.debug("Could not read version from pom.properties", e);
        }
        return "unknown";
    }
}
