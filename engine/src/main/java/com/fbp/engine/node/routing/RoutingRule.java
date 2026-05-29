package com.fbp.engine.node.routing;

import com.fbp.engine.message.Message;

import java.util.Objects;

public class RoutingRule {
    private final String field;
    private final String operator;
    private final Object value;
    private final String port;

    public RoutingRule(String field, String operator, Object value, String port) {
        this.field = Objects.requireNonNull(field);
        this.operator = Objects.requireNonNull(operator).toLowerCase();
        this.value = Objects.requireNonNull(value);
        this.port = Objects.requireNonNull(port);
    }

    public String getField() {
        return field;
    }

    public String getPort() {
        return port;
    }

    public boolean matches(Message message) {
        if (!message.hasKey(field)) {
            return false;
        }

        Object messageValue = message.get(field);
        if (messageValue == null) {
            return false;
        }

        try {
            switch (operator) {
                case "equals":
                    return messageValue.equals(value);
                case "gt":
                    if (messageValue instanceof Number && value instanceof Number) {
                        return ((Number) messageValue).doubleValue() > ((Number) value).doubleValue();
                    }
                    return false;
                case "lt":
                    if (messageValue instanceof Number && value instanceof Number) {
                        return ((Number) messageValue).doubleValue() < ((Number) value).doubleValue();
                    }
                    return false;
                case "contains":
                    if (messageValue instanceof String && value instanceof String) {
                        return ((String) messageValue).contains((String) value);
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
