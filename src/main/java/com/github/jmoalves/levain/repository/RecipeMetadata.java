package com.github.jmoalves.levain.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Metadata about an installed recipe.
 * Stored alongside the recipe YAML file in the registry to track
 * installation details and source information.
 * 
 * File format: {recipeName}.levain.meta (JSON)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeMetadata {
    private String recipeName;
    private String sourceRepository;
    private String sourceRepositoryUri;
    private String installedAt;
    private String installedVersion;

    public RecipeMetadata() {
    }

    public RecipeMetadata(String recipeName, String sourceRepository, String sourceRepositoryUri) {
        this.recipeName = recipeName;
        this.sourceRepository = sourceRepository;
        this.sourceRepositoryUri = sourceRepositoryUri;
        this.installedAt = Instant.now().toString();
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getSourceRepository() {
        return sourceRepository;
    }

    public void setSourceRepository(String sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public String getSourceRepositoryUri() {
        return sourceRepositoryUri;
    }

    public void setSourceRepositoryUri(String sourceRepositoryUri) {
        this.sourceRepositoryUri = sourceRepositoryUri;
    }

    public String getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(String installedAt) {
        this.installedAt = installedAt;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
    }

    @Override
    public String toString() {
        return "RecipeMetadata{" +
                "recipeName='" + recipeName + '\'' +
                ", sourceRepository='" + sourceRepository + '\'' +
                ", sourceRepositoryUri='" + sourceRepositoryUri + '\'' +
                ", installedAt='" + installedAt + '\'' +
                ", installedVersion='" + installedVersion + '\'' +
                '}';
    }
}
