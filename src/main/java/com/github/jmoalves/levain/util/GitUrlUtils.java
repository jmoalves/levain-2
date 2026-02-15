package com.github.jmoalves.levain.util;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitUrlUtils {
    private static final Pattern GIT_URL_PATTERN = Pattern.compile("(?<url>.*\\.git)(?:#(?<branch>.+))?$");
    private static final Pattern GITHUB_PATTERN = Pattern.compile("^(git@.*:|https://.*/)(?<user>[^/]+)/(?<repo>[^.]+)\\.git$");

    private GitUrlUtils() {
    }

    public static boolean isGitUrl(String url) {
        return parse(url) != null;
    }

    public static ParsedGitUrl parse(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) {
            return null;
        }

        Matcher matcher = GIT_URL_PATTERN.matcher(gitUrl);
        if (!matcher.matches()) {
            return null;
        }

        String url = matcher.group("url");
        String branch = matcher.group("branch");
        String user = null;
        String repo = null;

        if (url != null) {
            Matcher github = GITHUB_PATTERN.matcher(url);
            if (github.matches()) {
                user = github.group("user");
                repo = github.group("repo");
            }
        }

        return new ParsedGitUrl(url, branch, user, repo);
    }

    public static String deriveRepoName(String url) {
        if (url == null || url.isBlank()) {
            return "repo";
        }

        String base = url;
        int hashIndex = base.indexOf('#');
        if (hashIndex >= 0) {
            base = base.substring(0, hashIndex);
        }

        int slashIndex = Math.max(base.lastIndexOf('/'), base.lastIndexOf(':'));
        if (slashIndex >= 0 && slashIndex + 1 < base.length()) {
            base = base.substring(slashIndex + 1);
        }

        if (base.endsWith(".git")) {
            base = base.substring(0, base.length() - 4);
        }

        if (base.isBlank()) {
            return "repo";
        }

        return base;
    }

    public static Path deriveRepoPath(Path parentDir, String url, String repoNameFallback) {
        String repoName = repoNameFallback != null && !repoNameFallback.isBlank()
                ? repoNameFallback
                : deriveRepoName(url);
        if (parentDir == null) {
            return Path.of(repoName);
        }
        return parentDir.resolve(repoName);
    }

    public static final class ParsedGitUrl {
        private final String url;
        private final String branch;
        private final String user;
        private final String repo;

        private ParsedGitUrl(String url, String branch, String user, String repo) {
            this.url = url;
            this.branch = branch;
            this.user = user;
            this.repo = repo;
        }

        public String getUrl() {
            return url;
        }

        public String getBranch() {
            return branch;
        }

        public String getUser() {
            return user;
        }

        public String getRepo() {
            return repo;
        }
    }
}
