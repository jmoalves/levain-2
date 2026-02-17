package com.github.jmoalves.levain.cli.commands.config.backup;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

/**
 * Subcommand to manage backup configuration.
 */
@Command(name = "backup", description = "Manage backup configuration", mixinStandardHelpOptions = true, subcommands = {
        BackupShowCommand.class,
        BackupSetDirCommand.class,
        BackupEnableCommand.class,
        BackupDisableCommand.class,
        BackupSetKeepCountCommand.class,
        BackupSetMaxAgeCommand.class
})
public class BackupCommand implements Callable<Integer> {
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Override
    public Integer call() {
        console.info("Use 'levain config backup --help' to see available subcommands");
        return 0;
    }
}
