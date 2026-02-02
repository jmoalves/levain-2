package com.github.jmoalves.levain.repository;

import com.github.jmoalves.levain.model.Recipe;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for all repository implementations.
 * Provides common functionality like initialization tracking and recipe
 * caching.
 */
public abstract class AbstractRepository implements Repository {
    private final String name;
    private final String uri;
    private boolean initialized = false;

    protected AbstractRepository(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    @Override
    public void init() {
        // Subclasses should override and call setInitialized()
        setInitialized();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public int size() {
        return listRecipes().size();
    }

    @Override
    public String describe() {
        return String.format("%s (%s)", name, uri);
    }

    protected void setInitialized() {
        this.initialized = true;
    }
}
