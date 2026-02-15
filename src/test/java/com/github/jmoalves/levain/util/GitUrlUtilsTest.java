package com.github.jmoalves.levain.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitUrlUtilsTest {

    @Test
    void shouldParseGithubGitUrlWithBranch() {
        GitUrlUtils.ParsedGitUrl parsed = GitUrlUtils.parse("https://github.com/example/project.git#dev");

        assertNotNull(parsed);
        assertEquals("https://github.com/example/project.git", parsed.getUrl());
        assertEquals("dev", parsed.getBranch());
        assertEquals("example", parsed.getUser());
        assertEquals("project", parsed.getRepo());
    }

    @Test
    void shouldRejectNonGitUrls() {
        assertNull(GitUrlUtils.parse("https://github.com/example/project"));
        assertNull(GitUrlUtils.parse(""));
        assertNull(GitUrlUtils.parse(null));
        assertNull(GitUrlUtils.parse("   "));
    }

    @Test
    void shouldDeriveRepoName() {
        assertEquals("project", GitUrlUtils.deriveRepoName("git@github.com:example/project.git"));
        assertEquals("project", GitUrlUtils.deriveRepoName("https://github.com/example/project.git#main"));
        assertEquals("repo", GitUrlUtils.deriveRepoName(""));
        assertEquals("repo", GitUrlUtils.deriveRepoName(null));
        assertEquals("project", GitUrlUtils.deriveRepoName("project"));
        assertEquals("project", GitUrlUtils.deriveRepoName("project.git"));
        assertEquals("repo", GitUrlUtils.deriveRepoName("https://github.com/example/.git"));
        assertEquals("git@github.com:", GitUrlUtils.deriveRepoName("git@github.com:"));
        assertEquals("project", GitUrlUtils.deriveRepoName("file:///tmp/project.git"));
        assertTrue(GitUrlUtils.isGitUrl("git@github.com:example/project.git"));
        assertFalse(GitUrlUtils.isGitUrl("https://github.com/example/project"));
    }

    @Test
    void shouldParseGitUrlWithoutGithubUser() {
        GitUrlUtils.ParsedGitUrl parsed = GitUrlUtils.parse("file:///tmp/project.git");

        assertNotNull(parsed);
        assertEquals("file:///tmp/project.git", parsed.getUrl());
        assertNull(parsed.getBranch());
        assertNull(parsed.getUser());
        assertNull(parsed.getRepo());
    }

    @Test
    void shouldDeriveRepoPathWithFallbacks() {
        assertEquals(Path.of("fallback"),
                GitUrlUtils.deriveRepoPath(null, "file:///tmp/project.git", "fallback"));
        assertEquals(Path.of("project"),
                GitUrlUtils.deriveRepoPath(null, "file:///tmp/project.git", ""));
    }
}
