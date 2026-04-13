package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.util.Map;

public class GeneratorNode extends AbstractNode{

    public GeneratorNode(String id) {
        super(id);
        addOutputPort("out");
    }


    @Override
    public void onProcess(String portName, Message message) {
        throw new IllegalStateException();
    }


    public void generate(String key, Object value){
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }
        if(value == null){
            throw new IllegalArgumentException("value must be notNull");
        }

        getOutputPort("out").send(new Message(
                Map.of(key, value)
        ));
    }
}
