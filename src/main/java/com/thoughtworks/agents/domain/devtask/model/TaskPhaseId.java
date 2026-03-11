package com.thoughtworks.agents.domain.devtask.model;

import java.util.Objects;
import java.util.UUID;

public class TaskPhaseId {

    private final String value;

    public TaskPhaseId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskPhaseId value must not be blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("TaskPhaseId value must be a valid UUID format: " + value);
        }
        this.value = value;
    }

    public static TaskPhaseId generate() {
        return new TaskPhaseId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskPhaseId that = (TaskPhaseId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
