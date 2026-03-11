package com.thoughtworks.agents.infr.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketMessageBroker {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageBroker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendCCSessionOutput(String sessionId, String outputLine) {
        messagingTemplate.convertAndSend("/topic/cc-session/" + sessionId + "/output", outputLine);
    }

    public void sendDevTaskStatusUpdate(String taskId, String status) {
        messagingTemplate.convertAndSend("/topic/dev-task/" + taskId + "/status", status);
    }

    public void sendConversationMessage(String conversationId, Object message) {
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/message", message);
    }
}
