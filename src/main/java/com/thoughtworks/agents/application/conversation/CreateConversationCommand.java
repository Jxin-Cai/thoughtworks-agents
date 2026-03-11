package com.thoughtworks.agents.application.conversation;

public class CreateConversationCommand {

    private final String title;
    private final String repositoryFullName;

    private CreateConversationCommand(Builder builder) {
        this.title = builder.title;
        this.repositoryFullName = builder.repositoryFullName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public static class Builder {
        private String title;
        private String repositoryFullName;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder repositoryFullName(String repositoryFullName) {
            this.repositoryFullName = repositoryFullName;
            return this;
        }

        public CreateConversationCommand build() {
            return new CreateConversationCommand(this);
        }
    }
}
