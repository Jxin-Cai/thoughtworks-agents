package com.thoughtworks.agents.application.ccsession;

import com.thoughtworks.agents.domain.ccsession.model.CCSession;

import java.time.LocalDateTime;

public class CCSessionDTO {

    private final String id;
    private final String status;
    private final String command;
    private final String workingDirectory;
    private final LocalDateTime createdAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final Integer exitCode;

    private CCSessionDTO(String id, String status, String command, String workingDirectory,
                         LocalDateTime createdAt, LocalDateTime startedAt,
                         LocalDateTime finishedAt, Integer exitCode) {
        this.id = id;
        this.status = status;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.exitCode = exitCode;
    }

    public static CCSessionDTO from(CCSession session) {
        return new CCSessionDTO(
                session.getId().getValue(),
                session.getStatus().name(),
                session.getProcessConfig().getCommand(),
                session.getProcessConfig().getWorkingDirectory(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getExitCode()
        );
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
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

    public Integer getExitCode() {
        return exitCode;
    }
}
