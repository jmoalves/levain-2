package com.github.jmoalves.levain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    @Test
    void shouldDeriveRepoName() {
        assertEquals("project", GitUrlUtils.deriveRepoName("git@github.com:example/project.git"));
        assertEquals("project", GitUrlUtils.deriveRepoName("https://github.com/example/project.git#main"));
        assertEquals("repo", GitUrlUtils.deriveRepoName(""));
        assertEquals("project", GitUrlUtils.deriveRepoName("file:///tmp/project.git"));
        assertTrue(GitUrlUtils.isGitUrl("git@github.com:example/project.git"));
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
}
