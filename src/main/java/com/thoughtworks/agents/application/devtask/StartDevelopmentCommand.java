package com.thoughtworks.agents.application.devtask;

import java.util.Map;

public class StartDevelopmentCommand {

    private final String taskId;
    private final String workingDirectory;
    private final Map<String, String> environmentVariables;

    private StartDevelopmentCommand(Builder builder) {
        this.taskId = builder.taskId;
        this.workingDirectory = builder.workingDirectory;
        this.environmentVariables = builder.environmentVariables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public static class Builder {
        private String taskId;
        private String workingDirectory;
        private Map<String, String> environmentVariables;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
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

        public StartDevelopmentCommand build() {
            return new StartDevelopmentCommand(this);
        }
    }
}
