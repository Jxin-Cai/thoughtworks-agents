package com.thoughtworks.agents.application.devtask;

public class CreateDevTaskCommand {

    private final String conversationId;
    private final String repositoryFullName;
    private final String branchName;
    private final String requirement;

    private CreateDevTaskCommand(Builder builder) {
        this.conversationId = builder.conversationId;
        this.repositoryFullName = builder.repositoryFullName;
        this.branchName = builder.branchName;
        this.requirement = builder.requirement;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getRequirement() {
        return requirement;
    }

    public static class Builder {
        private String conversationId;
        private String repositoryFullName;
        private String branchName;
        private String requirement;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder repositoryFullName(String repositoryFullName) {
            this.repositoryFullName = repositoryFullName;
            return this;
        }

        public Builder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public Builder requirement(String requirement) {
            this.requirement = requirement;
            return this;
        }

        public CreateDevTaskCommand build() {
            return new CreateDevTaskCommand(this);
        }
    }
}
