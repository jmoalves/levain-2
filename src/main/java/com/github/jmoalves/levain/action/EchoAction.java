package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Echo a message to standard output.
 *
 * Usage:
 *   - echo Hello world
 */
@ApplicationScoped
public class EchoAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(EchoAction.class);

    @Override
    public String name() {
        return "echo";
    }

    @Override
    public void execute(ActionContext context, List<String> args) {
        String message = args == null || args.isEmpty() ? "" : String.join(" ", args);
        System.out.println(message);
        logger.debug("echo: {}", message);
    }
}
