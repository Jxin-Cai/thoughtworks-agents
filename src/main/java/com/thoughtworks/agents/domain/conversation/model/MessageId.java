package com.thoughtworks.agents.domain.conversation.model;

import java.util.Objects;
import java.util.UUID;

public class MessageId {

    private final String value;

    public MessageId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MessageId value must not be blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MessageId value must be a valid UUID format: " + value);
        }
        this.value = value;
    }

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageId that = (MessageId) o;
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
