package com.github.jmoalves.levain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Levain configuration model.
 */
public class LevainConfig {
    private List<RepositoryConfig> repositories = new ArrayList<>();

    public List<RepositoryConfig> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryConfig> repositories) {
        this.repositories = repositories;
    }
}
