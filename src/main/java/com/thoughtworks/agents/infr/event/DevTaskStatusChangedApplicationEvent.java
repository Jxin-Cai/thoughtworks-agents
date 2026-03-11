package com.thoughtworks.agents.infr.event;

import com.thoughtworks.agents.domain.devtask.event.DevTaskStatusChangedEvent;
import org.springframework.context.ApplicationEvent;

public class DevTaskStatusChangedApplicationEvent extends ApplicationEvent {

    private final DevTaskStatusChangedEvent domainEvent;

    public DevTaskStatusChangedApplicationEvent(Object source, DevTaskStatusChangedEvent domainEvent) {
        super(source);
        this.domainEvent = domainEvent;
    }

    public DevTaskStatusChangedEvent getDomainEvent() {
        return domainEvent;
    }
}
