package com.thoughtworks.agents.application.ccsession;

public class TerminateCCSessionCommand {

    private final String sessionId;

    private TerminateCCSessionCommand(Builder builder) {
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

        public TerminateCCSessionCommand build() {
            return new TerminateCCSessionCommand(this);
        }
    }
}
