package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

public class CollectorNode extends AbstractNode{
    private List<Message> collected;

    public CollectorNode(String id) {
        super(id);
        collected = new ArrayList<>();

        addInputPort("in");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must be notNull");
        }

        collected.add(message);
    }

    public List<Message> getCollected(){
        return new ArrayList<>(collected);
    }
}
