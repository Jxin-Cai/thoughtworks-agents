package com.thoughtworks.agents.infr.event;

import com.thoughtworks.agents.domain.ccsession.event.CCSessionEventPublisher;
import com.thoughtworks.agents.domain.ccsession.event.CCSessionStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CCSessionEventPublisherImpl implements CCSessionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public CCSessionEventPublisherImpl(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishStatusChanged(CCSessionStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(
                new CCSessionStatusChangedApplicationEvent(this, event));
    }
}
