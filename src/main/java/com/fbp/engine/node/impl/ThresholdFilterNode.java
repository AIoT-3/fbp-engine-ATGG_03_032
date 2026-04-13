package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

public class ThresholdFilterNode extends AbstractNode{
    private String fieldName;
    private double threshold;

    public ThresholdFilterNode(String id, String fieldName, double threshold) {
        super(id);
        if(fieldName == null || fieldName.isBlank()){
            throw new IllegalArgumentException("fieldName must be notBlank");
        }

        this.fieldName = fieldName;
        this.threshold = threshold;

        addInputPort("in");
        addOutputPort("alert");
        addOutputPort("normal");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        Double value = message.get(fieldName);
        if(value == null){
            return;
        }

        if(value>threshold){
            send("alert",message);
        }else{
            send("normal",message);
        }
    }
}
