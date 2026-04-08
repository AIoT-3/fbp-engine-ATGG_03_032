package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

public class FilterNode extends AbstractNode{
    private final String key;
    private final double threshold;

    public FilterNode(String id, String key, double threshold) {
        super(id);
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }

        this.key = key;
        this.threshold = threshold;
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }
        if(!message.hasKey(key)){
            return;
        }

        Object rawValue = message.get(key);
        if (!(rawValue instanceof Number)) {
            return;
        }
        double value = ((Number) rawValue).doubleValue();

        if(value < threshold){
            return;
        }

        send("out", message);
    }
}
