package com.thoughtworks.agents.ohs.websocket;

import java.time.LocalDateTime;

public class DevTaskStatusMessage {

    private String taskId;
    private String previousStatus;
    private String currentStatus;
    private LocalDateTime timestamp;

    public DevTaskStatusMessage() {
    }

    public DevTaskStatusMessage(String taskId, String previousStatus, String currentStatus, LocalDateTime timestamp) {
        this.taskId = taskId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.timestamp = timestamp;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
