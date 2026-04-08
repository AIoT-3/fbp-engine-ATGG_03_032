package com.fbp.engine.node.exception;

public class NotFoundPortNameException extends RuntimeException {
    public NotFoundPortNameException(String name) {
        super(String.format("Not Found Port... name: %s", name));
    }
}
