package com.thoughtworks.agents.application.devtask;

public class ExecutePublishCommand {

    private final String taskId;
    private final String baseBranch;

    private ExecutePublishCommand(Builder builder) {
        this.taskId = builder.taskId;
        this.baseBranch = builder.baseBranch;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public static class Builder {
        private String taskId;
        private String baseBranch;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder baseBranch(String baseBranch) {
            this.baseBranch = baseBranch;
            return this;
        }

        public ExecutePublishCommand build() {
            return new ExecutePublishCommand(this);
        }
    }
}
