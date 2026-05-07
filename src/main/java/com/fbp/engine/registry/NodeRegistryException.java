package com.fbp.engine.registry;

public class NodeRegistryException extends RuntimeException {
    public NodeRegistryException(String message) {
        super(message);
    }

    public NodeRegistryException(Exception e){
        super(e);
    }
}
