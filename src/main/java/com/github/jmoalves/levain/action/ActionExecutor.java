package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes Levain DSL commands by dispatching to actions.
 */
@ApplicationScoped
public class ActionExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|\\S+");

    private final Map<String, Action> actions = new HashMap<>();

    @Inject
    public ActionExecutor(Instance<Action> actionInstances) {
        for (Action action : actionInstances) {
            actions.put(action.name(), action);
        }
    }

    public void executeCommands(List<String> commands, ActionContext context) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }

            List<String> tokens = tokenize(command);
            if (tokens.isEmpty()) {
                continue;
            }

            String actionName = tokens.get(0);
            Action action = actions.get(actionName);
            if (action == null) {
                logger.warn("Unknown action '{}', skipping command: {}", actionName, command);
                continue;
            }

            List<String> args = tokens.subList(1, tokens.size());
            try {
                action.execute(context, args);
            } catch (Exception e) {
                throw new RuntimeException("Action '" + actionName + "' failed: " + e.getMessage(), e);
            }
        }
    }

    private List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(command);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token == null) {
                token = matcher.group(2);
            }
            if (token == null) {
                token = matcher.group();
            }
            if (token != null && !token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
