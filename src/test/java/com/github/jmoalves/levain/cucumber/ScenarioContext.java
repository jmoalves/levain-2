package com.github.jmoalves.levain.cucumber;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Shared context for Cucumber scenarios.
 * Stores state between steps in a scenario.
 */
@ApplicationScoped
public class ScenarioContext {
    private boolean installSuccessful;
    private Exception installException;
    private Object recipes;

    public boolean isInstallSuccessful() {
        return installSuccessful;
    }

    public void setInstallSuccessful(boolean installSuccessful) {
        this.installSuccessful = installSuccessful;
    }

    public Exception getInstallException() {
        return installException;
    }

    public void setInstallException(Exception installException) {
        this.installException = installException;
    }

    public Object getRecipes() {
        return recipes;
    }

    public void setRecipes(Object recipes) {
        this.recipes = recipes;
    }

    public void reset() {
        this.installSuccessful = false;
        this.installException = null;
        this.recipes = null;
    }
}
