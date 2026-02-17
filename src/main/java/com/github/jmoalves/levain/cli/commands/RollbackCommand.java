package com.github.jmoalves.levain.cli.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main dispatcher for rollback/restore operations.
 * Handles: levain rollback <subcommand>
 */
@Command(
    name = "rollback",
    description = "Rollback/restore packages from backups",
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
