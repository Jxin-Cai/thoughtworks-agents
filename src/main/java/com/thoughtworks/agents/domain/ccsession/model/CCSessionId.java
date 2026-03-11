package com.thoughtworks.agents.domain.ccsession.model;

import java.util.Objects;
import java.util.UUID;

public class CCSessionId {

    private final String value;

    public CCSessionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CCSessionId value must not be blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("CCSessionId value must be a valid UUID format: " + value);
        }
        this.value = value;
    }

    public static CCSessionId generate() {
        return new CCSessionId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CCSessionId that = (CCSessionId) o;
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
