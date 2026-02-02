package com.github.jmoalves.levain.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jmoalves.levain.service.ShellService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to open a configured shell with specified packages.
 */
@Command(name = "shell", description = "Open a configured shell with specified packages", mixinStandardHelpOptions = true)
public class ShellCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "0..*", description = "Package(s) to include in shell environment")
    private List<String> packages;

    private final ShellService shellService;

    @Inject
    public ShellCommand(ShellService shellService) {
        this.shellService = shellService;
    }

    @Override
    public Integer call() {
        logger.info("Opening shell with packages: {}", packages);

        try {
            shellService.openShell(packages != null ? packages : List.of());
            return 0;
        } catch (Exception e) {
            logger.error("Failed to open shell", e);
            console.error("Failed to open shell: {}", e.getMessage());
            return 1;
        }
    }
}
