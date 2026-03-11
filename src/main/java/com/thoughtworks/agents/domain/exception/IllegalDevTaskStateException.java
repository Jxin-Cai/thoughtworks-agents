package com.thoughtworks.agents.domain.exception;

import com.thoughtworks.agents.domain.devtask.model.DevTaskStatus;

public class IllegalDevTaskStateException extends RuntimeException {

    private final DevTaskStatus currentStatus;
    private final DevTaskStatus targetStatus;

    public IllegalDevTaskStateException(DevTaskStatus currentStatus, DevTaskStatus targetStatus) {
        super("Cannot transition DevTask from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public DevTaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public DevTaskStatus getTargetStatus() {
        return targetStatus;
    }
}
