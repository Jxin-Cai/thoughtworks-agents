package com.thoughtworks.agents.application.conversation;

import com.thoughtworks.agents.domain.conversation.model.Conversation;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDTO {

    private final String id;
    private final String title;
    private final String repositoryFullName;
    private final String status;
    private final List<MessageDTO> messages;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ConversationDTO(String id, String title, String repositoryFullName, String status,
                            List<MessageDTO> messages, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.repositoryFullName = repositoryFullName;
        this.status = status;
        this.messages = messages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConversationDTO from(Conversation conversation) {
        List<MessageDTO> messageDTOs = conversation.getMessages().stream()
                .map(MessageDTO::from)
                .toList();
        return new ConversationDTO(
                conversation.getId().getValue(),
                conversation.getTitle(),
                conversation.getRepositoryFullName(),
                conversation.getStatus().name(),
                messageDTOs,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public String getStatus() {
        return status;
    }

    public List<MessageDTO> getMessages() {
        return messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
