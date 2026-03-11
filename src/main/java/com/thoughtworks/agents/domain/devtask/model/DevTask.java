package com.thoughtworks.agents.domain.devtask.model;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.conversation.model.ConversationId;
import com.thoughtworks.agents.domain.exception.IllegalDevTaskStateException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevTask {

    private final DevTaskId id;
    private final ConversationId conversationId;
    private final String repositoryFullName;
    private final String branchName;
    private final String requirement;
    private DevTaskStatus status;
    private final List<TaskPhase> phases;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DevTask(DevTaskId id, ConversationId conversationId, String repositoryFullName,
                    String branchName, String requirement, DevTaskStatus status,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.repositoryFullName = repositoryFullName;
        this.branchName = branchName;
        this.requirement = requirement;
        this.status = status;
        this.phases = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DevTask create(ConversationId conversationId, String repositoryFullName,
                                 String branchName, String requirement) {
        if (conversationId == null) {
            throw new IllegalArgumentException("conversationId must not be null");
        }
        if (repositoryFullName == null || repositoryFullName.isBlank()) {
            throw new IllegalArgumentException("repositoryFullName must not be blank");
        }
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("branchName must not be blank");
        }
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement must not be blank");
        }
        LocalDateTime now = LocalDateTime.now();
        return new DevTask(
                DevTaskId.generate(),
                conversationId,
                repositoryFullName,
                branchName,
                requirement,
                DevTaskStatus.CREATED,
                now,
                now
        );
    }

    public static DevTask reconstitute(DevTaskId id, ConversationId conversationId, String repositoryFullName,
                                       String branchName, String requirement, DevTaskStatus status,
                                       List<TaskPhase> phases, LocalDateTime createdAt, LocalDateTime updatedAt) {
        DevTask task = new DevTask(id, conversationId, repositoryFullName, branchName, requirement,
                status, createdAt, updatedAt);
        if (phases != null) {
            task.phases.addAll(phases);
        }
        return task;
    }

    public TaskPhase startThinking(CCSessionId ccSessionId) {
        if (!status.canTransitionTo(DevTaskStatus.THINKING)) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.THINKING);
        }
        this.status = DevTaskStatus.THINKING;
        this.updatedAt = LocalDateTime.now();
        TaskPhase phase = new TaskPhase(TaskPhaseId.generate(), PhaseType.THINKING, ccSessionId, LocalDateTime.now());
        this.phases.add(phase);
        return phase;
    }

    public void completeThinking(String designOutput) {
        if (status != DevTaskStatus.THINKING) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.WORKING);
        }
        getCurrentPhase(PhaseType.THINKING).complete(designOutput);
        this.status = DevTaskStatus.WORKING;
        this.updatedAt = LocalDateTime.now();
    }

    public TaskPhase startWorking(CCSessionId ccSessionId) {
        if (status != DevTaskStatus.WORKING) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.WORKING);
        }
        this.updatedAt = LocalDateTime.now();
        TaskPhase phase = new TaskPhase(TaskPhaseId.generate(), PhaseType.WORKING, ccSessionId, LocalDateTime.now());
        this.phases.add(phase);
        return phase;
    }

    public void completeWorking() {
        if (status != DevTaskStatus.WORKING) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.READY_TO_PUBLISH);
        }
        getCurrentPhase(PhaseType.WORKING).complete(null);
        this.status = DevTaskStatus.READY_TO_PUBLISH;
        this.updatedAt = LocalDateTime.now();
    }

    public TaskPhase startPublishing() {
        if (!status.canTransitionTo(DevTaskStatus.PUBLISHING)) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.PUBLISHING);
        }
        this.status = DevTaskStatus.PUBLISHING;
        this.updatedAt = LocalDateTime.now();
        TaskPhase phase = new TaskPhase(TaskPhaseId.generate(), PhaseType.PUBLISHING, null, LocalDateTime.now());
        this.phases.add(phase);
        return phase;
    }

    public void completePublishing() {
        if (status != DevTaskStatus.PUBLISHING) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.PUBLISHED);
        }
        getCurrentPhase(PhaseType.PUBLISHING).complete(null);
        this.status = DevTaskStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (!status.canTransitionTo(DevTaskStatus.FAILED)) {
            throw new IllegalDevTaskStateException(status, DevTaskStatus.FAILED);
        }
        if (!phases.isEmpty()) {
            TaskPhase lastPhase = phases.get(phases.size() - 1);
            if (lastPhase.getFinishedAt() == null) {
                lastPhase.fail(reason);
            }
        }
        this.status = DevTaskStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    private TaskPhase getCurrentPhase(PhaseType expectedType) {
        for (int i = phases.size() - 1; i >= 0; i--) {
            TaskPhase phase = phases.get(i);
            if (phase.getPhaseType() == expectedType) {
                return phase;
            }
        }
        throw new IllegalArgumentException("No phase found for type: " + expectedType);
    }

    public DevTaskId getId() {
        return id;
    }

    public ConversationId getConversationId() {
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

    public DevTaskStatus getStatus() {
        return status;
    }

    public List<TaskPhase> getPhases() {
        return Collections.unmodifiableList(phases);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
