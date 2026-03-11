package com.thoughtworks.agents.domain.conversation.model;

import java.util.Objects;
import java.util.UUID;

public class ConversationId {

    private final String value;

    public ConversationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ConversationId value must not be blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ConversationId value must be a valid UUID format: " + value);
        }
        this.value = value;
    }

    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationId that = (ConversationId) o;
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
