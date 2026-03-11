package com.thoughtworks.agents.domain.exception;

import com.thoughtworks.agents.domain.conversation.model.ConversationStatus;

public class IllegalConversationStateException extends RuntimeException {

    private final ConversationStatus currentStatus;
    private final String targetDescription;

    public IllegalConversationStateException(ConversationStatus currentStatus, ConversationStatus targetStatus) {
        super("Cannot transition Conversation from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetDescription = targetStatus.name();
    }

    public IllegalConversationStateException(ConversationStatus currentStatus, String operation) {
        super("Cannot perform '" + operation + "' on Conversation in status " + currentStatus);
        this.currentStatus = currentStatus;
        this.targetDescription = operation;
    }

    public ConversationStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetDescription() {
        return targetDescription;
    }
}
