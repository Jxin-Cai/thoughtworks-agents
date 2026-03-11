package com.thoughtworks.agents.application.ccsession;

import java.util.Map;

public class CreateCCSessionCommand {

    private final String command;
    private final String workingDirectory;
    private final Map<String, String> environmentVariables;

    private CreateCCSessionCommand(Builder builder) {
        this.command = builder.command;
        this.workingDirectory = builder.workingDirectory;
        this.environmentVariables = builder.environmentVariables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public static class Builder {
        private String command;
        private String workingDirectory;
        private Map<String, String> environmentVariables;

        public Builder command(String command) {
            this.command = command;
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

        public CreateCCSessionCommand build() {
            return new CreateCCSessionCommand(this);
        }
    }
}
