package com.fbp.engine.core.parser;

public class FlowParserException extends RuntimeException {
    public FlowParserException(String message) {
        super(message);
    }
    public FlowParserException(Exception e){super(e);}
}
