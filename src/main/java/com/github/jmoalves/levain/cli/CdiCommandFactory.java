package com.github.jmoalves.levain.cli;

import jakarta.enterprise.inject.spi.CDI;
import picocli.CommandLine;

/**
 * Custom Picocli factory that uses CDI to create command instances.
 * 
 * This ensures that all Picocli commands get their dependencies injected
 * from the CDI container.
 */
public class CdiCommandFactory implements CommandLine.IFactory {

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            // Try to get the instance from CDI
            return CDI.current().select(cls).get();
        } catch (Exception e) {
            // Fall back to default construction if CDI doesn't have the bean
            return CommandLine.defaultFactory().create(cls);
        }
    }
}
