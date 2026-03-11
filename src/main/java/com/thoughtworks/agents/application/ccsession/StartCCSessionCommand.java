package com.thoughtworks.agents.application.ccsession;

public class StartCCSessionCommand {

    private final String sessionId;

    private StartCCSessionCommand(Builder builder) {
        this.sessionId = builder.sessionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public static class Builder {
        private String sessionId;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public StartCCSessionCommand build() {
            return new StartCCSessionCommand(this);
        }
    }
}
