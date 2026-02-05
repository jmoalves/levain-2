package com.github.jmoalves.levain.action;

import java.util.List;

/**
 * Action contract for Levain DSL commands.
 */
public interface Action {
    String name();

    void execute(ActionContext context, List<String> args) throws Exception;
}
