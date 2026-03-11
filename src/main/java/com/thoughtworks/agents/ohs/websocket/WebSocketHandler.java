package com.thoughtworks.agents.ohs.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushCCOutput(String sessionId, CCOutputMessage message) {
        messagingTemplate.convertAndSend("/topic/cc-sessions/" + sessionId + "/output", message);
    }

    public void pushDevTaskStatus(String taskId, DevTaskStatusMessage message) {
        messagingTemplate.convertAndSend("/topic/dev-tasks/" + taskId + "/status", message);
    }
}
