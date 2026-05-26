package com.fbp.engine.core.registry;

public class NodeRegistryException extends RuntimeException {
    public NodeRegistryException(String message) {
        super(message);
    }

    public NodeRegistryException(Exception e){
        super(e);
    }
}
