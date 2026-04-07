package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;

public class PrintNode implements Node {
    private final String id;

    public PrintNode(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }

        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void process(Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");

        }
        System.out.printf("[%s] %s\n", id, message);
    }
}
