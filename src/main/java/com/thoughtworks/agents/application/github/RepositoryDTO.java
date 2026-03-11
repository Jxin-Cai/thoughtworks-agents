package com.thoughtworks.agents.application.github;

import com.thoughtworks.agents.domain.github.model.Repository;

public class RepositoryDTO {

    private final String fullName;
    private final String defaultBranch;
    private final String cloneUrl;
    private final boolean isPrivate;

    private RepositoryDTO(String fullName, String defaultBranch, String cloneUrl, boolean isPrivate) {
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.cloneUrl = cloneUrl;
        this.isPrivate = isPrivate;
    }

    public static RepositoryDTO from(Repository repository) {
        return new RepositoryDTO(
                repository.getFullName(),
                repository.getDefaultBranch(),
                repository.getCloneUrl(),
                repository.isPrivate()
        );
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
}
