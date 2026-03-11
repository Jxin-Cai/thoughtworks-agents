package com.thoughtworks.agents.application.conversation;

import java.util.Map;

public class SendMessageCommand {

    private final String conversationId;
    private final String content;
    private final String workingDirectory;
    private final Map<String, String> environmentVariables;

    private SendMessageCommand(Builder builder) {
        this.conversationId = builder.conversationId;
        this.content = builder.content;
        this.workingDirectory = builder.workingDirectory;
        this.environmentVariables = builder.environmentVariables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public static class Builder {
        private String conversationId;
        private String content;
        private String workingDirectory;
        private Map<String, String> environmentVariables;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public SendMessageCommand build() {
            return new SendMessageCommand(this);
        }
    }
}
