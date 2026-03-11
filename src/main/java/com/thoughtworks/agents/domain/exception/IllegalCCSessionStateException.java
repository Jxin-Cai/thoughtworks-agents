package com.thoughtworks.agents.domain.exception;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionStatus;

public class IllegalCCSessionStateException extends RuntimeException {

    private final CCSessionStatus currentStatus;
    private final CCSessionStatus targetStatus;

    public IllegalCCSessionStateException(CCSessionStatus currentStatus, CCSessionStatus targetStatus) {
        super("Cannot transition CCSession from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public CCSessionStatus getCurrentStatus() {
        return currentStatus;
    }

    public CCSessionStatus getTargetStatus() {
        return targetStatus;
    }
}
