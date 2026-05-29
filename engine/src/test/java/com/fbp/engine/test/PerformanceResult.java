package com.fbp.engine.test;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerformanceResult {
    private long totalMessages;
    private long durationMs;
    private double throughput;
    private double avgLatencyMs;
    private double p99LatencyMs;
    private long errors;
    private double errorRate;
}
