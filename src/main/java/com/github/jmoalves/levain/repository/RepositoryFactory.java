package com.github.jmoalves.levain.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory for creating appropriate Repository instances based on the URI.
 * 
 * Automatically detects the repository type:
 * - git:// or https://...git -> GitRepository
 * - .zip -> ZipRepository
 * - http:// or https:// -> RemoteRepository
 * - local path -> DirectoryRepository
 */
@ApplicationScoped
public class RepositoryFactory {
    private static final Logger logger = LogManager.getLogger(RepositoryFactory.class);

    /**
     * Create a repository instance based on the URI.
     * 
     * @param uri the repository URI (local path, git URL, zip file, http URL, etc)
     * @return a Repository instance
     */
    public Repository createRepository(String uri) {
        if (uri == null || uri.isEmpty()) {
            logger.debug("Repository URI is null or empty");
            return null;
        }

        // Detect and create appropriate repository type
        if (isGitUrl(uri)) {
            logger.debug("Creating GitRepository for: {}", uri);
            return new GitRepository(uri);
        } else if (isZipFile(uri)) {
            logger.debug("Creating ZipRepository for: {}", uri);
            return new ZipRepository(uri);
        } else if (isRemoteUrl(uri)) {
            logger.debug("Creating RemoteRepository for: {}", uri);
            return new RemoteRepository(uri);
        } else {
            logger.debug("Creating DirectoryRepository for: {}", uri);
            return new DirectoryRepository("DirectoryRepository", uri);
        }
    }

    /**
     * Check if the URI is a Git repository URL.
     */
    private boolean isGitUrl(String uri) {
        return uri.startsWith("git://") ||
                uri.startsWith("git@") ||
                uri.endsWith(".git") ||
                (uri.contains("github.com") && !uri.endsWith(".zip"));
    }

    /**
     * Check if the URI is a ZIP file.
     */
    private boolean isZipFile(String uri) {
        return uri.endsWith(".zip");
    }

    /**
     * Check if the URI is a remote HTTP/HTTPS URL.
     */
    private boolean isRemoteUrl(String uri) {
        return uri.startsWith("http://") || uri.startsWith("https://");
    }
}
