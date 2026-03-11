package com.thoughtworks.agents.domain.exception;

public class GitHubMergeConflictException extends RuntimeException {

    private final String repositoryFullName;
    private final String headBranch;
    private final String baseBranch;

    public GitHubMergeConflictException(String repositoryFullName, String headBranch, String baseBranch) {
        super("Merge conflict when merging " + headBranch + " into " + baseBranch
                + " in repository " + repositoryFullName);
        this.repositoryFullName = repositoryFullName;
        this.headBranch = headBranch;
        this.baseBranch = baseBranch;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public String getBaseBranch() {
        return baseBranch;
    }
}
