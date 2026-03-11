package com.thoughtworks.agents.domain.github.model;

import java.util.Objects;

public class GitHubIntegrationId {

    private final String value;

    public GitHubIntegrationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitHubIntegrationId value must not be blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubIntegrationId that = (GitHubIntegrationId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
