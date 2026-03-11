package com.thoughtworks.agents.infr.process;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;

public interface ClaudeCodeCliProcessManager {

    CCProcessHandle start(CCSessionId sessionId, ProcessConfig config);

    void sendInput(CCSessionId sessionId, String input);

    void terminate(CCSessionId sessionId);

    boolean isRunning(CCSessionId sessionId);
}
