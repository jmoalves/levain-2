package com.github.jmoalves.levain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.cli.CdiCommandFactory;
import com.github.jmoalves.levain.cli.LevainCommand;

import picocli.CommandLine;

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

        int exitCode = 0;

        try (WeldContainer container = initializeCdiContainer()) {
            exitCode = executeCommand(container, args);
            logger.info("Levain finished with exit code: {}", exitCode);
        } catch (Exception e) {
            logger.error("Error executing command", e);
            exitCode = 1;
        }

        System.exit(exitCode);
    }

    /**
     * Initialize the CDI container using automatic bean discovery.
     * 
     * All beans marked with CDI scopes (@ApplicationScoped, @Dependent, etc.)
     * in the classpath will be automatically discovered via the beans.xml
     * configuration with bean-discovery-mode="all".
     * 
     * @return initialized WeldContainer
     */
    private static WeldContainer initializeCdiContainer() {
        Weld weld = new Weld();
        return weld.initialize();
    }

    /**
     * Execute the command line with CDI-injected dependencies.
     * 
     * @param container the CDI container
     * @param args      command line arguments
     * @return exit code from command execution
     */
    private static int executeCommand(WeldContainer container, String[] args) {
        LevainCommand command = container.select(LevainCommand.class).get();
        CommandLine cmd = new CommandLine(command, new CdiCommandFactory());
        CommandLine.IExecutionStrategy executionStrategy = parseResult -> {
            Object root = parseResult.commandSpec().root().userObject();
            if (root instanceof LevainCommand levainCommand) {
                levainCommand.applyOverrides();
            }
            return new CommandLine.RunLast().execute(parseResult);
        };
        cmd.setExecutionStrategy(executionStrategy);
        return cmd.execute(args);
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
