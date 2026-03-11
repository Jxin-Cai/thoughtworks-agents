package com.thoughtworks.agents.application.devtask;

import com.thoughtworks.agents.domain.devtask.model.DevTask;

import java.time.LocalDateTime;
import java.util.List;

public class DevTaskDTO {

    private final String id;
    private final String conversationId;
    private final String repositoryFullName;
    private final String branchName;
    private final String requirement;
    private final String status;
    private final List<TaskPhaseDTO> phases;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private DevTaskDTO(String id, String conversationId, String repositoryFullName, String branchName,
                       String requirement, String status, List<TaskPhaseDTO> phases,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.repositoryFullName = repositoryFullName;
        this.branchName = branchName;
        this.requirement = requirement;
        this.status = status;
        this.phases = phases;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DevTaskDTO from(DevTask task) {
        List<TaskPhaseDTO> phaseDTOs = task.getPhases().stream()
                .map(TaskPhaseDTO::from)
                .toList();
        return new DevTaskDTO(
                task.getId().getValue(),
                task.getConversationId().getValue(),
                task.getRepositoryFullName(),
                task.getBranchName(),
                task.getRequirement(),
                task.getStatus().name(),
                phaseDTOs,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getRequirement() {
        return requirement;
    }

    public String getStatus() {
        return status;
    }

    public List<TaskPhaseDTO> getPhases() {
        return phases;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
