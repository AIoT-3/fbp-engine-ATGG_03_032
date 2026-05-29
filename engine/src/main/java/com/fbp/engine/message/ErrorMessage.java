package com.fbp.engine.message;

public class ErrorMessage extends Message {
    private final Message originalMessage;
    private final Exception exception;
    private final String errorNodeId;

    public ErrorMessage(Message originalMessage, Exception exception, String errorNodeId) {
        super(originalMessage.getPayload()); // Keep original payload
        this.originalMessage = originalMessage;
        this.exception = exception;
        this.errorNodeId = errorNodeId;
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public Exception getException() {
        return exception;
    }

    public String getErrorNodeId() {
        return errorNodeId;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "errorNodeId='" + errorNodeId + '\'' +
                ", exception=" + exception.getClass().getSimpleName() +
                ", originalMessage=" + originalMessage +
                '}';
    }
}
