package com.github.jmoalves.levain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryFactoryTest {
    private RepositoryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RepositoryFactory();
    }

    @Test
    void shouldCreateDirectoryRepositoryForLocalPath() {
        Repository repo = factory.createRepository("./recipes");
        assertNotNull(repo);
        assertTrue(repo instanceof DirectoryRepository);
    }

    @Test
    void shouldCreateGitRepositoryForGitUrl() {
        Repository repo = factory.createRepository("https://github.com/user/repo.git");
        assertNotNull(repo);
        assertTrue(repo instanceof GitRepository);
    }

    @Test
    void shouldCreateGitRepositoryForGitScheme() {
        Repository repo = factory.createRepository("git://github.com/user/repo");
        assertNotNull(repo);
        assertTrue(repo instanceof GitRepository);
    }

    @Test
    void shouldCreateGitRepositoryForGitAtUrl() {
        Repository repo = factory.createRepository("git@github.com:user/repo.git");
        assertNotNull(repo);
        assertTrue(repo instanceof GitRepository);
    }

    @Test
    void shouldCreateZipRepositoryForZipFile() {
        Repository repo = factory.createRepository("https://example.com/archive.zip");
        assertNotNull(repo);
        assertTrue(repo instanceof ZipRepository);
    }

    @Test
    void shouldCreateRemoteRepositoryForHttpUrl() {
        Repository repo = factory.createRepository("https://example.com/recipes/");
        assertNotNull(repo);
        assertTrue(repo instanceof RemoteRepository);
    }

    @Test
    void shouldCreateGitRepositoryForGithubUrl() {
        Repository repo = factory.createRepository("https://github.com/user/repo/raw/main/recipes");
        assertNotNull(repo);
        assertTrue(repo instanceof GitRepository);
    }

    @Test
    void shouldReturnNullForNullUri() {
        Repository repo = factory.createRepository(null);
        assertNull(repo);
    }

    @Test
    void shouldReturnNullForEmptyUri() {
        Repository repo = factory.createRepository("");
        assertNull(repo);
    }
}
