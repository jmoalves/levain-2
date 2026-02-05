package com.github.jmoalves.levain.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a Levain recipe for package installation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe {
    private String version;
    private String description;
    private String recipesDir;
    private Map<String, List<String>> commands;
    private String name;
    private List<String> dependencies;
    private Map<String, Object> customAttributes = new HashMap<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    @JsonAnySetter
    public void addCustomAttribute(String key, Object value) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    /**
     * Check if this recipe should skip automatic baseDir creation.
     * Supports both 'levain.pkg.skipInstallDir' (Levain 1 compatibility) and 
     * 'skipInstallDir' (Levain 2 shorthand).
     */
    public boolean shouldSkipInstallDir() {
        // Check newer shorthand attribute first
        Object skipValue = customAttributes.get("skipInstallDir");
        if (skipValue instanceof Boolean) {
            return (Boolean) skipValue;
        }
        if (skipValue instanceof String) {
            return Boolean.parseBoolean((String) skipValue);
        }
        
        // Check Levain 1 compatibility attribute
        skipValue = customAttributes.get("levain.pkg.skipInstallDir");
        if (skipValue instanceof Boolean) {
            return (Boolean) skipValue;
        }
        if (skipValue instanceof String) {
            return Boolean.parseBoolean((String) skipValue);
        }
        
        return false;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", recipesDir='" + recipesDir + '\'' +
                ", commands=" + commands +
                ", name='" + name + '\'' +
                ", dependencies=" + dependencies +
                ", customAttributes=" + customAttributes +
                '}';
    }

    /**
     * Get the minVersion requirement from this recipe.
     * Returns the value of 'levain.minVersion' attribute if present.
     *
     * @return minVersion string, or null if not specified
     */
    public String getMinVersion() {
        Object minVersionObj = customAttributes.get("levain.minVersion");
        if (minVersionObj instanceof String) {
            return (String) minVersionObj;
        }
        return null;
    }
}
