package com.thoughtworks.agents.domain.github.model;

import com.thoughtworks.agents.domain.exception.GitHubNotAuthenticatedException;

import java.time.LocalDateTime;
import java.util.UUID;

public class GitHubIntegration {

    private final GitHubIntegrationId id;
    private OAuthToken oauthToken;
    private String authenticatedUser;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private GitHubIntegration(GitHubIntegrationId id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static GitHubIntegration create() {
        LocalDateTime now = LocalDateTime.now();
        return new GitHubIntegration(
                new GitHubIntegrationId(UUID.randomUUID().toString()),
                now,
                now
        );
    }

    public static GitHubIntegration reconstitute(GitHubIntegrationId id, OAuthToken oauthToken,
                                                  String authenticatedUser,
                                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        GitHubIntegration integration = new GitHubIntegration(id, createdAt, updatedAt);
        integration.oauthToken = oauthToken;
        integration.authenticatedUser = authenticatedUser;
        return integration;
    }

    public void authenticate(OAuthToken token, String username) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        this.oauthToken = token;
        this.authenticatedUser = username;
        this.updatedAt = LocalDateTime.now();
    }

    public void revokeAuthentication() {
        this.oauthToken = null;
        this.authenticatedUser = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAuthenticated() {
        return oauthToken != null;
    }

    public OAuthToken getRequiredToken() {
        if (!isAuthenticated()) {
            throw new GitHubNotAuthenticatedException();
        }
        return oauthToken;
    }

    public GitHubIntegrationId getId() {
        return id;
    }

    public OAuthToken getOauthToken() {
        return oauthToken;
    }

    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
