package com.fbp.engine.node.impl;

import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class GeneratorNode extends AbstractNode{

    public GeneratorNode(String id) {
        super(id);
        addOutputPort("out");
    }


    @Override
    public void onProcess(Message message) {
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
