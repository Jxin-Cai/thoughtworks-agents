package com.thoughtworks.agents.infr.process;

import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;
import com.thoughtworks.agents.infr.websocket.WebSocketMessageBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;

@Component
public class ClaudeCodeCliProcessManagerImpl implements ClaudeCodeCliProcessManager {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeCliProcessManagerImpl.class);
    private static final long TERMINATE_TIMEOUT_MS = 5000;
    private static final int MAX_CONCURRENT_PROCESSES = 10;

    private final ConcurrentHashMap<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final ExecutorService outputReaderExecutor = Executors.newCachedThreadPool();
    private final WebSocketMessageBroker webSocketMessageBroker;

    public ClaudeCodeCliProcessManagerImpl(WebSocketMessageBroker webSocketMessageBroker) {
        this.webSocketMessageBroker = webSocketMessageBroker;
    }

    @Override
    public CCProcessHandle start(CCSessionId sessionId, ProcessConfig config) {
        if (activeProcesses.size() >= MAX_CONCURRENT_PROCESSES) {
            throw new IllegalStateException("Maximum concurrent process limit reached: " + MAX_CONCURRENT_PROCESSES);
        }

        String sessionIdValue = sessionId.getValue();
        if (activeProcesses.containsKey(sessionIdValue)) {
            throw new IllegalStateException("Process already running for session: " + sessionIdValue);
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(Arrays.asList("/bin/sh", "-c", config.getCommand()));
            processBuilder.directory(new File(config.getWorkingDirectory()));
            processBuilder.redirectErrorStream(true);

            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                processBuilder.environment().putAll(config.getEnvironmentVariables());
            }

            Process process = processBuilder.start();
            activeProcesses.put(sessionIdValue, process);

            CompletableFuture<Integer> exitCodeFuture = new CompletableFuture<>();

            outputReaderExecutor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()), 8192)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        webSocketMessageBroker.sendCCSessionOutput(sessionIdValue, line);
                    }
                } catch (IOException e) {
                    log.error("Error reading process output for session: {}", sessionIdValue, e);
                }
            });

            outputReaderExecutor.submit(() -> {
                try {
                    int exitCode = process.waitFor();
                    activeProcesses.remove(sessionIdValue);
                    exitCodeFuture.complete(exitCode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    activeProcesses.remove(sessionIdValue);
                    exitCodeFuture.completeExceptionally(e);
                }
            });

            return new CCProcessHandle(sessionId, exitCodeFuture);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start process for session: " + sessionIdValue, e);
        }
    }

    @Override
    public void sendInput(CCSessionId sessionId, String input) {
        String sessionIdValue = sessionId.getValue();
        Process process = activeProcesses.get(sessionIdValue);
        if (process == null) {
            throw new IllegalStateException("No active process found for session: " + sessionIdValue);
        }
        try {
            OutputStream outputStream = process.getOutputStream();
            outputStream.write(input.getBytes());
            outputStream.write('\n');
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send input to process for session: " + sessionIdValue, e);
        }
    }

    @Override
    public void terminate(CCSessionId sessionId) {
        String sessionIdValue = sessionId.getValue();
        Process process = activeProcesses.get(sessionIdValue);
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            boolean exited = process.waitFor(TERMINATE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } finally {
            activeProcesses.remove(sessionIdValue);
        }
    }

    @Override
    public boolean isRunning(CCSessionId sessionId) {
        String sessionIdValue = sessionId.getValue();
        Process process = activeProcesses.get(sessionIdValue);
        return process != null && process.isAlive();
    }
}
