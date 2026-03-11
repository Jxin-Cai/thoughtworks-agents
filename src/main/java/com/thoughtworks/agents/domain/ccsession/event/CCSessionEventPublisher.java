package com.thoughtworks.agents.domain.ccsession.event;

public interface CCSessionEventPublisher {

    void publishStatusChanged(CCSessionStatusChangedEvent event);
}
