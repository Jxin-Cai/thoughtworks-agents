package com.thoughtworks.agents.domain.conversation.model;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.exception.IllegalConversationStateException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Conversation {

    private final ConversationId id;
    private final String title;
    private final String repositoryFullName;
    private CCSessionId ccSessionId;
    private ConversationStatus status;
    private final List<Message> messages;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Conversation(ConversationId id, String title, String repositoryFullName,
                         ConversationStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.repositoryFullName = repositoryFullName;
        this.status = status;
        this.messages = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Conversation create(String title, String repositoryFullName) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Conversation title must not be blank");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Conversation(
                ConversationId.generate(),
                title,
                repositoryFullName,
                ConversationStatus.CREATED,
                now,
                now
        );
    }

    public static Conversation reconstitute(ConversationId id, String title, String repositoryFullName,
                                            CCSessionId ccSessionId, ConversationStatus status,
                                            List<Message> messages, LocalDateTime createdAt,
                                            LocalDateTime updatedAt) {
        Conversation conversation = new Conversation(id, title, repositoryFullName, status, createdAt, updatedAt);
        conversation.ccSessionId = ccSessionId;
        if (messages != null) {
            conversation.messages.addAll(messages);
        }
        return conversation;
    }

    public void activate(CCSessionId ccSessionId) {
        if (!status.canTransitionTo(ConversationStatus.ACTIVE)) {
            throw new IllegalConversationStateException(status, ConversationStatus.ACTIVE);
        }
        if (ccSessionId == null) {
            throw new IllegalArgumentException("ccSessionId must not be null");
        }
        this.ccSessionId = ccSessionId;
        this.status = ConversationStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public Message addUserMessage(String content) {
        if (status != ConversationStatus.ACTIVE) {
            throw new IllegalConversationStateException(status, "addUserMessage");
        }
        Message message = new Message(MessageId.generate(), MessageRole.USER, content, LocalDateTime.now());
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
        return message;
    }

    public Message addAssistantMessage(String content) {
        if (status != ConversationStatus.ACTIVE) {
            throw new IllegalConversationStateException(status, "addAssistantMessage");
        }
        Message message = new Message(MessageId.generate(), MessageRole.ASSISTANT, content, LocalDateTime.now());
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
        return message;
    }

    public void complete() {
        if (!status.canTransitionTo(ConversationStatus.COMPLETED)) {
            throw new IllegalConversationStateException(status, ConversationStatus.COMPLETED);
        }
        this.status = ConversationStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        if (!status.canTransitionTo(ConversationStatus.ARCHIVED)) {
            throw new IllegalConversationStateException(status, ConversationStatus.ARCHIVED);
        }
        this.status = ConversationStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    public ConversationId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public CCSessionId getCcSessionId() {
        return ccSessionId;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
