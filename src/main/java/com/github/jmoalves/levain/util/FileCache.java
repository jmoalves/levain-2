package com.github.jmoalves.levain.util;

import com.github.jmoalves.levain.config.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@ApplicationScoped
public class FileCache {
    private static final Logger logger = LoggerFactory.getLogger(FileCache.class);

    private final Config config;
    private final HttpClient httpClient;

    @Inject
    public FileCache(Config config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().build();
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(src))
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
