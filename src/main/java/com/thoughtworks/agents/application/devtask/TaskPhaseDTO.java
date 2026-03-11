package com.thoughtworks.agents.application.devtask;

import com.thoughtworks.agents.domain.devtask.model.TaskPhase;

import java.time.LocalDateTime;

public class TaskPhaseDTO {

    private final String id;
    private final String phaseType;
    private final String ccSessionId;
    private final String output;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final String failureReason;

    private TaskPhaseDTO(String id, String phaseType, String ccSessionId, String output,
                         LocalDateTime startedAt, LocalDateTime finishedAt, String failureReason) {
        this.id = id;
        this.phaseType = phaseType;
        this.ccSessionId = ccSessionId;
        this.output = output;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failureReason = failureReason;
    }

    public static TaskPhaseDTO from(TaskPhase phase) {
        return new TaskPhaseDTO(
                phase.getId().getValue(),
                phase.getPhaseType().name(),
                phase.getCcSessionId() != null ? phase.getCcSessionId().getValue() : null,
                phase.getOutput(),
                phase.getStartedAt(),
                phase.getFinishedAt(),
                phase.getFailureReason()
        );
    }

    public String getId() {
        return id;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public String getCcSessionId() {
        return ccSessionId;
    }

    public String getOutput() {
        return output;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
