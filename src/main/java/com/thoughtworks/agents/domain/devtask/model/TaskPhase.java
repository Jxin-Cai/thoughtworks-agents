package com.thoughtworks.agents.domain.devtask.model;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;

import java.time.LocalDateTime;

public class TaskPhase {

    private final TaskPhaseId id;
    private final PhaseType phaseType;
    private final CCSessionId ccSessionId;
    private String output;
    private final LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String failureReason;

    public TaskPhase(TaskPhaseId id, PhaseType phaseType, CCSessionId ccSessionId, LocalDateTime startedAt) {
        if (id == null) {
            throw new IllegalArgumentException("TaskPhase id must not be null");
        }
        if (phaseType == null) {
            throw new IllegalArgumentException("TaskPhase phaseType must not be null");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("TaskPhase startedAt must not be null");
        }
        this.id = id;
        this.phaseType = phaseType;
        this.ccSessionId = ccSessionId;
        this.startedAt = startedAt;
    }

    public static TaskPhase reconstitute(TaskPhaseId id, PhaseType phaseType, CCSessionId ccSessionId,
                                         String output, LocalDateTime startedAt,
                                         LocalDateTime finishedAt, String failureReason) {
        TaskPhase phase = new TaskPhase(id, phaseType, ccSessionId, startedAt);
        phase.output = output;
        phase.finishedAt = finishedAt;
        phase.failureReason = failureReason;
        return phase;
    }

    public void complete(String output) {
        this.output = output;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.failureReason = reason;
        this.finishedAt = LocalDateTime.now();
    }

    public TaskPhaseId getId() {
        return id;
    }

    public PhaseType getPhaseType() {
        return phaseType;
    }

    public CCSessionId getCcSessionId() {
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
