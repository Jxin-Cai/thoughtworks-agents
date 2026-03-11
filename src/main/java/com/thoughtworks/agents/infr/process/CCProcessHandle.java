package com.thoughtworks.agents.infr.process;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;

import java.util.concurrent.CompletableFuture;

public class CCProcessHandle {

    private final CCSessionId sessionId;
    private final CompletableFuture<Integer> exitCodeFuture;

    public CCProcessHandle(CCSessionId sessionId, CompletableFuture<Integer> exitCodeFuture) {
        this.sessionId = sessionId;
        this.exitCodeFuture = exitCodeFuture;
    }

    public CCSessionId getSessionId() {
        return sessionId;
    }

    public CompletableFuture<Integer> getExitCodeFuture() {
        return exitCodeFuture;
    }
}
