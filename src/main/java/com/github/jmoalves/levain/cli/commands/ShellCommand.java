package com.github.jmoalves.levain.cli.commands;

import com.github.jmoalves.levain.service.ShellService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to open a configured shell with specified packages.
 */
@ApplicationScoped
@Command(name = "shell", description = "Open a configured shell with specified packages", mixinStandardHelpOptions = true)
public class ShellCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShellCommand.class);
    private static final Logger console = LoggerFactory.getLogger("CONSOLE");

    @Parameters(arity = "0..*", description = "Package(s) to include in shell environment")
    private List<String> packages;

    @Inject
    private ShellService shellService;

    public ShellCommand() {
    }

    @Override
    public Integer call() {
        logger.info("Opening shell with packages: {}", packages);

        try {
            shellService.openShell(packages != null ? packages : List.of());
            return 0;
        } catch (Exception e) {
            logger.error("Failed to open shell", e);
            console.error("Failed to open shell: " + e.getMessage());
            return 1;
        }
    }
}
