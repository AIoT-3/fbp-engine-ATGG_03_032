package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

public class DelayNode extends AbstractNode{
    private long delayMs;

    public DelayNode(String id, long delayMs) {
        super(id);
        if(delayMs<0){
            throw new IllegalArgumentException("delayMs must be more than 0");
        }
        this.delayMs = delayMs;

        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        send("out", message);
    }
}
