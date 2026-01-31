package com.github.jmoalves.levain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a Levain recipe for package installation.
 */
public class Recipe {
    private String version;
    private String description;
    private String recipesDir;
    private Map<String, List<String>> commands;
    
    public Recipe() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecipesDir() {
        return recipesDir;
    }

    public void setRecipesDir(String recipesDir) {
        this.recipesDir = recipesDir;
    }

    public Map<String, List<String>> getCommands() {
        return commands;
    }

    public void setCommands(Map<String, List<String>> commands) {
        this.commands = commands;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", recipesDir='" + recipesDir + '\'' +
                ", commands=" + commands +
                '}';
    }
}
