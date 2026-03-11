package com.thoughtworks.agents.application.github;

public class HandleOAuthCallbackCommand {

    private final String code;

    private HandleOAuthCallbackCommand(Builder builder) {
        this.code = builder.code;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public static class Builder {
        private String code;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public HandleOAuthCallbackCommand build() {
            return new HandleOAuthCallbackCommand(this);
        }
    }
}
