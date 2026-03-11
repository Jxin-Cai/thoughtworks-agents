package com.thoughtworks.agents.domain.ccsession.model;

import com.thoughtworks.agents.domain.exception.IllegalCCSessionStateException;

import java.time.LocalDateTime;

public class CCSession {

    private final CCSessionId id;
    private final ProcessConfig processConfig;
    private CCSessionStatus status;
    private Integer exitCode;
    private final LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private CCSession(CCSessionId id, ProcessConfig processConfig, CCSessionStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.processConfig = processConfig;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static CCSession create(ProcessConfig processConfig) {
        return new CCSession(
                CCSessionId.generate(),
                processConfig,
                CCSessionStatus.CREATED,
                LocalDateTime.now()
        );
    }

    public static CCSession reconstitute(CCSessionId id, ProcessConfig processConfig, CCSessionStatus status,
                                         Integer exitCode, LocalDateTime createdAt,
                                         LocalDateTime startedAt, LocalDateTime finishedAt) {
        CCSession session = new CCSession(id, processConfig, status, createdAt);
        session.exitCode = exitCode;
        session.startedAt = startedAt;
        session.finishedAt = finishedAt;
        return session;
    }

    public void markRunning() {
        if (!status.canTransitionTo(CCSessionStatus.RUNNING)) {
            throw new IllegalCCSessionStateException(status, CCSessionStatus.RUNNING);
        }
        this.status = CCSessionStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted(int exitCode) {
        if (!status.canTransitionTo(CCSessionStatus.COMPLETED)) {
            throw new IllegalCCSessionStateException(status, CCSessionStatus.COMPLETED);
        }
        this.status = CCSessionStatus.COMPLETED;
        this.exitCode = exitCode;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(int exitCode) {
        if (!status.canTransitionTo(CCSessionStatus.FAILED)) {
            throw new IllegalCCSessionStateException(status, CCSessionStatus.FAILED);
        }
        this.status = CCSessionStatus.FAILED;
        this.exitCode = exitCode;
        this.finishedAt = LocalDateTime.now();
    }

    public void markTerminated() {
        if (!status.canTransitionTo(CCSessionStatus.TERMINATED)) {
            throw new IllegalCCSessionStateException(status, CCSessionStatus.TERMINATED);
        }
        this.status = CCSessionStatus.TERMINATED;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == CCSessionStatus.CREATED || status == CCSessionStatus.RUNNING;
    }

    public CCSessionId getId() {
        return id;
    }

    public ProcessConfig getProcessConfig() {
        return processConfig;
    }

    public CCSessionStatus getStatus() {
        return status;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
