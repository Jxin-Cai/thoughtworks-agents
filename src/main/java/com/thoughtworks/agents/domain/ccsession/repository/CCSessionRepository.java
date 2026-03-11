package com.thoughtworks.agents.domain.ccsession.repository;

import com.thoughtworks.agents.domain.ccsession.model.CCSession;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;

import java.util.List;
import java.util.Optional;

public interface CCSessionRepository {

    void save(CCSession session);

    Optional<CCSession> findById(CCSessionId id);

    List<CCSession> findActiveSessions();
}
