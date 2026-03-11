package com.thoughtworks.agents.domain.github.model;

import java.util.Objects;

public class Repository {

    private final String fullName;
    private final String defaultBranch;
    private final String cloneUrl;
    private final boolean isPrivate;

    public Repository(String fullName, String defaultBranch, String cloneUrl, boolean isPrivate) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Repository fullName must not be blank");
        }
        if (!fullName.contains("/")) {
            throw new IllegalArgumentException("Repository fullName must contain '/': " + fullName);
        }
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.cloneUrl = cloneUrl;
        this.isPrivate = isPrivate;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return isPrivate == that.isPrivate
                && Objects.equals(fullName, that.fullName)
                && Objects.equals(defaultBranch, that.defaultBranch)
                && Objects.equals(cloneUrl, that.cloneUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, defaultBranch, cloneUrl, isPrivate);
    }
}
