package com.github.jmoalves.levain.cli;

import jakarta.enterprise.inject.spi.CDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Custom Picocli factory that uses CDI to create command instances.
 * 
 * This ensures that all Picocli commands get their dependencies injected
 * from the CDI container.
 */
public class CdiCommandFactory implements CommandLine.IFactory {
    private static final Logger logger = LoggerFactory.getLogger(CdiCommandFactory.class);

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            // Get the instance from CDI
            var instance = CDI.current().select(cls).get();
            logger.debug("Created bean of type {} from CDI", cls.getName());
            return instance;
        } catch (Exception e) {
            logger.error("Failed to create bean of type {} from CDI: {}", cls.getName(), e.getMessage());
            throw new CommandLine.ExecutionException(
                    new CommandLine(new Object()),
                    "Cannot instantiate " + cls.getName() + " from CDI: " + e.getMessage());
        }
    }
}
