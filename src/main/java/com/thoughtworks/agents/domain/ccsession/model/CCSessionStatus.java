package com.thoughtworks.agents.domain.ccsession.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum CCSessionStatus {

    CREATED,
    RUNNING,
    COMPLETED,
    FAILED,
    TERMINATED;

    private static final Map<CCSessionStatus, Set<CCSessionStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, EnumSet.of(RUNNING),
            RUNNING, EnumSet.of(COMPLETED, FAILED, TERMINATED),
            COMPLETED, EnumSet.noneOf(CCSessionStatus.class),
            FAILED, EnumSet.noneOf(CCSessionStatus.class),
            TERMINATED, EnumSet.noneOf(CCSessionStatus.class)
    );

    public boolean canTransitionTo(CCSessionStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(CCSessionStatus.class)).contains(target);
    }
}
