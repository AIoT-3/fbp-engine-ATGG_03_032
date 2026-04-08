package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

public class PrintNode extends AbstractNode {

    public PrintNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    public void onProcess(Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }
        System.out.printf("[%s] %s\n", getId(), message);
    }
}
