package com.thoughtworks.agents.domain.devtask.event;

public interface DevTaskEventPublisher {

    void publishStatusChanged(DevTaskStatusChangedEvent event);
}
