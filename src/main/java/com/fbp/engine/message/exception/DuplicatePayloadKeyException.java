package com.fbp.engine.message.exception;

public class DuplicatePayloadKeyException extends RuntimeException {
    public DuplicatePayloadKeyException(String key) {
        super(String.format("Duplicate key '%s' found in payload.", key));
    }
}
