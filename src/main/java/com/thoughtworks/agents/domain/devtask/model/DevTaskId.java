package com.thoughtworks.agents.domain.devtask.model;

import java.util.Objects;
import java.util.UUID;

public class DevTaskId {

    private final String value;

    public DevTaskId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DevTaskId value must not be blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("DevTaskId value must be a valid UUID format: " + value);
        }
        this.value = value;
    }

    public static DevTaskId generate() {
        return new DevTaskId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DevTaskId that = (DevTaskId) o;
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
