package com.fbp.engine.core.engine;

import lombok.Data;

@Data
public class ThreadPoolConfig {
    private int corePoolSize = Runtime.getRuntime().availableProcessors();
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private long keepAliveTimeMs = 60000L;
    private int queueCapacity = 1000;
}
