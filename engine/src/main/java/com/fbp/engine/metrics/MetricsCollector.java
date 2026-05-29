package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MetricsCollector {

    private static final MetricsCollector INSTANCE = new MetricsCollector();

    private final Map<String, Map<String, NodeMetrics>> flowMetrics = new ConcurrentHashMap<>();

    private MetricsCollector() {}

    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    public void recordProcessing(String flowId, String nodeId, long elapsedTime, boolean success) {
        Map<String, NodeMetrics> nodeMetricsMap = flowMetrics.computeIfAbsent(flowId, k -> new ConcurrentHashMap<>());
        NodeMetrics nodeMetrics = nodeMetricsMap.computeIfAbsent(nodeId, NodeMetrics::new);
        nodeMetrics.record(elapsedTime, success);
    }

    public NodeMetrics.Snapshot getNodeMetric(String flowId, String nodeId) {
        Map<String, NodeMetrics> nodeMetricsMap = flowMetrics.get(flowId);
        if (nodeMetricsMap != null) {
            NodeMetrics nodeMetrics = nodeMetricsMap.get(nodeId);
            if (nodeMetrics != null) {
                return nodeMetrics.getSnapshot();
            }
        }
        return null;
    }

    public List<NodeMetrics.Snapshot> getFlowMetrics(String flowId) {
        Map<String, NodeMetrics> nodeMetricsMap = flowMetrics.get(flowId);
        if (nodeMetricsMap != null) {
            return nodeMetricsMap.values().stream()
                    .map(NodeMetrics::getSnapshot)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void resetFlowMetrics(String flowId) {
        flowMetrics.remove(flowId);
    }

    public void resetAll() {
        flowMetrics.clear();
    }
}
