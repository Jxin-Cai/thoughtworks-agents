package com.thoughtworks.agents.domain.devtask.event;

import com.thoughtworks.agents.domain.devtask.model.DevTaskId;
import com.thoughtworks.agents.domain.devtask.model.DevTaskStatus;

import java.time.LocalDateTime;

public class DevTaskStatusChangedEvent {

    private final DevTaskId taskId;
    private final DevTaskStatus previousStatus;
    private final DevTaskStatus currentStatus;
    private final LocalDateTime occurredAt;

    public DevTaskStatusChangedEvent(DevTaskId taskId, DevTaskStatus previousStatus,
                                     DevTaskStatus currentStatus, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
    }

    public DevTaskId getTaskId() {
        return taskId;
    }

    public DevTaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public DevTaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
