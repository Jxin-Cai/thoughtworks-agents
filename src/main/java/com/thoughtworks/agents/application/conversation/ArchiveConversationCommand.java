package com.thoughtworks.agents.application.conversation;

public class ArchiveConversationCommand {

    private final String conversationId;

    private ArchiveConversationCommand(Builder builder) {
        this.conversationId = builder.conversationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() {
        return conversationId;
    }

    public static class Builder {
        private String conversationId;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ArchiveConversationCommand build() {
            return new ArchiveConversationCommand(this);
        }
    }
}
