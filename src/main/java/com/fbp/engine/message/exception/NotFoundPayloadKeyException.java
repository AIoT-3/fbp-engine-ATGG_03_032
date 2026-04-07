package com.fbp.engine.message.exception;

public class NotFoundPayloadKeyException extends RuntimeException {
    public NotFoundPayloadKeyException(String key) {
        super(
                String.format("Payload does not contain the key: '%s'",key)
        );
    }
}
