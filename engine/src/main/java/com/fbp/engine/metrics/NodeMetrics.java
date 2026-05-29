package com.fbp.engine.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class NodeMetrics {
    private final String nodeId;
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    public NodeMetrics(String nodeId) {
        this.nodeId = nodeId;
    }

    public void record(long elapsedTime, boolean success) {
        processedCount.incrementAndGet();
        if (!success) {
            errorCount.incrementAndGet();
        }
        totalProcessingTime.addAndGet(elapsedTime);
    }

    public void reset() {
        processedCount.set(0);
        errorCount.set(0);
        totalProcessingTime.set(0);
    }

    public Snapshot getSnapshot() {
        long processed = processedCount.get();
        long errors = errorCount.get();
        long totalTime = totalProcessingTime.get();
        double avgTime = (processed > 0) ? (double) totalTime / processed : 0.0;

        return new Snapshot(nodeId, processed, errors, avgTime);
    }

    public static class Snapshot {
        private final String nodeId;
        private final long processed;
        private final long errors;
        private final double avgProcessingTime;

        public Snapshot(String nodeId, long processed, long errors, double avgProcessingTime) {
            this.nodeId = nodeId;
            this.processed = processed;
            this.errors = errors;
            this.avgProcessingTime = avgProcessingTime;
        }

        public String getNodeId() {
            return nodeId;
        }

        public long getProcessed() {
            return processed;
        }

        public long getErrors() {
            return errors;
        }

        public double getAvgProcessingTime() {
            return avgProcessingTime;
        }
    }
}
