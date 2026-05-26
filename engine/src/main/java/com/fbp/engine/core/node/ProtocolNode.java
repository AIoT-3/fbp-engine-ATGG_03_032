package com.fbp.engine.core.node;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ProtocolNode extends AbstractNode {
    enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    private static final long DEFAULT_RECONNECT_INTERVAL_MS = 5000;
    private static final int DEFAULT_MAXIMUM_RECONNECTION_ATTEMPTS = 10;

    private final Map<String, Object> config;
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private final long reconnectIntervalMs;
    private final int maximumReconnectAttempts;

    private ScheduledExecutorService scheduledExecutorService;
    private volatile int reconnectionAttempts = 0;

    public ProtocolNode(String id, Map<String, Object> config) {
        super(id);
        this.config = Objects.requireNonNull(config, "config must be notNull");

        Long configReconnectIntervalMs = (Long) config.get("RECONNECT_INTERVAL_MS");
        this.reconnectIntervalMs = configReconnectIntervalMs != null ? configReconnectIntervalMs : DEFAULT_RECONNECT_INTERVAL_MS;

        Integer configMaximumReconnectAttempts = (Integer) config.get("MAXIMUM_RECONNECT_ATTEMPTS");
        this.maximumReconnectAttempts = configMaximumReconnectAttempts != null ? configMaximumReconnectAttempts : DEFAULT_MAXIMUM_RECONNECTION_ATTEMPTS;
    }

    public void initialize() {
        log.info("[{}], initializing", getId());
        this.connectionState = ConnectionState.CONNECTING;
        try {
            connect();
            this.connectionState = ConnectionState.CONNECTED;
            log.info("[{}], connected", getId());
        } catch (Exception e) {
            this.connectionState = ConnectionState.ERROR;
            log.error("[{}], connection failed. starting reconnect...", getId());
            reconnect();
        }
    }

    public void shutdown() {
        log.info("[{}], shutting down", getId());
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (connectionState == ConnectionState.CONNECTED) {
            disconnect();
        }
        this.connectionState = ConnectionState.DISCONNECTED;
        log.info("[{}], disconnected", getId());
    }

    protected abstract void connect() throws Exception;
    protected abstract void disconnect();

    public void reconnect() {
        log.info("[{}], reconnect scheduled (interval: {}ms, maxAttempts: {})", getId(), reconnectIntervalMs, maximumReconnectAttempts);

        reconnectionAttempts = 0;
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdownNow();
        }

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (reconnectionAttempts >= maximumReconnectAttempts) {
                log.error("[{}], max reconnection attempts reached. giving up.", getId());
                connectionState = ConnectionState.ERROR;
                scheduledExecutorService.shutdown();
                return;
            }
            reconnectionAttempts++;
            log.info("[{}], reconnection attempt {}/{}", getId(), reconnectionAttempts, maximumReconnectAttempts);
            try {
                connect();
                connectionState = ConnectionState.CONNECTED;
                log.info("[{}], reconnected successfully (attempt: {})", getId(), reconnectionAttempts);
                scheduledExecutorService.shutdown();
            } catch (Exception e) {
                log.warn("[{}], reconnection attempt {}/{} failed. error: {}", getId(), reconnectionAttempts, maximumReconnectAttempts, e.getMessage());
            }

        }, reconnectIntervalMs, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public Object getConfig(String key) {
        return config.get(key);
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }
}