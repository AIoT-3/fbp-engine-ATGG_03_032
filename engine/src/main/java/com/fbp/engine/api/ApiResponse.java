package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public class ApiResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void ok(HttpExchange exchange, Object data) throws IOException {
        sendResponse(exchange, 200, data);
    }

    public static void created(HttpExchange exchange, Object data) throws IOException {
        sendResponse(exchange, 201, data);
    }

    public static void badRequest(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 400, message);
    }

    public static void notFound(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 404, message);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        sendError(exchange, 405, "Method Not Allowed");
    }

    public static void internalServerError(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 500, message);
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(message);
        sendResponse(exchange, statusCode, errorResponse);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] responseBytes = new byte[0];
        if (data != null) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            responseBytes = objectMapper.writeValueAsBytes(data);
        }
        
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        if (responseBytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    public static <T> T fromJson(byte[] src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }

    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
