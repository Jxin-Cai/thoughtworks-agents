package com.thoughtworks.agents.domain.devtask.repository;

import com.thoughtworks.agents.domain.conversation.model.ConversationId;
import com.thoughtworks.agents.domain.devtask.model.DevTask;
import com.thoughtworks.agents.domain.devtask.model.DevTaskId;

import java.util.List;
import java.util.Optional;

public interface DevTaskRepository {

    void save(DevTask task);

    Optional<DevTask> findById(DevTaskId id);

    List<DevTask> findByConversationId(ConversationId conversationId);

    List<DevTask> findByRepositoryFullName(String repositoryFullName);
}
