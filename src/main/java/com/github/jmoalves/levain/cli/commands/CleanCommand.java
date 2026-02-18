package com.github.jmoalves.levain.cli.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main dispatcher for cleanup operations.
 * Handles: levain clean <subcommand>
 */
@Command(
    name = "clean",
    description = "Clean up old files and backups",
    subcommands = {
        CleanBackupsCommand.class
    }
)
public class CleanCommand implements Runnable {
    
    @Override
    public void run() {
        // This is just a dispatcher, print help if no subcommand provided
        CommandLine.usage(this, System.out);
    }
}
