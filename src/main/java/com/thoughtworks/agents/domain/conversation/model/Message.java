package com.thoughtworks.agents.domain.conversation.model;

import java.time.LocalDateTime;

public class Message {

    private final MessageId id;
    private final MessageRole role;
    private final String content;
    private final LocalDateTime createdAt;

    public Message(MessageId id, MessageRole role, String content, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("Message id must not be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Message role must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Message createdAt must not be null");
        }
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public MessageId getId() {
        return id;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
