package com.github.jmoalves.levain.util;

import com.github.jmoalves.levain.config.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@ApplicationScoped
public class FileCache {
    private static final Logger logger = LoggerFactory.getLogger(FileCache.class);

    private final Config config;
    private final HttpClient httpClient;

    @Inject
    public FileCache(Config config) {
        this.config = config;
        this.httpClient = buildHttpClient();
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .version(HttpClient.Version.HTTP_1_1);

        ProxySelector proxySelector = createProxySelector();
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }

        return builder.build();
    }

    private ProxySelector createProxySelector() {
        String proxyEnv = firstNonBlank(
                System.getenv("HTTPS_PROXY"),
                System.getenv("https_proxy"),
                System.getenv("HTTP_PROXY"),
                System.getenv("http_proxy"));

        if (proxyEnv == null || proxyEnv.isBlank()) {
            return null;
        }

        String proxyUrl = proxyEnv.contains("://") ? proxyEnv : "http://" + proxyEnv;
        URI proxyUri;
        try {
            proxyUri = URI.create(proxyUrl);
        } catch (Exception e) {
            logger.warn("Invalid proxy URL '{}': {}", proxyEnv, e.getMessage());
            return null;
        }

        String host = proxyUri.getHost();
        int port = proxyUri.getPort();
        if (host == null || port <= 0) {
            logger.warn("Proxy URL missing host/port: {}", proxyEnv);
            return null;
        }

        List<String> noProxy = parseNoProxyList(System.getenv("NO_PROXY"), System.getenv("no_proxy"));
        InetSocketAddress address = new InetSocketAddress(host, port);

        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if (uri == null || uri.getHost() == null) {
                    return List.of(Proxy.NO_PROXY);
                }
                if (isNoProxy(uri.getHost(), noProxy)) {
                    return List.of(Proxy.NO_PROXY);
                }
                return List.of(new Proxy(Proxy.Type.HTTP, address));
            }

            @Override
            public void connectFailed(URI uri, java.net.SocketAddress sa, IOException ioe) {
                logger.warn("Proxy connect failed for {}: {}", uri, ioe.getMessage());
            }
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static List<String> parseNoProxyList(String... values) {
        String raw = firstNonBlank(values);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static boolean isNoProxy(String host, List<String> noProxy) {
        if (noProxy == null || noProxy.isEmpty()) {
            return false;
        }
        String lowerHost = host.toLowerCase();
        for (String entry : noProxy) {
            String rule = entry.toLowerCase();
            if (rule.equals("*")) {
                return true;
            }
            String ruleHost = rule;
            int colonIndex = ruleHost.indexOf(':');
            if (colonIndex > -1) {
                ruleHost = ruleHost.substring(0, colonIndex);
            }
            if (ruleHost.startsWith(".")) {
                if (lowerHost.endsWith(ruleHost)) {
                    return true;
                }
            } else if (lowerHost.equals(ruleHost) || lowerHost.endsWith("." + ruleHost)) {
                return true;
            }
        }
        return false;
    }

    public Path get(String src) throws IOException, InterruptedException {
        if (FileUtils.isFileSystemUrl(src)) {
            Path path = Path.of(src).toAbsolutePath().normalize();
            FileUtils.throwIfNotExists(path);
            return path;
        }

        Path cacheDir = config.getCacheDir().resolve("downloads");
        Files.createDirectories(cacheDir);

        String filename = FileUtils.getFileNameFromUrl(src);
        String prefix = hash(src).substring(0, 12);
        Path cachedFile = cacheDir.resolve(prefix + "-" + filename);

        if (Files.exists(cachedFile) && cacheIsValid(src, cachedFile)) {
            logger.debug("Using cached file: {}", cachedFile);
            return cachedFile;
        }

        logger.debug("Downloading {} -> {}", src, cachedFile);
        downloadTo(src, cachedFile);
        return cachedFile;
    }

    private boolean cacheIsValid(String src, Path cachedFile) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(src))
                    .timeout(Duration.ofSeconds(30))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                return true;
            }

            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            String lastModified = response.headers().firstValue("Last-Modified").orElse(null);

            if (contentLength > 0 && Files.size(cachedFile) != contentLength) {
                return false;
            }

            if (lastModified != null) {
                Instant remoteTime = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                        .parse(lastModified, Instant::from);
                FileTime localTime = Files.getLastModifiedTime(cachedFile);
                return !remoteTime.isAfter(localTime.toInstant());
            }

            return true;
        } catch (Exception e) {
            logger.debug("Cache validation failed, using cached file: {}", e.getMessage());
            return true;
        }
    }

    private void downloadTo(String src, Path cachedFile) throws IOException, InterruptedException {
        try {
            downloadUsingHttpClient(src, cachedFile);
        } catch (java.net.http.HttpConnectTimeoutException e) {
            // Fallback to curl if HttpClient times out (known issue with Java 25 and some servers)
            logger.debug("HttpClient timeout, falling back to curl: {}", e.getMessage());
            downloadUsingCurl(src, cachedFile);
        }
    }

    private void downloadUsingHttpClient(String src, Path cachedFile) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(src))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to download " + src + ": HTTP " + response.statusCode());
        }

        Path tempFile = cachedFile.resolveSibling(cachedFile.getFileName() + ".tmp");
        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tempFile, cachedFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void downloadUsingCurl(String src, Path cachedFile) throws IOException {
        Path tempFile = cachedFile.resolveSibling(cachedFile.getFileName() + ".tmp");
        
        ProcessBuilder pb = new ProcessBuilder("curl", "-f", "-L", "-o", tempFile.toString(), src);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        int exitCode;
        try {
            // Allow curl up to 10 minutes for very large files
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)) {
                process.destroy();
                throw new IOException("curl download timed out after 10 minutes");
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            process.destroy();
            throw new IOException("curl download interrupted", e);
        }
        
        if (exitCode != 0) {
            throw new IOException("curl failed with exit code " + exitCode + " downloading " + src);
        }
        
        if (!Files.exists(tempFile)) {
            throw new IOException("curl completed but file not created: " + tempFile);
        }
        
        Files.move(tempFile, cachedFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
