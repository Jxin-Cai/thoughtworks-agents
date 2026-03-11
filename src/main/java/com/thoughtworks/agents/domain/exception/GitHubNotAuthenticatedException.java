package com.thoughtworks.agents.domain.exception;

public class GitHubNotAuthenticatedException extends RuntimeException {

    public GitHubNotAuthenticatedException() {
        super("GitHub is not authenticated. Please complete OAuth authentication first.");
    }
}
