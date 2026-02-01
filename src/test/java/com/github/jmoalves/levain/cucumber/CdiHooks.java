package com.github.jmoalves.levain.cucumber;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import io.cucumber.java.After;
import io.cucumber.java.Before;

/**
 * Cucumber hooks for CDI lifecycle management.
 * 
 * Initializes and manages the Weld CDI container for use during Cucumber
 * scenarios.
 */
public class CdiHooks {
    private static WeldContainer container;

    /**
     * Initialize CDI container before scenarios.
     */
    @Before
    public static void initializeCdi() {
        getContainer();
    }

    /**
     * Shutdown CDI container after scenarios.
     */
    @After
    public static void shutdownCdi() {
        closeContainer();
    }

    /**
     * Get the CDI container instance.
     * 
     * @return WeldContainer instance
     */
    public static WeldContainer getContainer() {
        if (container == null) {
            container = new Weld().initialize();
        }
        return container;
    }

    /**
     * Close the CDI container explicitly.
     */
    public static void closeContainer() {
        if (container != null) {
            container.close();
            container = null;
        }
    }
}
