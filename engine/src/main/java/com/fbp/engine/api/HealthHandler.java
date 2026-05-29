package com.fbp.engine.api;

import com.fbp.engine.core.engine.FlowManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class HealthHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("GET".equals(method)) {
            try {
                FlowManager flowManager = FlowManager.getInstance();
                long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
                int flowCount = flowManager.getDeployedFlowList().size();

                Map<String, Object> response = new HashMap<>();
                response.put("status", "UP");
                response.put("uptime", uptime);
                response.put("flowCount", flowCount);

                ApiResponse.ok(exchange, response);
            } catch (Exception e) {
                ApiResponse.internalServerError(exchange, e.getMessage());
            }
        } else {
            ApiResponse.methodNotAllowed(exchange);
        }
    }
}
