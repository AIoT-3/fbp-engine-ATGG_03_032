package com.fbp.engine.api;

import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetrics;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/flows/") && path.endsWith("/metrics")) {
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        String flowId = parts[2];
                        handleGetFlowMetrics(exchange, flowId);
                        return;
                    }
                } else if (path.startsWith("/nodes/") && path.endsWith("/stats")) {
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        String flowId = exchange.getRequestURI().getQuery();
                        String nodeId = parts[2];
                        handleGetNodeStats(exchange, nodeId);
                        return;
                    }
                }
                ApiResponse.notFound(exchange, "Not Found");
            } else {
                ApiResponse.methodNotAllowed(exchange);
            }
        } catch (Exception e) {
            ApiResponse.internalServerError(exchange, e.getMessage());
        }
    }

    private void handleGetFlowMetrics(HttpExchange exchange, String flowId) throws IOException {
        MetricsCollector collector = MetricsCollector.getInstance();
        List<NodeMetrics.Snapshot> nodeMetricsList = collector.getFlowMetrics(flowId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("nodes", nodeMetricsList);
        
        ApiResponse.ok(exchange, response);
    }

    private void handleGetNodeStats(HttpExchange exchange, String nodeId) throws IOException {
        MetricsCollector collector = MetricsCollector.getInstance();
        String query = exchange.getRequestURI().getQuery();
        String flowId = null;
        if (query != null && query.startsWith("flowId=")) {
            flowId = query.substring(7);
        }

        if (flowId == null) {
            ApiResponse.badRequest(exchange, "flowId query parameter is required for /nodes/{id}/stats");
            return;
        }

        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(flowId, nodeId);
        if (snapshot != null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("processed", snapshot.getProcessed());
            stats.put("errors", snapshot.getErrors());
            stats.put("avgTime", snapshot.getAvgProcessingTime());
            stats.put("queueSize", 0);
            ApiResponse.ok(exchange, stats);
        } else {
            ApiResponse.notFound(exchange, "Node stats not found");
        }
    }
}
