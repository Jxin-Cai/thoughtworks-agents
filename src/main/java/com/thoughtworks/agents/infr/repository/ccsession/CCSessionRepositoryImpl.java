package com.thoughtworks.agents.infr.repository.ccsession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.agents.domain.ccsession.model.CCSession;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionStatus;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;
import com.thoughtworks.agents.domain.ccsession.repository.CCSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CCSessionRepositoryImpl implements CCSessionRepository {

    private final CCSessionMapper ccSessionMapper;
    private final ObjectMapper objectMapper;

    public CCSessionRepositoryImpl(CCSessionMapper ccSessionMapper, ObjectMapper objectMapper) {
        this.ccSessionMapper = ccSessionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(CCSession session) {
        CCSessionPO po = toPO(session);
        CCSessionPO existing = ccSessionMapper.selectById(po.getId());
        if (existing != null) {
            ccSessionMapper.updateById(po);
        } else {
            ccSessionMapper.insert(po);
        }
    }

    @Override
    public Optional<CCSession> findById(CCSessionId id) {
        CCSessionPO po = ccSessionMapper.selectById(id.getValue());
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<CCSession> findActiveSessions() {
        List<CCSessionPO> poList = ccSessionMapper.selectActiveSessions();
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private CCSessionPO toPO(CCSession session) {
        CCSessionPO po = new CCSessionPO();
        po.setId(session.getId().getValue());
        po.setCommand(session.getProcessConfig().getCommand());
        po.setWorkingDirectory(session.getProcessConfig().getWorkingDirectory());
        po.setEnvironmentVariables(serializeMap(session.getProcessConfig().getEnvironmentVariables()));
        po.setStatus(session.getStatus().name());
        po.setExitCode(session.getExitCode());
        po.setCreatedAt(session.getCreatedAt());
        po.setStartedAt(session.getStartedAt());
        po.setFinishedAt(session.getFinishedAt());
        return po;
    }

    private CCSession toDomain(CCSessionPO po) {
        Map<String, String> envVars = deserializeMap(po.getEnvironmentVariables());
        ProcessConfig processConfig = new ProcessConfig(po.getCommand(), po.getWorkingDirectory(), envVars);
        return CCSession.reconstitute(
                new CCSessionId(po.getId()),
                processConfig,
                CCSessionStatus.valueOf(po.getStatus()),
                po.getExitCode(),
                po.getCreatedAt(),
                po.getStartedAt(),
                po.getFinishedAt()
        );
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize environment variables", e);
        }
    }

    private Map<String, String> deserializeMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize environment variables", e);
        }
    }
}
