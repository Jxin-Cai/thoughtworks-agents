package com.thoughtworks.agents.domain.conversation.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ConversationStatus {

    CREATED,
    ACTIVE,
    COMPLETED,
    ARCHIVED;

    private static final Map<ConversationStatus, Set<ConversationStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, EnumSet.of(ACTIVE),
            ACTIVE, EnumSet.of(COMPLETED, ARCHIVED),
            COMPLETED, EnumSet.of(ARCHIVED),
            ARCHIVED, EnumSet.noneOf(ConversationStatus.class)
    );

    public boolean canTransitionTo(ConversationStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(ConversationStatus.class)).contains(target);
    }
}
