package com.thoughtworks.agents.domain.github.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class OAuthToken {

    private final String accessToken;
    private final String tokenType;
    private final String scope;
    private final LocalDateTime createdAt;

    public OAuthToken(String accessToken, String tokenType, String scope, LocalDateTime createdAt) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.createdAt = createdAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuthToken that = (OAuthToken) o;
        return Objects.equals(accessToken, that.accessToken)
                && Objects.equals(tokenType, that.tokenType)
                && Objects.equals(scope, that.scope)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, tokenType, scope, createdAt);
    }
}
