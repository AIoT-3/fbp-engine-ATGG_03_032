package com.fbp.engine.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class RouterHandler implements HttpHandler {
    private final FlowHandler flowHandler = new FlowHandler();
    private final MetricsHandler metricsHandler = new MetricsHandler();
    private final HealthHandler healthHandler = new HealthHandler();

    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/health") || path.equals("/health/")) {
                healthHandler.handle(exchange);
                return;
            }

            if (path.equals("/flows") || path.equals("/flows/")) {
                flowHandler.handle(exchange);
                return;
            }

            if (path.startsWith("/flows/")) {
                String[] parts = path.split("/");
                if (parts.length == 3) {
                    flowHandler.handle(exchange);
                    return;
                } else if (parts.length == 4 && path.endsWith("/metrics")) {
                    metricsHandler.handle(exchange);
                    return;
                }
            }

            if (path.startsWith("/nodes/") && path.endsWith("/stats")) {
                String[] parts = path.split("/");
                if (parts.length == 4) {
                    metricsHandler.handle(exchange);
                    return;
                }
            }

            ApiResponse.notFound(exchange, "Endpoint Not Found");

        } catch (Exception e) {
            try {
                ApiResponse.internalServerError(exchange, "Internal Server Error: " + e.getMessage());
            } catch (IOException ioException) {
            }
        }
    }
}
