package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Check if a TCP port is reachable.
 *
 * Usage:
 *   - checkPort <port>
 *   - checkPort <host> <port>
 *   - checkPort --timeout=2000 <host> <port>
 */
@ApplicationScoped
public class CheckPortAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CheckPortAction.class);

    @Override
    public String name() {
        return "checkPort";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("checkPort requires host/port arguments");
        }

        int timeoutMs = 2000;
        List<String> positionals = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("--timeout=")) {
                timeoutMs = parseTimeout(arg.substring("--timeout=".length()));
                continue;
            }
            if ("--timeout".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--timeout requires a value");
                }
                timeoutMs = parseTimeout(args.get(++i));
                continue;
            }
            positionals.add(arg);
        }

        String host;
        int port;
        if (positionals.size() == 1) {
            host = "localhost";
            port = parsePort(positionals.get(0));
        } else if (positionals.size() == 2) {
            host = positionals.get(0);
            port = parsePort(positionals.get(1));
        } else {
            throw new IllegalArgumentException("checkPort requires <port> or <host> <port>");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
        } catch (Exception e) {
            throw new IllegalArgumentException("checkPort failed: " + host + ":" + port, e);
        }

        logger.debug("checkPort OK: {}:{}", host, port);
    }

    private int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + value);
        }
    }

    private int parseTimeout(String value) {
        try {
            int timeout = Integer.parseInt(value.trim());
            if (timeout < 0) {
                throw new IllegalArgumentException("--timeout must be >= 0");
            }
            return timeout;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--timeout requires a numeric value");
        }
    }
}
