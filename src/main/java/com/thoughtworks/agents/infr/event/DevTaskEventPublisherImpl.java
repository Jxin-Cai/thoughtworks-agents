package com.thoughtworks.agents.infr.event;

import com.thoughtworks.agents.domain.devtask.event.DevTaskEventPublisher;
import com.thoughtworks.agents.domain.devtask.event.DevTaskStatusChangedEvent;
import com.thoughtworks.agents.infr.websocket.WebSocketMessageBroker;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DevTaskEventPublisherImpl implements DevTaskEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final WebSocketMessageBroker webSocketMessageBroker;

    public DevTaskEventPublisherImpl(ApplicationEventPublisher applicationEventPublisher,
                                     WebSocketMessageBroker webSocketMessageBroker) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.webSocketMessageBroker = webSocketMessageBroker;
    }

    @Override
    public void publishStatusChanged(DevTaskStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(
                new DevTaskStatusChangedApplicationEvent(this, event));
        webSocketMessageBroker.sendDevTaskStatusUpdate(
                event.getTaskId().getValue(),
                event.getCurrentStatus().name());
    }
}
