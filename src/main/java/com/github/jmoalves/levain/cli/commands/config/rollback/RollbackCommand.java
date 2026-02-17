package com.github.jmoalves.levain.cli.commands.config.rollback;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main dispatcher for rollback commands.
 * Handles: levain config rollback <subcommand>
 */
@Command(
    name = "rollback",
    description = "Manage rollback/restore operations for installed packages",
    subcommands = {
        RollbackListCommand.class,
        RollbackRestoreCommand.class
    }
)
public class RollbackCommand implements Runnable {
    
    @Override
    public void run() {
        // This is just a dispatcher, print help if no subcommand provided
        CommandLine.usage(this, System.out);
    }
}
