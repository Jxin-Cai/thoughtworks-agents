package com.thoughtworks.agents.application.github;

public class MergeResultDTO {

    private final String repositoryFullName;
    private final String headBranch;
    private final String baseBranch;
    private final boolean success;

    private MergeResultDTO(String repositoryFullName, String headBranch, String baseBranch, boolean success) {
        this.repositoryFullName = repositoryFullName;
        this.headBranch = headBranch;
        this.baseBranch = baseBranch;
        this.success = success;
    }

    public static MergeResultDTO success(String repositoryFullName, String headBranch, String baseBranch) {
        return new MergeResultDTO(repositoryFullName, headBranch, baseBranch, true);
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

    public boolean isSuccess() {
        return success;
    }
}
