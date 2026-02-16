package com.github.jmoalves.levain.action;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check if a URL is reachable and returns an expected status code.
 *
 * Usage:
 *   - checkUrl <url>
 *   - checkUrl --status=200,302 --timeout=8000 --method=GET <url>
 */
@ApplicationScoped
public class CheckUrlAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(CheckUrlAction.class);

    @Override
    public String name() {
        return "checkUrl";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("checkUrl requires a URL");
        }

        String method = "GET";
        int timeoutMs = 5000;
        Set<Integer> expectedStatuses = null;
        String url = null;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("--method=")) {
                method = arg.substring("--method=".length()).trim().toUpperCase();
                continue;
            }
            if ("--method".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--method requires a value");
                }
                method = args.get(++i).trim().toUpperCase();
                continue;
            }
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
            if (arg.startsWith("--status=")) {
                expectedStatuses = parseStatuses(arg.substring("--status=".length()));
                continue;
            }
            if ("--status".equals(arg)) {
                if (i + 1 >= args.size()) {
                    throw new IllegalArgumentException("--status requires a value");
                }
                expectedStatuses = parseStatuses(args.get(++i));
                continue;
            }
            if (url == null) {
                url = arg;
                continue;
            }
            throw new IllegalArgumentException("Unexpected argument: " + arg);
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("checkUrl requires a URL");
        }

        int status = fetchStatus(url, method, timeoutMs);
        if ("HEAD".equals(method) && (status == 405 || status == 501)) {
            status = fetchStatus(url, "GET", timeoutMs);
        }

        if (expectedStatuses != null && !expectedStatuses.contains(status)) {
            throw new IllegalArgumentException("checkUrl failed: status " + status + " for " + url);
        }
        if (expectedStatuses == null && (status < 200 || status >= 400)) {
            throw new IllegalArgumentException("checkUrl failed: status " + status + " for " + url);
        }

        logger.debug("checkUrl OK: {} (status {})", url, status);
    }

    private int fetchStatus(String url, String method, int timeoutMs) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.connect();
        int status = connection.getResponseCode();
        connection.disconnect();
        return status;
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

    private Set<Integer> parseStatuses(String value) {
        Set<Integer> result = new HashSet<>();
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid status code: " + trimmed);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("--status requires at least one code");
        }
        return result;
    }
}
