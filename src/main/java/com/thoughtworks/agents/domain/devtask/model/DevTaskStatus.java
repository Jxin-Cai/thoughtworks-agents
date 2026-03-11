package com.thoughtworks.agents.domain.devtask.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum DevTaskStatus {

    CREATED,
    THINKING,
    WORKING,
    READY_TO_PUBLISH,
    PUBLISHING,
    PUBLISHED,
    FAILED;

    private static final Map<DevTaskStatus, Set<DevTaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, EnumSet.of(THINKING),
            THINKING, EnumSet.of(WORKING, FAILED),
            WORKING, EnumSet.of(READY_TO_PUBLISH, FAILED),
            READY_TO_PUBLISH, EnumSet.of(PUBLISHING, FAILED),
            PUBLISHING, EnumSet.of(PUBLISHED, FAILED),
            PUBLISHED, EnumSet.noneOf(DevTaskStatus.class),
            FAILED, EnumSet.noneOf(DevTaskStatus.class)
    );

    public boolean canTransitionTo(DevTaskStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(DevTaskStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == PUBLISHED || this == FAILED;
    }
}
