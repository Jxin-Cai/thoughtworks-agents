package com.thoughtworks.agents.application.ccsession;

import com.thoughtworks.agents.application.exception.BusinessException;
import com.thoughtworks.agents.domain.ccsession.event.CCSessionEventPublisher;
import com.thoughtworks.agents.domain.ccsession.event.CCSessionStatusChangedEvent;
import com.thoughtworks.agents.domain.ccsession.model.CCSession;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionStatus;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;
import com.thoughtworks.agents.domain.ccsession.repository.CCSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CCSessionApplicationService {

    private final CCSessionRepository ccSessionRepository;
    private final CCSessionEventPublisher ccSessionEventPublisher;

    public CCSessionApplicationService(CCSessionRepository ccSessionRepository,
                                       CCSessionEventPublisher ccSessionEventPublisher) {
        this.ccSessionRepository = ccSessionRepository;
        this.ccSessionEventPublisher = ccSessionEventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public CCSessionDTO createCCSession(CreateCCSessionCommand command) {
        ProcessConfig processConfig = new ProcessConfig(
                command.getCommand(),
                command.getWorkingDirectory(),
                command.getEnvironmentVariables()
        );
        CCSession session = CCSession.create(processConfig);
        ccSessionRepository.save(session);
        return CCSessionDTO.from(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void startCCSession(StartCCSessionCommand command) {
        CCSession session = ccSessionRepository.findById(new CCSessionId(command.getSessionId()))
                .orElseThrow(() -> new BusinessException("CC 会话不存在: " + command.getSessionId()));

        CCSessionStatus previousStatus = session.getStatus();
        session.markRunning();
        ccSessionRepository.save(session);

        ccSessionEventPublisher.publishStatusChanged(new CCSessionStatusChangedEvent(
                session.getId(), previousStatus, session.getStatus(), LocalDateTime.now()
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminateCCSession(TerminateCCSessionCommand command) {
        CCSession session = ccSessionRepository.findById(new CCSessionId(command.getSessionId()))
                .orElseThrow(() -> new BusinessException("CC 会话不存在: " + command.getSessionId()));

        if (!session.isActive()) {
            throw new BusinessException("会话已结束，无法终止");
        }

        CCSessionStatus previousStatus = session.getStatus();
        session.markTerminated();
        ccSessionRepository.save(session);

        ccSessionEventPublisher.publishStatusChanged(new CCSessionStatusChangedEvent(
                session.getId(), previousStatus, session.getStatus(), LocalDateTime.now()
        ));
    }

    @Transactional(readOnly = true)
    public CCSessionDTO getSession(String sessionId) {
        CCSession session = ccSessionRepository.findById(new CCSessionId(sessionId))
                .orElseThrow(() -> new BusinessException("CC 会话不存在: " + sessionId));
        return CCSessionDTO.from(session);
    }

    @Transactional(readOnly = true)
    public List<CCSessionDTO> getActiveSessions() {
        List<CCSession> sessions = ccSessionRepository.findActiveSessions();
        return sessions.stream().map(CCSessionDTO::from).toList();
    }
}
