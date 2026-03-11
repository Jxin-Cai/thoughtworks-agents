package com.thoughtworks.agents.infr.event;

import com.thoughtworks.agents.domain.ccsession.event.CCSessionStatusChangedEvent;
import org.springframework.context.ApplicationEvent;

public class CCSessionStatusChangedApplicationEvent extends ApplicationEvent {

    private final CCSessionStatusChangedEvent domainEvent;

    public CCSessionStatusChangedApplicationEvent(Object source, CCSessionStatusChangedEvent domainEvent) {
        super(source);
        this.domainEvent = domainEvent;
    }

    public CCSessionStatusChangedEvent getDomainEvent() {
        return domainEvent;
    }
}
