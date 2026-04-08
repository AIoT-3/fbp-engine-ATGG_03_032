package com.fbp.engine.node.impl;

import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class GeneratorNode implements Node {
    private final String id;
    private OutputPort outputPort;

    public GeneratorNode(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        this.id = id;
        this.outputPort = new DefaultOutputPort("out");
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void initialize() {
        //Nothing to do
        throw new IllegalStateException();
    }

    @Override
    public void shutdown() {
        //Nothing to do
        throw new IllegalStateException();
    }

    @Override
    public void process(Message message) {
        //Nothing to do
        throw new IllegalStateException();
    }

    public void generate(String key, Object value){
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }
        if(value == null){
            throw new IllegalArgumentException("value must be notNull");
        }

        outputPort.send(new Message(
                Map.of(key, value)
        ));
    }

    public OutputPort getOutputPort(){
        return outputPort;
    }

    public void setOutputPort(OutputPort outputPort) {
        this.outputPort = Objects.requireNonNull(
                outputPort, "outputPort must be notNull");
    }
}
