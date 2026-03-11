package com.thoughtworks.agents.application.conversation;

import com.thoughtworks.agents.domain.conversation.model.Message;

import java.time.LocalDateTime;

public class MessageDTO {

    private final String id;
    private final String role;
    private final String content;
    private final LocalDateTime createdAt;

    private MessageDTO(String id, String role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static MessageDTO from(Message message) {
        return new MessageDTO(
                message.getId().getValue(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
