package com.github.jmoalves.levain.cucumber;

import io.cucumber.core.backend.ObjectFactory;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Cucumber ObjectFactory implementation using CDI (Weld).
 * 
 * This factory creates instances of step definition classes using the CDI
 * container,
 * enabling dependency injection in Cucumber scenarios.
 */
public class CdiObjectFactory implements ObjectFactory {

    public CdiObjectFactory() {
        // ObjectFactory is instantiated by Cucumber.
        // Ensure CDI container is initialized via CdiHooks.
        CdiHooks.getContainer();
    }

    @Override
    public void start() {
        // Container is managed by CdiHooks
    }

    @Override
    public void stop() {
        // Container will be closed by CdiHooks @After hook
    }

    @Override
    public <T> T getInstance(Class<T> glueClass) {
        // Get the CDI container and retrieve the bean instance
        WeldContainer container = CdiHooks.getContainer();
        return container.select(glueClass).get();
    }

    @Override
    public boolean addClass(Class<?> glueClass) {
        // CDI automatically discovers all beans via bean discovery mode
        return true;
    }
}
