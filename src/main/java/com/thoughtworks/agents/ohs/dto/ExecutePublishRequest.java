package com.thoughtworks.agents.ohs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExecutePublishRequest {

    @NotBlank
    @Size(max = 200)
    private String baseBranch;

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }
}
