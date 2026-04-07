package com.fbp.engine.node.impl;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;

import java.util.Objects;

public class FilterNode implements Node {
    private final String id;
    private final String key;
    private double threshold;
    private final InputPort inputPort;
    private final OutputPort outputPort;

    public FilterNode(String id, String key, double threshold) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }

        this.id = id;
        this.key = key;
        this.threshold = threshold;
        this.inputPort = new DefaultInputPort("in", this);
        this.outputPort = new DefaultOutputPort("out");
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

        outputPort.send(message);
    }

    public InputPort getInputPort(){
        return this.inputPort;
    }

    public OutputPort getOutputPort(){
        return this.outputPort;
    }
}
