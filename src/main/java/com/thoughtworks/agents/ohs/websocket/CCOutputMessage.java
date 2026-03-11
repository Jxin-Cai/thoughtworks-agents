package com.thoughtworks.agents.ohs.websocket;

import java.time.LocalDateTime;

public class CCOutputMessage {

    private String sessionId;
    private String content;
    private LocalDateTime timestamp;
    private String type;

    public CCOutputMessage() {
    }

    public CCOutputMessage(String sessionId, String content, LocalDateTime timestamp, String type) {
        this.sessionId = sessionId;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
