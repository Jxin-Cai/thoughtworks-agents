package com.thoughtworks.agents.domain.conversation.repository;

import com.thoughtworks.agents.domain.conversation.model.Conversation;
import com.thoughtworks.agents.domain.conversation.model.ConversationId;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    void save(Conversation conversation);

    Optional<Conversation> findById(ConversationId id);

    List<Conversation> findByRepositoryFullName(String repositoryFullName);

    List<Conversation> findAll();
}
