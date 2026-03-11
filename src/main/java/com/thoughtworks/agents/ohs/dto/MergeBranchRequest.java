package com.thoughtworks.agents.ohs.dto;

import jakarta.validation.constraints.NotBlank;

public class MergeBranchRequest {

    @NotBlank
    private String repositoryFullName;

    @NotBlank
    private String headBranch;

    @NotBlank
    private String baseBranch;

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public void setHeadBranch(String headBranch) {
        this.headBranch = headBranch;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }
}
