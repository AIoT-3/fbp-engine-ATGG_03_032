package com.fbp.engine.api;

import com.fbp.engine.core.engine.FlowManager;
import com.fbp.engine.core.parser.FlowDefinition;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if ("/flows".equals(path) || "/flows/".equals(path)) {
                    handleGetFlows(exchange);
                } else {
                    ApiResponse.notFound(exchange, "Not Found");
                }
            } else if ("POST".equals(method)) {
                if ("/flows".equals(path) || "/flows/".equals(path)) {
                    handlePostFlow(exchange);
                } else {
                    ApiResponse.notFound(exchange, "Not Found");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/flows/")) {
                    String[] parts = path.split("/");
                    if (parts.length == 3) {
                        String flowId = parts[2];
                        handleDeleteFlow(exchange, flowId);
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

    private void handleGetFlows(HttpExchange exchange) throws IOException {
        FlowManager flowManager = FlowManager.getInstance();
        List<String> flowIds = flowManager.getDeployedFlowList();
        
        List<Map<String, Object>> response = new ArrayList<>();
        for (String id : flowIds) {
            Map<String, Object> flowInfo = new HashMap<>();
            flowInfo.put("id", id);
            try {
                flowInfo.put("status", flowManager.getStatus(id).toString());
            } catch (Exception e) {
                flowInfo.put("status", "UNKNOWN");
            }
            response.add(flowInfo);
        }
        
        ApiResponse.ok(exchange, response);
    }

    private void handlePostFlow(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] body = is.readAllBytes();
            if (body.length == 0) {
                ApiResponse.badRequest(exchange, "Empty request body");
                return;
            }

            FlowDefinition flowDefinition = ApiResponse.fromJson(body, FlowDefinition.class);
            
            if (flowDefinition.getId() == null || flowDefinition.getId().isEmpty()) {
                ApiResponse.badRequest(exchange, "Flow ID is required");
                return;
            }

            FlowManager flowManager = FlowManager.getInstance();
            try {
                flowManager.deploy(flowDefinition);
                
                Map<String, String> response = new HashMap<>();
                response.put("id", flowDefinition.getId());
                response.put("status", flowManager.getStatus(flowDefinition.getId()).toString());
                
                ApiResponse.created(exchange, response);
            } catch (IllegalArgumentException e) {
                ApiResponse.badRequest(exchange, e.getMessage());
            }
        } catch (Exception e) {
            ApiResponse.badRequest(exchange, "Invalid JSON format: " + e.getMessage());
        }
    }

    private void handleDeleteFlow(HttpExchange exchange, String flowId) throws IOException {
        FlowManager flowManager = FlowManager.getInstance();
        try {
            flowManager.remove(flowId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Flow " + flowId + " removed successfully");
            ApiResponse.ok(exchange, response);
        } catch (IllegalArgumentException e) {
            ApiResponse.notFound(exchange, e.getMessage());
        }
    }
}
