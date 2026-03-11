package com.thoughtworks.agents.domain.ccsession.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProcessConfig {

    private final String command;
    private final String workingDirectory;
    private final Map<String, String> environmentVariables;

    public ProcessConfig(String command, String workingDirectory, Map<String, String> environmentVariables) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        if (workingDirectory == null || workingDirectory.isBlank()) {
            throw new IllegalArgumentException("workingDirectory must not be blank");
        }
        if (environmentVariables == null) {
            throw new IllegalArgumentException("environmentVariables must not be null");
        }
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.environmentVariables = Collections.unmodifiableMap(new HashMap<>(environmentVariables));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessConfig that = (ProcessConfig) o;
        return Objects.equals(command, that.command)
                && Objects.equals(workingDirectory, that.workingDirectory)
                && Objects.equals(environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, workingDirectory, environmentVariables);
    }
}
