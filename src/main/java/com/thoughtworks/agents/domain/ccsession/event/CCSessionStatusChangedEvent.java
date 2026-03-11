package com.thoughtworks.agents.domain.ccsession.event;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionStatus;

import java.time.LocalDateTime;

public class CCSessionStatusChangedEvent {

    private final CCSessionId sessionId;
    private final CCSessionStatus previousStatus;
    private final CCSessionStatus currentStatus;
    private final LocalDateTime occurredAt;

    public CCSessionStatusChangedEvent(CCSessionId sessionId, CCSessionStatus previousStatus,
                                       CCSessionStatus currentStatus, LocalDateTime occurredAt) {
        this.sessionId = sessionId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
    }

    public CCSessionId getSessionId() {
        return sessionId;
    }

    public CCSessionStatus getPreviousStatus() {
        return previousStatus;
    }

    public CCSessionStatus getCurrentStatus() {
        return currentStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
