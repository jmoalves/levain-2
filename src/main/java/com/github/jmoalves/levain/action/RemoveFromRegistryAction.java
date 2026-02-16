package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.repository.Registry;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Remove one or more recipes from the registry.
 *
 * Usage:
 *   - removeFromRegistry <recipeName> [recipeName2] ...
 */
@ApplicationScoped
public class RemoveFromRegistryAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(RemoveFromRegistryAction.class);

    @Override
    public String name() {
        return "removeFromRegistry";
    }

    @Override
    public void execute(ActionContext context, List<String> args) {
        if (context == null || context.getConfig() == null) {
            throw new IllegalArgumentException("removeFromRegistry requires an action context");
        }
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("removeFromRegistry requires at least one recipe name");
        }

        Path registryDir = context.getConfig().getRegistryDir();
        Registry registry = new Registry(registryDir.toString());
        registry.init();

        for (String name : args) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("removeFromRegistry requires non-blank recipe names");
            }
            boolean removed = registry.remove(name);
            if (!removed) {
                logger.debug("Recipe not found in registry: {}", name);
            }
        }
    }
}
