package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

public class SplitNode extends AbstractNode{
    private final String key;
    private final double threshold;

    public SplitNode(String id, String key, double threshold) {
        super(id);
        if(key == null || key.isBlank()){
            throw new IllegalStateException("key must be notBlank");
        }
        this.key = key;
        this.threshold = threshold;

        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    @Override
    public void onProcess(Message message) {
        Object rawValue = message.get(key);

        if(!(rawValue instanceof Number)){return;}

        double value = ((Number) rawValue).doubleValue();

        if(value>=threshold){
            send("match", message);
        }else{
            send("mismatch", message);
        }
    }
}
