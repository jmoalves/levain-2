package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Set a recipe-scoped variable for use in subsequent actions.
 * 
 * This action stores variables in the recipe context, making them available
 * for variable substitution in subsequent action parameters.
 * 
 * Usage:
 *   - setVar mavenHome ${baseDir}/apache-maven
 *   - setVar JAVA_VERSION 11
 *   - setVar customPath ${baseDir}/bin:${baseDir}/lib
 * 
 * Variables set with setVar can be referenced in subsequent actions:
 *   - mkdir ${customPath}
 *   - copy ${customPath}/* /final/destination
 * 
 * Improvements over original levain implementation:
 * 1. Stores variables in recipe context (ActionContext) for scope awareness
 * 2. Supports variable substitution in the value (e.g., setVar X ${baseDir})
 * 3. Better error messages with clear guidance
 * 4. Flexible for future enhancements (e.g., scope options, conditional sets)
 */
@ApplicationScoped
public class SetVarAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(SetVarAction.class);

    @Override
    public String name() {
        return "setVar";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args.size() < 2) {
            throw new IllegalArgumentException(
                "setVar requires at least two arguments: name and value\n" +
                "Usage: setVar <name> <value>\n" +
                "Example: setVar myHome ${baseDir}"
            );
        }

        String varName = args.get(0);
        String varValue = args.get(1);

        // Store in recipe context (recipe-scoped variables)
        context.setRecipeVariable(varName, varValue);

        logger.debug("Set recipe variable: {} = {}", varName, varValue);
    }
}
